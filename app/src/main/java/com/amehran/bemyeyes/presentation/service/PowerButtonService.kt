package com.amehran.bemyeyes.presentation.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.amehran.bemyeyes.R
import com.amehran.bemyeyes.domain.model.Detection
import com.amehran.bemyeyes.domain.repository.ObjectDetector
import com.amehran.bemyeyes.domain.repository.TextToSpeechManager
import com.amehran.bemyeyes.domain.repository.VibrationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class PowerButtonService : LifecycleService() {

    @Inject lateinit var objectDetector: ObjectDetector
    @Inject lateinit var textToSpeechManager: TextToSpeechManager
    @Inject lateinit var vibrationManager: VibrationManager

    private var pressCount = 0
    private var lastPressTime = 0L
    private val resetTimeMs = 2000L
    
    private var isDetectionRunning = false
    private var cameraProvider: ProcessCameraProvider? = null

    // Detection State
    private var isProcessing = false
    private var lastSpokenMessage: String? = null
    private var lastSpokenTime: Long = 0
    private val spamCooldownMs = 4000L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastPressTime > resetTimeMs) {
                pressCount = 0
            }
            pressCount++
            lastPressTime = currentTime

            if (pressCount >= 3) {
                toggleDetection()
                pressCount = 0
            }
        }
    }

    private fun toggleDetection() {
        if (isDetectionRunning) {
            stopCamera()
            textToSpeechManager.speak("Detection stopped")
        } else {
            textToSpeechManager.speak("Detection started")
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                processImage(imageProxy)
            }

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this,
                    cameraSelector,
                    imageAnalysis
                )
                isDetectionRunning = true
            } catch (exc: Exception) {
                exc.printStackTrace()
                textToSpeechManager.speak("Camera failed to start")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopCamera() {
        cameraProvider?.unbindAll()
        isDetectionRunning = false
    }

    private val tracker = com.amehran.bemyeyes.domain.model.ObjectTracker()

    private fun processImage(imageProxy: ImageProxy) {
        if (isProcessing) {
            imageProxy.close()
            return
        }
        isProcessing = true
        
        // Convert ImageProxy to Bitmap
        val bitmap = imageProxy.toBitmap()

        lifecycleScope.launch {
            try {
                val detections = objectDetector.detect(bitmap)
                val trackedObjects = tracker.processDetections(detections)
                handleTrackedObjects(trackedObjects)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                imageProxy.close()
                isProcessing = false
            }
        }
    }

    private fun handleTrackedObjects(trackedObjects: List<com.amehran.bemyeyes.domain.model.TrackedObject>) {
        // 1. Filter out unstable objects (must be seen for at least 3 frames)
        // Exception: Urgent objects are reported immediately (frame 1) for safety
        val stableObjects = trackedObjects.filter { 
            it.consecutiveFrames >= 3 || isUrgent(it.label)
        }

        if (stableObjects.isEmpty()) return

        // 2. Sort by urgency and proximity
        val bestObject = stableObjects.sortedWith(
            compareByDescending<com.amehran.bemyeyes.domain.model.TrackedObject> { isUrgent(it.label) }
                .thenByDescending { getDistanceScore(it.boundingBox) }
        ).firstOrNull()

        bestObject?.let { obj ->
            val currentTime = System.currentTimeMillis()
            val message = getMessageForObject(obj)

            // 3. Smart Feedback Logic
            // Speak if:
            // - It's a NEW object (different ID than last spoken)
            // - It's the SAME object but:
            //    - It has become Urgent (e.g. car got closer)
            //    - Enough time has passed (spamCooldown) AND it's still relevant
            
            // Check if this specific object instance was recently spoken
            val timeSinceLastSpoken = currentTime - obj.lastSpokenTime
            
            val shouldSpeak = if (message != lastSpokenMessage) {
                // New message (different object or different state) -> Speak immediately
                true
            } else {
                // Same message -> Only speak if cooldown passed
                timeSinceLastSpoken > spamCooldownMs
            }

            if (shouldSpeak) {
                textToSpeechManager.speak(message)
                if (isUrgent(obj.label)) {
                    vibrationManager.vibrateCaution()
                }
                
                lastSpokenMessage = message
                obj.lastSpokenTime = currentTime // Update this object's spoken time
            }
        }
    }

    private fun getMessageForObject(obj: com.amehran.bemyeyes.domain.model.TrackedObject): String {
        val label = obj.label
        val box = obj.boundingBox
        
        // Calculate position
        val centerX = box.centerX()
        val position = when {
            centerX < 106 -> "on left" // 320 / 3
            centerX > 213 -> "on right"
            else -> "in front"
        }

        // Calculate distance approximation
        val height = box.height()
        val distance = when {
            height > 200 -> "very close" // > 60% of screen
            height > 100 -> "nearby"     // > 30% of screen
            else -> "" // far away, don't mention distance
        }

        return if (distance.isNotEmpty()) {
            "$label, $position, $distance"
        } else {
            "$label, $position"
        }
    }

    private fun isUrgent(label: String): Boolean {
        return setOf("car", "bus", "truck", "traffic light", "stop sign", "fire hydrant").contains(label)
    }

    private fun getDistanceScore(box: android.graphics.RectF): Float {
        return box.height() / 320f
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        registerReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
        stopCamera()
        textToSpeechManager.shutdown()
    }

    private fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)
    }

    private fun startForegroundService() {
        val channelId = "PowerButtonServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Blind Assistant Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("BeMyEyes Assistant")
            .setContentText("Triple-press power button to toggle detection")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, 
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or 
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            )
        } else {
            startForeground(1, notification)
        }
    }
}
