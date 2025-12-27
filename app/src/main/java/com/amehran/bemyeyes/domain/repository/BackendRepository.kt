package com.amehran.bemyeyes.domain.repository

import com.amehran.bemyeyes.domain.model.SceneAnalysis
import com.amehran.bemyeyes.data.remote.model.Telemetry

interface BackendRepository {
    suspend fun analyzeImage(
        imageBase64: String, 
        userIntent: String,
        telemetry: Telemetry?
    ): Result<SceneAnalysis>
}
