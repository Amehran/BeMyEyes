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
    private val vibrationManager: VibrationManager
) : ViewModel() {

    private val _detections = MutableStateFlow<List<Detection>>(emptyList())
    val detections = _detections.asStateFlow()

    private var isProcessing = false
    private var lastSpokenMessage: String? = null
    private var lastSpokenTime: Long = 0
    private val spamCooldownMs = 4000L // Don't repeat same message for 4 seconds

    fun detect(bitmap: Bitmap) {
        if (isProcessing) return
        isProcessing = true

        viewModelScope.launch {
            try {
                val detections = objectDetector.detect(bitmap)
                _detections.value = detections

                // Prioritize detections: Urgent > Close > Confident
                val bestDetection = detections.sortedWith(
                    compareByDescending<Detection> { isUrgent(it.label) } // Urgent first
                        .thenByDescending { getDistanceScore(it.boundingBox) } // Closer first
                        .thenByDescending { it.confidence } // More confident first
                ).firstOrNull()

                bestDetection?.let { detection ->
                    val message = detection.getDescription()
                    val currentTime = System.currentTimeMillis()

                    // Speak if it's a new message OR if enough time has passed for the same message
                    if (message != lastSpokenMessage || (currentTime - lastSpokenTime) > spamCooldownMs) {
                        textToSpeechManager.speak(message)
                        
                        if (isUrgent(detection.label)) {
                            vibrationManager.vibrateCaution()
                        }

                        lastSpokenMessage = message
                        lastSpokenTime = currentTime
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
