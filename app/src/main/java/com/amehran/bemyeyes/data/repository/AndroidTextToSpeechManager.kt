package com.amehran.bemyeyes.data.repository

import android.content.Context
import android.speech.tts.TextToSpeech
import com.amehran.bemyeyes.domain.repository.TextToSpeechManager
import java.util.Locale

class AndroidTextToSpeechManager(
    context: Context
) : TextToSpeechManager, TextToSpeech.OnInitListener {

    private val tts: TextToSpeech = TextToSpeech(context, this)

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        } else {
            // Handle initialization error
        }
    }

    override fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun stop() {
        tts.stop()
    }

    override fun shutdown() {
        tts.shutdown()
    }
}
