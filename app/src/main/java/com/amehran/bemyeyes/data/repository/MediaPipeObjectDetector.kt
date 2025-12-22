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

        // Inference
        val detectionResult = detector.detect(mpImage)

        // Mapping to Domain Model
        val detections = detectionResult.detections().mapNotNull { detection ->
            val topCategory = detection.categories().maxByOrNull { it.score() }
            
            if (topCategory != null && relevantLabels.contains(topCategory.categoryName())) {
                val box = detection.boundingBox() // RectF
                
                // Normalize coordinates (0..1) based on input bitmap size
                val normalizedBox = android.graphics.RectF(
                    box.left / bitmap.width,
                    box.top / bitmap.height,
                    box.right / bitmap.width,
                    box.bottom / bitmap.height
                )
                
                // Estimate Distance
                val label = topCategory.categoryName()
                val realHeight = objectHeights[label]
                val distance = if (realHeight != null) {
                    // Simple heuristic: Distance = RealHeight / (NormalizedBoxHeight * K)
                    // Assuming K ~ 1.0 for typical phone FOV (~60 deg vertical)
                    // D = RealHeight / normalizedBox.height()
                    realHeight / normalizedBox.height()
                } else {
                    null
                }

                Detection(
                    label = label,
                    confidence = topCategory.score(),
                    boundingBox = normalizedBox,
                    distanceMeters = distance
                )
            } else {
                null
            }
        }

        return detections
    }
}
