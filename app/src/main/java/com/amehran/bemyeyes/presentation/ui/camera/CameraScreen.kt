package com.amehran.bemyeyes.presentation.ui.camera

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraScreen(viewModel: CameraViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCamPermission by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCamPermission = granted
        }
    )
    LaunchedEffect(key1 = true) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    val detections by viewModel.detections.collectAsState()



    if (hasCamPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraPreview(context, lifecycleOwner, viewModel::detect)
            
            // Visual Boxes Overlay
            DetectionOverlay(detections = detections)
            
            // Text Log Overlay (Bottom)
            if (detections.isNotEmpty()) {
                val detectionText = detections.joinToString { "${it.label} ${(it.confidence*100).toInt()}%" }
                Text(
                    text = detectionText,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun CameraPreview(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onDetect: (Bitmap) -> Unit
) {
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = {
            val previewView = PreviewView(it)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(it)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(android.util.Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            // Correctly rotate the bitmap
                            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                            val bitmap = imageProxy.toBitmap()
                            
                            // Log.d("CameraScreen", "Original: ${bitmap.width}x${bitmap.height}, Rotation: $rotationDegrees")

                            // If the image is rotated (e.g., portrait mode is 90 deg), we must rotate the bitmap
                            // 'toBitmap()' converts YUV to Bitmap but DOES NOT apply rotation automatically solely based on ImageInfo.
                            // However, we can use a Matrix or check if 'toBitmap()' handles it.
                            // Actually, 'toBitmap()' *tries* to respect it but often needs explicit handling if passed to ML Kit or TFLite.
                            // Since we are passing a plain Bitmap to MediaPipe, MediaPipe expects it upright.
                            
                            // Let's ensure rotation.
                            val matrix = android.graphics.Matrix()
                            matrix.postRotate(rotationDegrees.toFloat())
                            val rotatedBitmap = Bitmap.createBitmap(
                                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                            )
                            
                            // Log.d("CameraScreen", "Rotated: ${rotatedBitmap.width}x${rotatedBitmap.height}")

                            onDetect(rotatedBitmap)
                            imageProxy.close()
                        }
                    }

                preview.setSurfaceProvider(previewView.surfaceProvider)
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    // Log the error
                }
            }, ContextCompat.getMainExecutor(it))
            previewView
        },
        modifier = Modifier.fillMaxSize().testTag("camera_preview")
    )
}