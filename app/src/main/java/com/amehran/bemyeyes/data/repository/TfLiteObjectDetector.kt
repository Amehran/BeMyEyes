package com.amehran.bemyeyes.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.amehran.bemyeyes.domain.model.Detection
import com.amehran.bemyeyes.domain.repository.ObjectDetector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.ObjectDetector as TfLiteTaskDetector

class TfLiteObjectDetector(
    private val context: Context,
    private val modelPath: String = "efficientdet-lite0.tflite",
    private val confidenceThreshold: Float = 0.70f
) : ObjectDetector {

    private val objectDetector: TfLiteTaskDetector
    private val imageProcessor: ImageProcessor

    // Whitelist of objects relevant for blind navigation to reduce noise/hallucinations
    private val relevantLabels = setOf(
        // Outdoor / Traffic
        "person", "bicycle", "car", "motorcycle", "bus", "truck", "traffic light", "fire hydrant", "stop sign", "parking meter", "bench",
        // Indoor / Obstacles
        "chair", "couch", "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "door", "stairs" // Note: 'door' and 'stairs' might not be in COCO, but keeping for intent.
    )

    init {
        // REMOVED .useGpu() to prevent the crash. We will run on the CPU for now.
        // Enable multi-threading for CPU inference to improve speed
        val baseOptions = BaseOptions.builder().setNumThreads(4).build()
        val options = TfLiteTaskDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setMaxResults(5)
            .setScoreThreshold(confidenceThreshold)
            .build()
        
        objectDetector = TfLiteTaskDetector.createFromFileAndOptions(context, modelPath, options)

        // Set for EfficientDet-Lite0
        val modelInputWidth = 320
        val modelInputHeight = 320
        imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(modelInputHeight, modelInputWidth, ResizeOp.ResizeMethod.BILINEAR))
            .build()
    }

    override suspend fun detect(bitmap: Bitmap): List<Detection> {
        val tensorImage = TensorImage.fromBitmap(bitmap)
        val processedImage = imageProcessor.process(tensorImage)

        val results = objectDetector.detect(processedImage)

        val detections = results.mapNotNull { detectionResult ->
            detectionResult.categories.firstOrNull()?.let { category ->
                if (relevantLabels.contains(category.label)) {
                    Detection(
                        label = category.label,
                        confidence = category.score,
                        boundingBox = detectionResult.boundingBox
                    )
                } else {
                    null
                }
            }
        }

        val uniqueDetectionsMap = mutableMapOf<String, Detection>()
        for (detection in detections) {
            val existing = uniqueDetectionsMap[detection.label]
            if (existing == null || detection.confidence > existing.confidence) {
                uniqueDetectionsMap[detection.label] = detection
            }
        }

        return uniqueDetectionsMap.values.toList()
    }
}
