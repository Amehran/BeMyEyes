package com.amehran.bemyeyes.domain.repository

interface SpeechManager {
    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onPartialResult: ((String) -> Unit)? = null
    )
    fun stopListening()
    fun shutdown()
}
