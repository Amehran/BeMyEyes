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
    private val modelPath: String = "model.tflite",
    private val confidenceThreshold: Float = 0.55f // A better threshold for EfficientDet
) : ObjectDetector {

    private val objectDetector: TfLiteTaskDetector
    private val imageProcessor: ImageProcessor

    init {
        // REMOVED .useGpu() to prevent the crash. We will run on the CPU for now.
        val baseOptions = BaseOptions.builder().build()
        val options = TfLiteTaskDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setMaxResults(5)
            .setScoreThreshold(confidenceThreshold)
            .build()
        objectDetector = TfLiteTaskDetector.createFromFileAndOptions(context, modelPath, options)

        // Set for EfficientDet-Lite1
        val modelInputWidth = 384
        val modelInputHeight = 384
        imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(modelInputHeight, modelInputWidth, ResizeOp.ResizeMethod.BILINEAR))
            .build()
    }

    override fun detect(bitmap: Bitmap): Flow<List<Detection>> = flow {
        val tensorImage = TensorImage.fromBitmap(bitmap)
        val processedImage = imageProcessor.process(tensorImage)

        val results = objectDetector.detect(processedImage)

        val detections = results.mapNotNull { detectionResult ->
            detectionResult.categories.firstOrNull()?.let {
                Detection(it.label, it.score)
            }
        }
        emit(detections)
    }
}
