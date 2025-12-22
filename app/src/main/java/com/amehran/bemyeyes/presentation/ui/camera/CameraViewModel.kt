package com.amehran.bemyeyes.presentation.ui.camera

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amehran.bemyeyes.domain.model.Detection
import com.amehran.bemyeyes.domain.repository.ObjectDetector
import com.amehran.bemyeyes.domain.repository.TextToSpeechManager
import com.amehran.bemyeyes.domain.repository.VibrationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val objectDetector: ObjectDetector,
    private val textToSpeechManager: TextToSpeechManager,
    private val vibrationManager: VibrationManager,
    private val detectionTracker: com.amehran.bemyeyes.domain.tracker.DetectionTracker,
    private val sceneDescriber: com.amehran.bemyeyes.domain.describer.SceneDescriber
) : ViewModel() {

    private val _detections = MutableStateFlow<List<Detection>>(emptyList())
    val detections = _detections.asStateFlow()

    private var isProcessing = false
    private val lastSpokenTimestamp = mutableMapOf<String, Long>()
    private val spamCooldownMs = 4000L // Don't repeat same message for 4 seconds

    fun detect(bitmap: Bitmap) {
        if (isProcessing) return
        isProcessing = true

        viewModelScope.launch {
            try {
                val rawDetections = objectDetector.detect(bitmap)
                val trackingResult = detectionTracker.process(rawDetections)
                val allStableDetections = trackingResult.allStableDetections
                val newStableDetections = trackingResult.newStableDetections
                
                _detections.value = allStableDetections

                // PHASE 5: Contextual Intelligence
                // Shift from "Single Object" to "Scene Description"

                // 1. If scene changed (new objects stable), describe the scene.
                if (newStableDetections.isNotEmpty()) {
                    speakScene(allStableDetections)
                    return@launch
                }

                // 2. Periodic Reminder (if something urgent is there)
                // Check if we have urgent items and it's been a while since we spoke about them
                val hasUrgent = allStableDetections.any { isUrgent(it.label) }
                if (hasUrgent) {
                     // Check global scene cooldown for urgent reminders (e.g. 5s)
                     val currentTime = System.currentTimeMillis()
                     if (currentTime - lastSceneSpokenTime > 5000L) {
                         speakScene(allStableDetections)
                     }
                }
            } finally {
                isProcessing = false
            }
        }
    }

    private var lastSceneSpokenTime = 0L

    private fun isUrgent(label: String): Boolean {
        return setOf("car", "bus", "truck", "traffic light", "stop sign", "fire hydrant").contains(label)
    }

    private fun speakScene(detections: List<Detection>) {
        if (detections.isEmpty()) return
        
        val message = sceneDescriber.describe(detections)
        textToSpeechManager.speak(message)
        lastSceneSpokenTime = System.currentTimeMillis()

        if (detections.any { isUrgent(it.label) }) {
            vibrationManager.vibrateCaution()
        }
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeechManager.shutdown()
    }
}
