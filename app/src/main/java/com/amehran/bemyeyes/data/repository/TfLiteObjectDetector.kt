package com.amehran.bemyeyes.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.amehran.bemyeyes.domain.model.Detection
import com.amehran.bemyeyes.domain.repository.ObjectDetector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector as TfLiteTaskDetector

class TfLiteObjectDetector(
    private val context: Context,
    private val modelPath: String = "model.tflite",
    private val confidenceThreshold: Float = 0.5f
) : ObjectDetector { // This now correctly implements OUR interface

    private val objectDetector: TfLiteTaskDetector

    init {
        val options = TfLiteTaskDetector.ObjectDetectorOptions.builder()
            .setMaxResults(5)
            .setScoreThreshold(confidenceThreshold)
            .build()
        objectDetector = TfLiteTaskDetector.createFromFileAndOptions(context, modelPath, options)
    }

    override fun detect(bitmap: Bitmap): Flow<List<Detection>> = flow {
        // The Task Library expects a TensorImage.
        val tensorImage = TensorImage.fromBitmap(bitmap)

        // The Task Library handles all the complex output parsing automatically.
        val results = objectDetector.detect(tensorImage)

        // Map the library's results to our domain model
        val detections = results.mapNotNull { detectionResult ->
            // Take the first and highest-confidence category for each detected object.
            detectionResult.categories.firstOrNull()?.let { category ->
                Detection(category.label, category.score)
            }
        }
        emit(detections)
    }
}
