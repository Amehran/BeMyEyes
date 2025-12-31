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
    private val speechManager: com.amehran.bemyeyes.domain.repository.SpeechManager,
    private val detectionTracker: com.amehran.bemyeyes.domain.tracker.DetectionTracker,
    private val sceneDescriber: com.amehran.bemyeyes.domain.describer.SceneDescriber,
    private val backendRepository: com.amehran.bemyeyes.domain.repository.BackendRepository,
    private val deviceInterpreter: com.amehran.bemyeyes.data.interpreter.OnDeviceGeminiInterpreter
) : ViewModel() {

    private val _detections = MutableStateFlow<List<Detection>>(emptyList())
    val detections = _detections.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()
    
    private var activeAudioQuery: String? = null

    private var isProcessing = false
    
    // AI Description State
    private var shouldDescribeNextFrame = false
    private var descriptionModeIsCloud = true
    private var currentLanguageCode = "en"

    fun onDescribeScene() {
        // Feature: Tap to Interrupt
        if (textToSpeechManager.isSpeaking()) {
            textToSpeechManager.stop()
            // Optionally cancel pending processing if we could, but for now just silencing is enough.
            return
        }

        if (shouldDescribeNextFrame) return // Already queued
        
        // Use persisted state
        descriptionModeIsCloud = isCloudMode.value
        currentLanguageCode = if (isFarsi.value) "fa" else "en"
        
        shouldDescribeNextFrame = true
        activeAudioQuery = null // Clear any old query for simple tap
        vibrationManager.vibrateCaution() // Haptic feedback acknowledging request
    }

    fun startListening() {
        if (_isListening.value) return
        
        // Stop TTS if speaking
        if (textToSpeechManager.isSpeaking()) {
            textToSpeechManager.stop()
        }

        _isListening.value = true
        vibrationManager.vibrateClick()
        
        speechManager.startListening(
            onResult = { text ->
                if (text.isNotBlank()) {
                    android.util.Log.d("CameraViewModel", "Speech recognized: $text")
                    activeAudioQuery = text
                    shouldDescribeNextFrame = true // Trigger analysis with this query
                    vibrationManager.vibrateCaution()
                }
                _isListening.value = false
            },
            onError = { error ->
                android.util.Log.e("CameraViewModel", "Speech Error: $error")
                _isListening.value = false
                textToSpeechManager.speak("Didn't catch that.")
            }
        )
    }

    fun stopListening() {
        if (_isListening.value) {
            speechManager.stopListening()
            // State update happens in callback or we force it if needed, 
            // but let's wait for callback to ensure we get results or error.
            // Actually, Android Speech Recognizer might not callback if stopped abruptly without silence?
            // Usually valid usage is: stopListening() -> waits for result processing.
        }
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

    private val _isPowerSaverMode = MutableStateFlow(prefs.getBoolean("power_saver_mode", true)) // Default to True for battery
    val isPowerSaverMode = _isPowerSaverMode.asStateFlow()

    fun setPowerSaverMode(enabled: Boolean) {
        _isPowerSaverMode.value = enabled
        prefs.edit().putBoolean("power_saver_mode", enabled).apply()
    }

    private val _isOutdoorMode = MutableStateFlow(prefs.getBoolean("is_outdoor", false)) // Default Indoor
    val isOutdoorMode = _isOutdoorMode.asStateFlow()

    fun setOutdoorMode(enabled: Boolean) {
        _isOutdoorMode.value = enabled
        prefs.edit().putBoolean("is_outdoor", enabled).apply()
    }

    private val _isTtsEnabled = MutableStateFlow(prefs.getBoolean("is_tts_enabled", true))
    val isTtsEnabled = _isTtsEnabled.asStateFlow()

    fun setTtsEnabled(enabled: Boolean) {
        _isTtsEnabled.value = enabled
        prefs.edit().putBoolean("is_tts_enabled", enabled).apply()
        if (enabled) textToSpeechManager.speak("Voice Feedback Enabled")
    }

    fun detect(imageProxy: androidx.camera.core.ImageProxy) {
        if (isProcessing) {
            imageProxy.close()
            return
        }
        isProcessing = true

        // 1. Check if user requested a full Scene Description (Backend Analysis)
        if (shouldDescribeNextFrame) {
            shouldDescribeNextFrame = false // Consume request
            
            viewModelScope.launch {
                try {
                    val bitmap = imageProxy.toBitmap()
                    // 1. Convert to Base64
                    val base64Image = bitmapToBase64(bitmap)
                    
                    // UX: Randomized Feedback
                    val feedback = getProcessingFeedback(isFarsi.value)
                    textToSpeechManager.speak(feedback)
                    
                    // 2. Call Backend (Orchestrator)
                    android.util.Log.d("CameraViewModel", "Sending Image to Backend...")
                    
                    val locationType = if (isOutdoorMode.value) "OUTDOOR" else "INDOOR"
                    val telemetryData = com.amehran.bemyeyes.data.remote.model.Telemetry(
                        speedMps = 0.0,
                        locationType = locationType
                    )

                    val result = backendRepository.analyzeImage(
                        imageBase64 = base64Image,
                        userIntent = if (activeAudioQuery != null) "GENERAL" else "AUTO",
                        telemetry = telemetryData,
                        audioQuery = activeAudioQuery
                    )
                    
                    // Reset query after using it
                    activeAudioQuery = null
                    
                    result.onSuccess { analysis ->
                         android.util.Log.d("CameraViewModel", "Backend Response: $analysis")
                         // 3. Execute Actions
                         analysis.actions.forEach { action ->
                             when(action.type) {
                                 com.amehran.bemyeyes.domain.model.ActionType.TTS -> {
                                     // Handle Language Translation if needed, or assume backend returns standard
                                     android.util.Log.d("CameraViewModel", "Action TTS: ${action.content}")
                                     _lastDescription.value = action.content
                                     if (_isTtsEnabled.value) {
                                         textToSpeechManager.speak(action.content)
                                     }
                                 }
                                 com.amehran.bemyeyes.domain.model.ActionType.HAPTIC -> {
                                     vibrationManager.vibrateCaution()
                                 }
                                 com.amehran.bemyeyes.domain.model.ActionType.SETTING_UPDATE -> {
                                     android.util.Log.d("CameraViewModel", "Received SETTING_UPDATE: ${action.content}")
                                     val parts = action.content.split("=")
                                     if (parts.size == 2) {
                                         val key = parts[0].trim() // Trim just in case
                                         val value = parts[1].trim().toBoolean()
                                         android.util.Log.d("CameraViewModel", "Parsed Setting: $key = $value")
                                         when(key) {
                                             "OUTDOOR" -> {
                                                 android.util.Log.d("CameraViewModel", "Setting Outdoor Mode to $value")
                                                 setOutdoorMode(value)
                                             }
                                             "IS_TTS_ENABLED" -> {
                                                 setTtsEnabled(value)
                                             }
                                             "REALTIME_ENABLED" -> {
                                                 setRealtimeDetectionEnabled(value)
                                             }
                                         }
                                     }
                                 }
                                 else -> {}
                             }
                         }
                    }.onFailure { e ->
                        android.util.Log.e("CameraViewModel", "Backend Error", e)
                        e.printStackTrace()
                        textToSpeechManager.speak("Connection failed.")
                    }

                } catch (e: Exception) {
                    android.util.Log.e("CameraViewModel", "Processing Error", e)
                    e.printStackTrace()
                    textToSpeechManager.speak("Error processing image.")
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
    
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = java.io.ByteArrayOutputStream()
        // Resize if too big? Backend handles it, but better safe bandwidth
        val resized = if (bitmap.width > 800) {
             val aspect = bitmap.height.toFloat() / bitmap.width.toFloat()
             Bitmap.createScaledBitmap(bitmap, 800, (800 * aspect).toInt(), true)
        } else bitmap
        
        resized.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
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

    private val _lastDescription = MutableStateFlow<String?>(null)
    val lastDescription = _lastDescription.asStateFlow()

    private fun speakScene(detections: List<Detection>) {
        if (detections.isEmpty()) return
        
        val message = sceneDescriber.describe(detections)
        _lastDescription.value = message
        
        if (_isTtsEnabled.value) {
            textToSpeechManager.speak(message)
        }
        
        lastSceneSpokenTime = System.currentTimeMillis()

        if (detections.any { isUrgent(it.label) }) {
            vibrationManager.vibrateCaution()
        }
    }

    private fun getProcessingFeedback(isFarsi: Boolean): String {
        val englishPhrases = listOf("Analyzing...", "Looking...", "Processing...", "Scanning...", "One moment...")
        val farsiPhrases = listOf("در حال پردازش...", "نگاه میکنم...", "کمی صبر کنید...", "تصویر گرفته شد...")
        
        return if (isFarsi) {
            farsiPhrases.random()
        } else {
            englishPhrases.random()
        }
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeechManager.shutdown()
    }
}
