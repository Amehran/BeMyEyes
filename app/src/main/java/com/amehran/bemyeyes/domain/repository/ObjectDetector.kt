package com.amehran.bemyeyes.domain.repository

import android.graphics.Bitmap
import com.amehran.bemyeyes.domain.model.Detection
import kotlinx.coroutines.flow.Flow

interface ObjectDetector {
    suspend fun detect(bitmap: Bitmap): List<Detection>
}
