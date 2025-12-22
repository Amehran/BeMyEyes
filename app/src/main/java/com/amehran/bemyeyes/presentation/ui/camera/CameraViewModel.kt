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
                val stableDetections = detectionTracker.process(rawDetections)
                
                _detections.value = stableDetections

                // Prioritize detections: Urgent > Close > Confident
                val bestDetection = stableDetections.sortedWith(
                    compareByDescending<Detection> { isUrgent(it.label) } // Urgent first
                        .thenByDescending { getDistanceScore(it.boundingBox) } // Closer first
                        .thenByDescending { it.confidence } // More confident first
                ).firstOrNull()

                bestDetection?.let { detection ->
                    val message = detection.getDescription()
                    val currentTime = System.currentTimeMillis()
                    val label = detection.label
                    
                    val lastTimeForThisObject = lastSpokenTimestamp[label] ?: 0L

                    // Speak if enough time has passed for THIS specific object
                    if ((currentTime - lastTimeForThisObject) > spamCooldownMs) {
                        textToSpeechManager.speak(message)
                        
                        if (isUrgent(label)) {
                            vibrationManager.vibrateCaution()
                        }

                        lastSpokenTimestamp[label] = currentTime
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

    override fun onCleared() {
        super.onCleared()
        textToSpeechManager.shutdown()
    }
}
