package com.amehran.bemyeyes.data.interpreter

import android.graphics.Bitmap
import com.amehran.bemyeyes.domain.interpreter.SceneInterpreter
import javax.inject.Inject

class OnDeviceGeminiInterpreter @Inject constructor() : SceneInterpreter {

    override suspend fun describe(bitmap: Bitmap, languageCode: String): String {
        // Placeholder for Gemini Nano / AICore integration.
        // Requires specific Google Pixel / Samsung devices and AICore system module.
        // Falling back to a static message for now.
        return "On-device intelligence (Gemini Nano) is currently unavailable on this device. Please use Cloud Mode for now."
    }
}
