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
class RecorderFSM(context: Context) {
    private var state: State = State.Idle
    private var thresholdCount: Int = 0

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
                // [Idle] amplitude exceeds speaking? To Speaking.
                if (amplitude > thresholdIdleToSpeaking) {
                    reset()
                    state = State.Speaking
                    return RecorderStateOutput.Normal
                }

                // [Idle] amplitude is lower than cancel threshold? Accumulate cancel count.
                thresholdCount = if (amplitude < thresholdIdleCancel) {
                    thresholdCount + 1
                } else {
                    0
                }

                if (thresholdCount >= windowLengthIdle) {
                    return RecorderStateOutput.CancelRecording
                }
            }

            State.Speaking -> {
                // [Speaking] amplitude is lower than finish threshold? Accumulate finish count.
                thresholdCount = if (amplitude < thresholdSpeakingFinish) {
                    thresholdCount + 1
                } else {
                    0
                }

                if (thresholdCount >= windowLengthSpeaking) {
                    return RecorderStateOutput.FinishRecording
                }
            }
        }

        return RecorderStateOutput.Normal
    }

    fun reset() {
        state = State.Idle
        thresholdCount = 0
    }
}

enum class RecorderStateOutput {
    Normal, CancelRecording, FinishRecording
}