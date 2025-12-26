package com.amehran.bemyeyes.domain.interpreter

import android.graphics.Bitmap

interface SceneInterpreter {
    suspend fun describe(bitmap: Bitmap, languageCode: String = "en"): String
}
