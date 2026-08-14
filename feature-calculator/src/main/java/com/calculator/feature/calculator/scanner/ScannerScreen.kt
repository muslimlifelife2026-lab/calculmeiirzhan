package com.calculator.feature.calculator.scanner

import android.Manifest
import android.content.Context
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FlashOff
import com.calculator.core.ui.theme.ElectricViolet
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    onResult: (String) -> Unit,
    onClose: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    if (cameraPermissionState.status.isGranted) {
        CameraPreview(onResult = onResult, onClose = onClose)
    } else {
        LaunchedEffect(Unit) {
            cameraPermissionState.launchPermissionRequest()
        }
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("Требуется разрешение на использование камеры", color = Color.White)
            Button(
                onClick = { cameraPermissionState.launchPermissionRequest() },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp)
            ) {
                Text("Разрешить")
            }
        }
    }
}

@Composable
private fun CameraPreview(
    onResult: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    
    val imageCapture = remember { ImageCapture.Builder().build() }
    val mathOcrEngine = remember { MathOcrEngine() }
    
    var isProcessing by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    // State for visual focus ring position and animation
    var tapOffset by remember { mutableStateOf<Offset?>(null) }
    val focusRingScale = remember { Animatable(1.5f) }
    val focusRingAlpha = remember { Animatable(0f) }

    // Infinite laser sweep animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    this.scaleType = PreviewView.ScaleType.FILL_CENTER
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                
                previewViewRef = previewView
                previewView
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(camera, previewViewRef) {
                    detectTapGestures { offset ->
                        val cam = camera ?: return@detectTapGestures
                        val view = previewViewRef ?: return@detectTapGestures
                        tapOffset = offset
                        coroutineScope.launch {
                            focusRingAlpha.snapTo(1f)
                            focusRingScale.snapTo(1.5f)
                            focusRingScale.animateTo(1f, tween(300))
                            focusRingAlpha.animateTo(0f, tween(500, delayMillis = 300))
                            tapOffset = null
                        }

                        // Trigger autofocus at tapped point
                        val factory = view.meteringPointFactory
                        val point = factory.createPoint(offset.x, offset.y)
                        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF).build()
                        cam.cameraControl.startFocusAndMetering(action)
                    }
                }
        )
        
        // Viewfinder overlay + Laser line
        Box(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val boxWidth = size.width * 0.9f
                val boxHeight = size.height * 0.35f
                val left = (size.width - boxWidth) / 2
                val top = (size.height - boxHeight) / 2
                val right = left + boxWidth
                val bottom = top + boxHeight
                
                // Draw dark overlay (4 rectangles around the clear center)
                val overlayColor = Color(0x99000000)
                drawRect(overlayColor, topLeft = Offset(0f, 0f), size = Size(size.width, top))
                drawRect(overlayColor, topLeft = Offset(0f, top), size = Size(left, boxHeight))
                drawRect(overlayColor, topLeft = Offset(right, top), size = Size(size.width - right, boxHeight))
                drawRect(overlayColor, topLeft = Offset(0f, bottom), size = Size(size.width, size.height - bottom))

                val strokeWidth = 4.dp.toPx()
                val cornerLength = 24.dp.toPx()
                val color = ElectricViolet
                
                // Top Left
                drawLine(color, Offset(left, top), Offset(left + cornerLength, top), strokeWidth)
                drawLine(color, Offset(left, top), Offset(left, top + cornerLength), strokeWidth)
                // Top Right
                drawLine(color, Offset(right, top), Offset(right - cornerLength, top), strokeWidth)
                drawLine(color, Offset(right, top), Offset(right, top + cornerLength), strokeWidth)
                // Bottom Left
                drawLine(color, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth)
                drawLine(color, Offset(left, bottom), Offset(left, bottom - cornerLength), strokeWidth)
                // Bottom Right
                drawLine(color, Offset(right, bottom), Offset(right - cornerLength, bottom), strokeWidth)
                drawLine(color, Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth)

                // Laser sweep line
                val laserPosition = top + boxHeight * laserY
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            ElectricViolet,
                            Color.Transparent
                        )
                    ),
                    start = Offset(left, laserPosition),
                    end = Offset(right, laserPosition),
                    strokeWidth = 2.dp.toPx()
                )
            }
            
            // Text hint below the frame
            Text(
                text = "Наведите камеру на пример",
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 160.dp)
            )
        }

        // Animated Focus Ring
        tapOffset?.let { offset ->
            Box(
                modifier = Modifier
                    .offset(
                        x = (offset.x / LocalContext.current.resources.displayMetrics.density).dp - 24.dp,
                        y = (offset.y / LocalContext.current.resources.displayMetrics.density).dp - 24.dp
                    )
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .background(Color.White.copy(alpha = focusRingAlpha.value * 0.2f))
            )
        }

        // Header controls (Close and Flashlight)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close Scanner",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(
                onClick = {
                    val cam = camera ?: return@IconButton
                    isTorchEnabled = !isTorchEnabled
                    cam.cameraControl.enableTorch(isTorchEnabled)
                }
            ) {
                Icon(
                    imageVector = if (isTorchEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                    contentDescription = "Toggle Flashlight",
                    tint = if (isTorchEnabled) Color.Yellow else Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        // Scan Button
        Button(
            onClick = {
                if (isProcessing) return@Button
                isProcessing = true
                
                imageCapture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(imageProxy: ImageProxy) {
                            coroutineScope.launch {
                                val text = withContext(Dispatchers.IO) {
                                    mathOcrEngine.processImage(imageProxy)
                                }
                                isProcessing = false
                                if (!text.isNullOrBlank()) {
                                    onResult(text)
                                } else {
                                    android.widget.Toast.makeText(context, "Текст не найден или не похож на формулу. Поднесите ближе.", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            isProcessing = false
                            android.widget.Toast.makeText(context, "Ошибка камеры: ${exception.message}", android.widget.Toast.LENGTH_SHORT).show()
                            exception.printStackTrace()
                        }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .size(80.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
        ) {
            if (isProcessing) {
                CircularProgressIndicator(color = Color.White)
            } else {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = "Capture Image",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

