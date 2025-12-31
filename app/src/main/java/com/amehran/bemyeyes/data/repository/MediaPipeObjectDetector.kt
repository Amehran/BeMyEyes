package com.amehran.bemyeyes.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.amehran.bemyeyes.domain.model.Detection
import com.amehran.bemyeyes.domain.repository.ObjectDetector
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector as MPObjectDetector

class MediaPipeObjectDetector(
    private val context: Context,
    private val modelPath: String = "efficientdet-lite2.tflite",
    private val confidenceThreshold: Float = 0.3f
) : ObjectDetector {

    private var objectDetector: MPObjectDetector? = null

    // Whitelist of objects relevant for blind navigation
    private val relevantLabels = setOf(
        // Outdoor / Traffic
        "person", "bicycle", "car", "motorcycle", "bus", "truck", "traffic light", "fire hydrant", "stop sign", "parking meter", "bench",
        // Animals
        "dog", "cat", "bird", "horse",
        // Indoor / Obstacles
        "chair", "couch", "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "door", "stairs", "refrigerator", "oven"
    )

    init {
        setupDetector()
    }

    private fun setupDetector() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(modelPath)
            .build()

        val options = MPObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setScoreThreshold(confidenceThreshold)
            .setRunningMode(RunningMode.IMAGE)
            .setMaxResults(10)
            .build()

        objectDetector = MPObjectDetector.createFromOptions(context, options)
    }

    // Approximate real-world heights in meters
    private val objectHeights = mapOf(
        "person" to 1.7f,
        "bicycle" to 1.0f,
        "car" to 1.5f,
        "motorcycle" to 1.0f,
        "bus" to 3.2f,
        "truck" to 3.0f,
        "traffic light" to 0.8f,
        "stop sign" to 0.75f,
        "fire hydrant" to 0.6f,
        "dog" to 0.6f,
        "cat" to 0.3f,
        "chair" to 0.9f,
        "couch" to 0.8f,
        "potted plant" to 0.5f, // varies widely
        "bed" to 0.6f,
        "dining table" to 0.75f,
        "toilet" to 0.4f // seat height?
    )

    override suspend fun detect(bitmap: Bitmap): List<Detection> {
        val detector = objectDetector ?: return emptyList()

        // Convert Bitmap to MPImage
        val mpImage = BitmapImageBuilder(bitmap).build()

        // Inference (Bitmap is assumed upright)
        val detectionResult = detector.detect(mpImage)

        return processResult(detectionResult, bitmap.width, bitmap.height)
    }

    override suspend fun detect(imageProxy: androidx.camera.core.ImageProxy): List<Detection> {
        val detector = objectDetector ?: return emptyList()
        
        // Convert to Bitmap to handle padding/strides correctly
        // This avoids the "buffer size mismatch" error in MediaPipe
        val bitmap = imageProxy.toBitmap()
        val mpImage = BitmapImageBuilder(bitmap).build()

        // Handle Rotation
        val imageProcessingOptions = com.google.mediapipe.tasks.vision.core.ImageProcessingOptions.builder()
            .setRotationDegrees(imageProxy.imageInfo.rotationDegrees)
            .build()

        // Inference
        val detectionResult = detector.detect(mpImage, imageProcessingOptions)

        val width = if (imageProxy.imageInfo.rotationDegrees % 180 == 0) bitmap.width else bitmap.height
        val height = if (imageProxy.imageInfo.rotationDegrees % 180 == 0) bitmap.height else bitmap.width

        return processResult(detectionResult, width, height)
    }

    private fun processResult(
        detectionResult: com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult,
        imageWidth: Int,
        imageHeight: Int
    ): List<Detection> {
        return detectionResult.detections().mapNotNull { detection ->
            val topCategory = detection.categories().maxByOrNull { it.score() }

                val box = detection.boundingBox()
                
                // Normalize coordinates (0..1)
                val normalizedBox = android.graphics.RectF(
                    box.left / imageWidth,
                    box.top / imageHeight,
                    box.right / imageWidth,
                    box.bottom / imageHeight
                )

                val originalLabel = detection.categories().maxByOrNull { it.score() }?.categoryName() ?: "Unknown"
                val isRelevant = relevantLabels.contains(originalLabel)
                
                // Check if it's a "significant" obstacle even if not in whitelist
                // Criteria: Covers significant portion of screen (>30% height or width) 
                // AND has decent confidence
                val isSignificantObstacle = normalizedBox.height() > 0.3f || normalizedBox.width() > 0.3f

                if (isRelevant || isSignificantObstacle) {
                    val finalLabel = if (isRelevant) originalLabel else "Obstacle"
                    
                    // Estimate Distance
                    val realHeight = objectHeights[finalLabel] ?: 1.0f // Default to 1m for generic obstacles
                    
                    // Adjusted Focal Factor 0.3f based on calibration
                    val distance = (realHeight / normalizedBox.height().coerceAtLeast(0.1f)) * 0.3f

                    Detection(
                        label = finalLabel,
                        confidence = detection.categories().maxByOrNull { it.score() }?.score() ?: 0f,
                        boundingBox = normalizedBox,
                        distanceMeters = distance
                    )
                } else {
                    null
                }
        }
    }
}
