package com.amehran.bemyeyes.data.remote.api

import com.amehran.bemyeyes.data.remote.model.AnalysisRequest
import com.amehran.bemyeyes.data.remote.model.AnalysisResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface BeMyEyesApi {
    
    @POST("/api/v1/analyze")
    suspend fun analyzeImage(@Body request: AnalysisRequest): AnalysisResponse
}
