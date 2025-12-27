package com.amehran.bemyeyes.data.remote.model

import com.google.gson.annotations.SerializedName

data class AnalysisResponse(
    @SerializedName("agent_used") val agentUsed: String,
    @SerializedName("actions") val actions: List<Action>
)

data class Action(
    @SerializedName("type") val type: String, // "TTS", "HAPTIC"
    @SerializedName("content") val content: String
)
