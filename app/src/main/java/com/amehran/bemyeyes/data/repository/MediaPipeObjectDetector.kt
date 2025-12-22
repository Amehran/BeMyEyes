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
    private val modelPath: String = "efficientdet-lite0.tflite",
    private val confidenceThreshold: Float = 0.5f
) : ObjectDetector {

    private var objectDetector: MPObjectDetector? = null

    // Whitelist of objects relevant for blind navigation
    private val relevantLabels = setOf(
        // Outdoor / Traffic
        "person", "bicycle", "car", "motorcycle", "bus", "truck", "traffic light", "fire hydrant", "stop sign", "parking meter", "bench",
        // Indoor / Obstacles
        "chair", "couch", "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "door", "stairs"
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
            .setMaxResults(5)
            .build()

        objectDetector = MPObjectDetector.createFromOptions(context, options)
    }

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
                Detection(
                    label = topCategory.categoryName(),
                    confidence = topCategory.score(),
                    boundingBox = box
                )
            } else {
                null
            }
        }

        return detections
    }
}
