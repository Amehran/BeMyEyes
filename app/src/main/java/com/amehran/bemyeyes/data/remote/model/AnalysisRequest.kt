package com.amehran.bemyeyes.data.remote.model

import com.google.gson.annotations.SerializedName

data class AnalysisRequest(
    @SerializedName("image_base64") val imageBase64: String,
    @SerializedName("user_intent") val userIntent: String, // "AUTO", "NAVIGATION", "READING", "GENERAL"
    @SerializedName("telemetry") val telemetry: Telemetry? = null,
    @SerializedName("audio_query") val audioQuery: String? = null,
    @SerializedName("language") val language: String? = "en",
    @SerializedName("looking_for") val lookingFor: String? = null,
    @SerializedName("user_id") val userId: String? = null
)

data class Telemetry(
    @SerializedName("speed_mps") val speedMps: Double,
    @SerializedName("location_type") val locationType: String, // "INDOOR", "OUTDOOR", "UNKNOWN"
    @SerializedName("heading") val heading: Double = 0.0,
    @SerializedName("pitch") val pitch: Double = 0.0
)
