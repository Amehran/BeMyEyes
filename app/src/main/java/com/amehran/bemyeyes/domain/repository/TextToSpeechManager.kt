package com.amehran.bemyeyes.domain.repository

interface TextToSpeechManager {
    fun speak(text: String)
    fun stop()
    fun shutdown()
    fun setLanguage(languageCode: String)
}
