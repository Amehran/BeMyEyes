package com.amehran.bemyeyes.presentation.ui.camera

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.window.Dialog
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraScreen(viewModel: CameraViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCamPermission by remember { mutableStateOf(false) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasCamPermission = permissions[Manifest.permission.CAMERA] == true
        }
    )
    LaunchedEffect(key1 = true) {
        launcher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    val detections by viewModel.detections.collectAsState()

    val isRealtimeDetectionEnabled by viewModel.isRealtimeDetectionEnabled.collectAsState()
    val isCurtainMode by viewModel.isCurtainMode.collectAsState()
    val isCloudMode by viewModel.isCloudMode.collectAsState()
    val isFarsi by viewModel.isFarsi.collectAsState()
    val isPowerSaverMode by viewModel.isPowerSaverMode.collectAsState()
    val isOutdoorMode by viewModel.isOutdoorMode.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val lastDescription by viewModel.lastDescription.collectAsState()
    val isTtsEnabled by viewModel.isTtsEnabled.collectAsState()
    
    var showSettings by remember { mutableStateOf(false) }

    if (hasCamPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraPreview(context, lifecycleOwner, isPowerSaverMode, viewModel::detect)
            
            if (isCurtainMode) {
                 Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .semantics { 
                            contentDescription = "Curtain Mode Active. Double tap to exit. Tap to Describe." 
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { 
                                    viewModel.setCurtainMode(false) 
                                },
                                onTap = {
                                    viewModel.onDescribeScene()
                                }
                            )
                        }
                ) {
                    Text(
                        text = "Curtain Mode Active\nDouble tap to exit\nTap to Describe",
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            } else {
                 // Normal Camera UI
                 Box(
                     modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    viewModel.setCurtainMode(true)
                                },
                                onTap = {
                                    viewModel.onDescribeScene()
                                }
                            )
                        }
                 ) {
                     if (isRealtimeDetectionEnabled) {
                         DetectionOverlay(detections = detections)
                     }
                    
                     // App Voice is managed via Settings. 
                     // No visual captioning per user request.

                     // Settings Button (Top Right)
                     IconButton(
                        onClick = { 
                            showSettings = true 
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 48.dp, end = 16.dp)
                            .zIndex(1f) 
                            .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                     ) {
                         Icon(
                             imageVector = Icons.Filled.Settings,
                             contentDescription = "Settings",
                             tint = Color.White
                         )
                     }

                     // Voice Assistant Button (Bottom Bar)
                     Button(
                        onClick = { 
                            if (isListening) viewModel.stopListening() else viewModel.startListening()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListening) Color.Red else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(0.25f)
                            .zIndex(1f)
                     ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                             Icon(
                                 imageVector = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                                 contentDescription = if (isListening) "Stop Listening" else "Start Voice Assistant",
                                 modifier = Modifier.size(48.dp)
                             )
                             Text(
                                 text = if (isListening) "Listening..." else "Tap to Speak",
                                 style = MaterialTheme.typography.headlineSmall
                             )
                        }
                     }
                 }
            }
            
            // Settings Overlay
            if (showSettings) {
                val isCurtainMode by viewModel.isCurtainMode.collectAsState()
                val isCloudMode by viewModel.isCloudMode.collectAsState()
                val isFarsi by viewModel.isFarsi.collectAsState()
                val isRealtimeDetectionEnabled by viewModel.isRealtimeDetectionEnabled.collectAsState()
                val isPowerSaver by viewModel.isPowerSaverMode.collectAsState()
                val isOutdoor by viewModel.isOutdoorMode.collectAsState()
                
                // Full Screen Settings Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f) // Higher than everything else
                ) {
                     com.amehran.bemyeyes.presentation.ui.settings.SettingsScreen(
                         onDismiss = { 
                             showSettings = false 
                         },
                         isCurtainMode = isCurtainMode,
                         onCurtainModeChange = viewModel::setCurtainMode,
                         isCloudMode = isCloudMode,
                         onCloudModeChange = viewModel::setCloudMode,
                         isFarsi = isFarsi,
                         onLanguageChange = viewModel::setLanguageFarsi,
                         isRealtimeDetectionEnabled = isRealtimeDetectionEnabled,
                         onRealtimeDetectionChange = viewModel::setRealtimeDetectionEnabled,
                         isPowerSaverMode = isPowerSaver,
                         onPowerSaverChange = viewModel::setPowerSaverMode,
                         isOutdoorMode = isOutdoor,
                         onOutdoorModeChange = viewModel::setOutdoorMode,
                         isTtsEnabled = isTtsEnabled,
                         onTtsChange = viewModel::setTtsEnabled
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
    isPowerSaverMode: Boolean,
    onDetect: (androidx.camera.core.ImageProxy) -> Unit
) {
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Rebuild camera stack when Power Saver Mode toggles
    key(isPowerSaverMode) {
        AndroidView(
            factory = {
                val previewView = PreviewView(it)
                previewView.keepScreenOn = true
                val cameraProviderFuture = ProcessCameraProvider.getInstance(it)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build()
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    // Power Saver: VGA vs HD
                    val targetSize = if (isPowerSaverMode) android.util.Size(640, 480) else android.util.Size(1280, 720)

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(targetSize)
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
}