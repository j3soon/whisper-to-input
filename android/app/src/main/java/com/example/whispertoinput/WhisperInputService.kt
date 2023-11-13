package com.example.whispertoinput

import android.inputmethodservice.InputMethodService
import android.view.View
import kotlinx.coroutines.*


class WhisperInputService : InputMethodService()
{
    private var whisperKeyboard : WhisperKeyboard = WhisperKeyboard()
    private var whisperJobManager : WhisperJobManager = WhisperJobManager()

    private fun transcriptionCallback(text : String?)
    {
        if (text == null)
        {
            return
        }

        currentInputConnection?.commitText(text, text.length)
        whisperKeyboard.Reset()
    }

    override fun onCreateInputView(): View {
        return whisperKeyboard.Setup(
            layoutInflater,
            { },
            { },
            { whisperJobManager.startTranscriptionJobAsync { transcriptionCallback(it) } },
            { whisperJobManager.clearTranscriptionJob() })
    }

    override fun onWindowShown() {
        super.onWindowShown()
        whisperJobManager.clearTranscriptionJob()
        whisperKeyboard.Reset()
    }

    override fun onWindowHidden() {

        super.onWindowHidden()
        whisperJobManager.clearTranscriptionJob()
        whisperKeyboard.Reset()
    }
}
