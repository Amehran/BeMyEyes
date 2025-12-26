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
    private val sceneDescriber: com.amehran.bemyeyes.domain.describer.SceneDescriber,
    private val cloudInterpreter: com.amehran.bemyeyes.data.interpreter.CloudGeminiInterpreter,
    private val deviceInterpreter: com.amehran.bemyeyes.data.interpreter.OnDeviceGeminiInterpreter
) : ViewModel() {

    private val _detections = MutableStateFlow<List<Detection>>(emptyList())
    val detections = _detections.asStateFlow()

    private var isProcessing = false
    
    // AI Description State
    private var shouldDescribeNextFrame = false
    private var descriptionModeIsCloud = true
    private var currentLanguageCode = "en"

    fun onDescribeScene() {
        if (shouldDescribeNextFrame) return // Already queued
        
        // Use persisted state
        descriptionModeIsCloud = isCloudMode.value
        currentLanguageCode = if (isFarsi.value) "fa" else "en"
        
        shouldDescribeNextFrame = true
        vibrationManager.vibrateCaution() // Haptic feedback acknowledging request
    }

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

    // Feature Toggles & Settings with Persistence
    private val prefs by lazy { 
        // Quick & dirty safe context access for settings. Ideally use DataStore, but this is robust for checking persistence NOW.
        // We'll use a standard name.
        com.amehran.bemyeyes.MainApplication.instance.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
    }

    private val _isRealtimeDetectionEnabled = MutableStateFlow(prefs.getBoolean("realtime_enabled", true))
    val isRealtimeDetectionEnabled = _isRealtimeDetectionEnabled.asStateFlow()

    private val _isCurtainMode = MutableStateFlow(prefs.getBoolean("curtain_mode", true))
    val isCurtainMode = _isCurtainMode.asStateFlow()

    private val _isCloudMode = MutableStateFlow(prefs.getBoolean("cloud_mode", true))
    val isCloudMode = _isCloudMode.asStateFlow()

    private val _isFarsi = MutableStateFlow(prefs.getBoolean("is_farsi", false))
    val isFarsi = _isFarsi.asStateFlow()

    fun setRealtimeDetectionEnabled(enabled: Boolean) {
        _isRealtimeDetectionEnabled.value = enabled
        prefs.edit().putBoolean("realtime_enabled", enabled).apply()
        if (!enabled) {
            _detections.value = emptyList() // Clear UI immediately
        }
    }

    fun setCurtainMode(enabled: Boolean) {
        _isCurtainMode.value = enabled
        prefs.edit().putBoolean("curtain_mode", enabled).apply()
    }

    fun setCloudMode(enabled: Boolean) {
        _isCloudMode.value = enabled
        prefs.edit().putBoolean("cloud_mode", enabled).apply()
    }

    fun setLanguageFarsi(enabled: Boolean) {
        _isFarsi.value = enabled
        prefs.edit().putBoolean("is_farsi", enabled).apply()
    }

    fun detect(imageProxy: androidx.camera.core.ImageProxy) {
        if (isProcessing) {
            imageProxy.close()
            return
        }
        isProcessing = true

        // 1. Check if user requested a full Scene Description (Gemini)
        // ... (Logic for Scene Description remains same) ...
        if (shouldDescribeNextFrame) {
            shouldDescribeNextFrame = false // Consume request
            val isCloud = descriptionModeIsCloud
            
            viewModelScope.launch {
                try {
                    val bitmap = imageProxy.toBitmap()
                    val lang = currentLanguageCode
                    textToSpeechManager.setLanguage(lang)
                    
                    val analyzingText = if (lang == "fa") "در حال تحلیل..." else "Analyzing..."
                    textToSpeechManager.speak(analyzingText)
                    
                    val response = if (isCloud) {
                        cloudInterpreter.describe(bitmap, lang)
                    } else {
                        deviceInterpreter.describe(bitmap, lang)
                    }
                    textToSpeechManager.speak(response)
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                    textToSpeechManager.speak("Sorry, I couldn't describe the scene.")
                } finally {
                    imageProxy.close()
                    isProcessing = false
                }
            }
            return
        }

        // 2. Regular Real-time Object Detection
        if (!_isRealtimeDetectionEnabled.value) {
            imageProxy.close()
            isProcessing = false
            return
        }

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
