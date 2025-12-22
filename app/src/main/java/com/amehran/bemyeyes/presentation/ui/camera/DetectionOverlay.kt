package com.amehran.bemyeyes.presentation.ui.camera

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.amehran.bemyeyes.domain.model.Detection

@Composable
fun DetectionOverlay(
    detections: List<Detection>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // Assume default camera resolution for scaling (e.g. 640x480 or similar)
        // Ideally we should pass the source image size to scale correctly.
        // For now, let's assume the bounding boxes are normalized or in the same coordinate space 
        // as the view if the view fills the screen.
        // NOTE: MediaPipe often returns coordinates relative to the input image. 
        // If the `Detection` model has absolute pixel coordinates from a 320x320 input,
        // we need to scale them to the screen size.
        
        // However, looking at previous `CameraViewModel` code, it seemed to just check height/320f.
        // Let's assume the detection.boundingBox is in 320x320 coordinates (from TFLite efficientdet model).
        // WE NEED TO SCALE.

        // Model Input Size
        val modelWidth = 320f
        val modelHeight = 320f

        val scaleX = size.width / modelWidth
        val scaleY = size.height / modelHeight

        val paint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 40f
            style = Paint.Style.FILL
        }

        detections.forEach { detection ->
            val box = detection.boundingBox
            
            // Scale Model Coords -> Screen Coords
            // Note: Camera feed might be rotated/cropped. This is a "best effort" overlay.
            val left = box.left * scaleX
            val top = box.top * scaleY
            val width = box.width() * scaleX
            val height = box.height() * scaleY

            drawRect(
                color = Color.Green,
                topLeft = Offset(left, top),
                size = Size(width, height),
                style = Stroke(width = 8.dp.toPx())
            )

            // Draw Label
            drawContext.canvas.nativeCanvas.drawText(
                "${detection.label} ${(detection.confidence * 100).toInt()}%",
                left,
                top - 10,
                paint
            )
        }
    }
}
