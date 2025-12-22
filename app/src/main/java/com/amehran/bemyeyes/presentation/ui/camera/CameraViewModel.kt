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
    private val detectionTracker: com.amehran.bemyeyes.domain.tracker.DetectionTracker
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
                // Filter raw detections through the Temporal Smoothing Tracker
                val trackingResult = detectionTracker.process(rawDetections)
                val allStableDetections = trackingResult.allStableDetections
                val newStableDetections = trackingResult.newStableDetections
                
                _detections.value = allStableDetections

                // STRATEGY: 
                // 1. Always announce "New" stable objects (entered the scene or stabilized).
                // 2. Announce "Urgent" objects if cooldown passed (re-warn safety).
                // 3. Announce "Best" object ONLY if we haven't said it recently (scanning assistance).

                // Rule 1: New Stable Objects (High Priority)
                val bestNewDetection = newStableDetections.maxByOrNull { it.confidence }
                if (bestNewDetection != null) {
                    speakDetection(bestNewDetection)
                    return@launch
                }

                // Rule 2 & 3: Persistent Objects
                // If nothing new appeared, look at what's already there.
                val bestPersistentDetection = allStableDetections.sortedWith(
                    compareByDescending<Detection> { isUrgent(it.label) } // Urgent first
                        .thenByDescending { getDistanceScore(it.boundingBox) } // Closer first
                ).firstOrNull()

                bestPersistentDetection?.let { detection ->
                    val currentTime = System.currentTimeMillis()
                    val label = detection.label
                    val lastTime = lastSpokenTimestamp[label] ?: 0L
                    
                    // Urgent items have a shorter cooldown (e.g., 5s) to ensure safety reminders
                    // Normal items have a long cooldown (e.g., 10s) to avoid "Chair... Chair..." chatter
                    val cooldownMs = if (isUrgent(label)) 5000L else 10000L

                    if ((currentTime - lastTime) > cooldownMs) {
                        speakDetection(detection)
                    }
                }
            } finally {
                isProcessing = false
            }
        }
    }

    private fun isUrgent(label: String): Boolean {
        return setOf("car", "bus", "truck", "traffic light", "stop sign", "fire hydrant").contains(label)
    }

    private fun getDistanceScore(box: android.graphics.RectF): Float {
        // Height ratio is a proxy for distance (larger height = closer)
        // Model input is 320x320
        return box.height() / 320f
    }

    private fun speakDetection(detection: Detection) {
        val message = detection.getDescription()
        val label = detection.label
        
        textToSpeechManager.speak(message)
        
        if (isUrgent(label)) {
            vibrationManager.vibrateCaution()
        }
        
        lastSpokenTimestamp[label] = System.currentTimeMillis()
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeechManager.shutdown()
    }
}
