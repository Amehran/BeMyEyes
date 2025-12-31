package com.amehran.bemyeyes.data.remote.model

import com.google.gson.annotations.SerializedName

data class AnalysisRequest(
    @SerializedName("image_base64") val imageBase64: String,
    @SerializedName("user_intent") val userIntent: String, // "AUTO", "NAVIGATION", "READING", "GENERAL"
    @SerializedName("telemetry") val telemetry: Telemetry? = null,
    @SerializedName("audio_query") val audioQuery: String? = null
)

data class Telemetry(
    @SerializedName("speed_mps") val speedMps: Double,
    @SerializedName("location_type") val locationType: String // "INDOOR", "OUTDOOR", "UNKNOWN"
)
