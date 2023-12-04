package com.example.whispertoinput.keyboard

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val QUICK_BACKSPACE_DELAY: Long = 80
private const val DELAY_BEFORE_QUICK_BACKSPACE: Long = 600

class BackspaceButton(context: Context, attrs: AttributeSet) :
    AppCompatImageButton(context, attrs) {

    fun setBackspaceCallback(callback: () -> Unit) {
        backspaceCallback = callback
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    performClick()
                    startLongPressDetector()
                }

                MotionEvent.ACTION_UP -> abortLongPressDetector()
            }
            true
        }
    }

    // Override this for accessibility.
    // performClick() will be called either in appropriate touch events,
    // or when accessibility tools demand its invocation
    override fun performClick(): Boolean {
        super.performClick()
        backspaceCallback()
        return true
    }

    // Starts a job that periodically performs backspace
    private fun startLongPressDetector() {
        longPressDetectorJob?.cancel()
        longPressDetectorJob = CoroutineScope(Dispatchers.Main).launch {
            // Long Press: delay for a while before actually starting
            //   quick backspace. Any ACTION_UP will terminate either
            //   the waiting or quick backspacing.
            delay(DELAY_BEFORE_QUICK_BACKSPACE)
            while (this.isActive) {
                backspaceCallback()
                delay(QUICK_BACKSPACE_DELAY)
            }
        }
    }

    // Aborts any currently running quick backspace job.
    private fun abortLongPressDetector() {
        longPressDetectorJob?.cancel()
        longPressDetectorJob = null
    }

    // Stores the callback when a backspace should be performed
    private var backspaceCallback: () -> Unit = { }

    // Stores the currently running long press detector job
    private var longPressDetectorJob: Job? = null
}