package com.example.whispertoinput.recorder

import android.content.Context
import com.example.whispertoinput.R
import kotlin.math.roundToInt

// A recorder FSM.
// Takes in amplitude reports, and for each report,
// provides an action (Normal, CancelRecording, and FinishRecording)
// that should be done with the recorder.

// Amplitude reports should come in regularly (every R.integer.recorder_amplitude_report_period ms)
// The FSM has two states,
// [Idle] The initial state when starting recording.
//   1. If any amplitude report exceeds R.integer.recorder_fsm_idle_speaking_threshold,
//      state turns into Speaking.
//   2. If the user stays rather silent for at least R.integer.recorder_fsm_idle_cancel_time (ms),
//      recording is cancelled, as if nothing happened. Specifically, all reports should be below
//      R.integer.recorder_fsm_idle_cancel_threshold during this time.
// [Speaking] The state in which the user is speaking.
//   1. The user can speak for an arbitrary amount of time.
//   2. If the user stays rather silent for at least R.integer.recorder_fsm_speaking_finish_time (ms),
//      recording is finished, and transcription started. Specifically, all reports should be below
//      R.integer.recorder_fsm_speaking_finish_threshold during this time.

// Maximum in A Windowed Stream
// The mechanism above involves a stream of data (amplitude reports),
// and detecting the maximum over the most recent some-period-of-time (window).
// (e.g., when the maximum report over the last 2 seconds is below some threshold, do something)
// We refer to this problem as "maximum in a windowed stream".
// To efficiently obtain the maximum in a windowed stream, several data is maintained.
//   1. data window: containing all the data points in the current window.
//      As the stream feeds more data, older data of the window will be popped
//      from the front, and newer data will be appended to the end of the window.
//   2. decreasing deque: a deque whose element is a subset of the data window.
//      More of this deque below.
// ----------------------

// Trivially, it is sufficient to just maintain the data window, if we want to
// obtain the maximum value in the window, but with time complexity linear to window length.
// We would like to achieve amortized constant time here. It is not enough to just maintain
// the current maximum value, as if it is removed from the window, the (orig) second-max
// element in the window would now become the new max element.
// Thus, we make use of a decreasing deque (dq), that has the following properties,
//   1. The elements in dq are a subset of those in the data window, with the same relative order.
//      (e.g., window=[2 5 1 3 4], dq = [5 4])
//   2. The first element of dq is always the maximum value of the current window.
//   3. dq is a monotonically non-strict decreasing sequence (aka. a non-increasing sequence).
// If dq is properly maintained, then we can obtain the current maximum in constant time.
// The current maximum can only change when a new value arrives. When it happens,
//   1. If the current window is full, an old value should be popped first.
//      And if the popped value is the current maximum, a value should be popped from dq as well.
//   2. Then, the arriving value is appended to the back of the window.
//   3. This new value should be appended to the back of dq.
//      However, to maintain the properties of dq, elements will be popped from the back
//      until the new value is the minimum in dq (decreasing property)
//      For example, if the new value is extremely large, all values originally in dq
//      will be popped. The new value now becomes the head of dq, also the current maximum.
// Check https://paste.ofcode.org/wg5QLYp6QvqyD7AqbLw5FE for a Python simulation of this algorithm.
class RecorderFSM(context: Context) {
    private var state: State = State.Idle
    private var decreasingDeque: ArrayDeque<Int> = ArrayDeque()
    private var dataWindow: ArrayDeque<Int> = ArrayDeque()
    private var numReportsThisState: Int = 0

    private val context: Context
    private val windowLengthIdle: Int
    private val windowLengthSpeaking: Int
    private val thresholdIdleCancel: Int
    private val thresholdIdleToSpeaking: Int
    private val thresholdSpeakingFinish: Int

    init {
        this.context = context

        val idleCancelTime = context.resources.getInteger(R.integer.recorder_fsm_idle_cancel_time)
        val speakingFinishTime =
            context.resources.getInteger(R.integer.recorder_fsm_speaking_finish_time)
        val amplitudeReportPeriod =
            context.resources.getInteger(R.integer.recorder_amplitude_report_period)

        windowLengthIdle = (idleCancelTime.toFloat() / amplitudeReportPeriod).roundToInt()
        windowLengthSpeaking = (speakingFinishTime.toFloat() / amplitudeReportPeriod).roundToInt()

        thresholdIdleCancel =
            context.resources.getInteger(R.integer.recorder_fsm_idle_cancel_threshold)
        thresholdIdleToSpeaking =
            context.resources.getInteger(R.integer.recorder_fsm_idle_speaking_threshold)
        thresholdSpeakingFinish =
            context.resources.getInteger(R.integer.recorder_fsm_speaking_finish_threshold)
    }

    enum class State {
        Idle, Speaking
    }

    fun reportAmplitude(amplitude: Int): RecorderStateOutput {
        when (state) {
            State.Idle -> {
                pushNewReport(amplitude, windowLengthIdle)
                if (amplitude > thresholdIdleToSpeaking) {
                    state = State.Speaking
                    dataWindow = ArrayDeque()
                    numReportsThisState = 0
                    return RecorderStateOutput.Normal
                }

                if (dataWindow.size == windowLengthIdle && currentMaximumReport() < thresholdIdleCancel) {
                    return RecorderStateOutput.CancelRecording
                }
            }

            State.Speaking -> {
                pushNewReport(amplitude, windowLengthSpeaking)
                if (dataWindow.size == windowLengthSpeaking && currentMaximumReport() < thresholdSpeakingFinish) {
                    return RecorderStateOutput.FinishRecording
                }
            }
        }

        return RecorderStateOutput.Normal
    }

    fun reset() {
        state = State.Idle
        dataWindow = ArrayDeque()
        numReportsThisState = 0
    }

    // Pushes a new value into the stream. Updates the current window and the decreasing queue.
    private fun pushNewReport(amplitude: Int, maxWindowLength: Int) {
        // If the dataWindow is currently full, remove one first.
        // If the decreasing queue's largest element is being removed here, update it as well.
        if (dataWindow.size == maxWindowLength) {
            if (decreasingDeque.first() == dataWindow.first()) {
                decreasingDeque.removeFirst()
            }
            dataWindow.removeFirst()
        }

        // maintain the decreasing property of the decreasing queue
        while (decreasingDeque.size > 0 && decreasingDeque.last() < amplitude) {
            decreasingDeque.removeLast()
        }

        dataWindow.addLast(amplitude)
        decreasingDeque.addLast(amplitude)
    }

    // The first element of the decreasing queue is always the max value in the current window.
    private fun currentMaximumReport(): Int {
        return decreasingDeque.first()
    }
}

enum class RecorderStateOutput {
    Normal, CancelRecording, FinishRecording
}