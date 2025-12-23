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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
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



    var isCurtainMode by remember { mutableStateOf(true) }

    if (hasCamPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraPreview(context, lifecycleOwner, viewModel::detect)
            
            // Visual Boxes Overlay (Hide in curtain mode for true blackness? Or keep for debug? Hide is safer for privacy)
            if (!isCurtainMode) {
                DetectionOverlay(detections = detections)
                
                // Privacy / Curtain Mode Button (Top Right)
                IconButton(
                    onClick = { isCurtainMode = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 16.dp)
                        .zIndex(1f) // Ensure button is always on top of detection layers
                        .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Enter Curtain Mode",
                        tint = Color.White
                    )
                }
            } else {
                // Curtain Mode Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { isCurtainMode = false }
                            )
                        }
                ) {
                    Text(
                        text = "Curtain Mode Active\nDouble tap to exit",
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onDetect: (androidx.camera.core.ImageProxy) -> Unit
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
            previewView.keepScreenOn = true
            val cameraProviderFuture = ProcessCameraProvider.getInstance(it)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(android.util.Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also {
                        var lastAnalyzedTimestamp = 0L

                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            // Throttle analysis to ~5 FPS (200ms)
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastAnalyzedTimestamp < 200) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            lastAnalyzedTimestamp = currentTime

                            // Pass directly to VM. VM is responsible for closing imageProxy!
                            onDetect(imageProxy)
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