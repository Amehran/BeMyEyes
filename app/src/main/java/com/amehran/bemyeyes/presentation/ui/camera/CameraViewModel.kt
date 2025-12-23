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

    fun detect(bitmap: Bitmap) {
        if (isProcessing) return
        isProcessing = true

        viewModelScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                val rawDetections = objectDetector.detect(bitmap)
                processDetections(rawDetections, startTime)
            } finally {
                isProcessing = false
            }
        }
    }

    fun detect(imageProxy: androidx.camera.core.ImageProxy) {
        if (isProcessing) {
            imageProxy.close()
            return
        }
        isProcessing = true

        viewModelScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                val rawDetections = objectDetector.detect(imageProxy)
                processDetections(rawDetections, startTime)
            } finally {
                imageProxy.close()
                isProcessing = false
            }
        }
    }

    private suspend fun processDetections(rawDetections: List<Detection>, startTime: Long) {
        val endTime = System.currentTimeMillis()



        val trackingResult = detectionTracker.process(rawDetections)
        val allStableDetections = trackingResult.allStableDetections
        val newStableDetections = trackingResult.newStableDetections
        
        _detections.value = allStableDetections

        // PHASE 5: Contextual Intelligence
        
        // 1. If scene changed, describe logic...
        if (newStableDetections.isNotEmpty()) {
            speakScene(allStableDetections)
            return
        }

        // 2. Periodic Reminder
        val hasUrgent = allStableDetections.any { isUrgent(it.label) }
        if (hasUrgent) {
             val currentTime = System.currentTimeMillis()
             if (currentTime - lastSceneSpokenTime > 5000L) {
                 speakScene(allStableDetections)
             }
        }
    }

    private var lastSceneSpokenTime = 0L

    private fun isUrgent(label: String): Boolean {
        return setOf("car", "bus", "truck", "traffic light", "stop sign", "fire hydrant", "Obstacle").contains(label)
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
