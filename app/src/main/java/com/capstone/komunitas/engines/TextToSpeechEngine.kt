package com.capstone.komunitas.engines

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import java.util.*

class TextToSpeechEngine (
    context: Context
) {
    private val appContext = context.applicationContext

    private val textToSpeechEngine: TextToSpeech by lazy {
        TextToSpeech(appContext,
            TextToSpeech.OnInitListener { status ->
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeechEngine.language = Locale.getDefault()
                }
            })
    }

    fun textToSpeech(text:String){
        textToSpeechEngine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts1")
    }
}