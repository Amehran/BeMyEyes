package com.amehran.bemyeyes.data.repository

import com.amehran.bemyeyes.data.remote.api.BeMyEyesApi
import com.amehran.bemyeyes.data.remote.model.AnalysisRequest
import com.amehran.bemyeyes.data.remote.model.Telemetry
import com.amehran.bemyeyes.domain.model.ActionType
import com.amehran.bemyeyes.domain.model.SceneAction
import com.amehran.bemyeyes.domain.model.SceneAnalysis
import com.amehran.bemyeyes.domain.repository.BackendRepository
import javax.inject.Inject

class BackendRepositoryImpl @Inject constructor(
    private val api: BeMyEyesApi
) : BackendRepository {

    override suspend fun analyzeImage(
        imageBase64: String, 
        userIntent: String,
        telemetry: Telemetry?,
        audioQuery: String?,
        language: String
    ): Result<SceneAnalysis> {
        return try {
            val response = api.analyzeImage(
                AnalysisRequest(
                    imageBase64 = imageBase64,
                    userIntent = userIntent,
                    telemetry = telemetry,
                    audioQuery = audioQuery,
                    language = language
                )
            )
            
            // Map DTO to Domain Model
            val domainActions = response.actions.map { actionDto ->
                SceneAction(
                    type = when(actionDto.type.uppercase()) {
                        "TTS" -> ActionType.TTS
                        "HAPTIC" -> ActionType.HAPTIC
                        "SETTING_UPDATE" -> ActionType.SETTING_UPDATE
                        else -> ActionType.UNKNOWN
                    },
                    content = actionDto.content
                )
            }
            
            Result.success(
                SceneAnalysis(
                    agentUsed = response.agentUsed,
                    actions = domainActions
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
