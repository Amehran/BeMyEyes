package com.amehran.bemyeyes.domain.model

data class SceneAnalysis(
    val agentUsed: String,
    val actions: List<SceneAction>
)

data class SceneAction(
    val type: ActionType,
    val content: String
)

enum class ActionType {
    TTS,
    HAPTIC,
    UNKNOWN
}
