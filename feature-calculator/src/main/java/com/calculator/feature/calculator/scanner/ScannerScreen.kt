package com.calculator.feature.calculator.scanner

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.google.accompanist.permissions.shouldShowRationale
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    onResult: (String) -> Unit,
    onClose: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val engine = remember { MathOcrEngine() }
    val scope = rememberCoroutineScope()
    val currentOnResult by rememberUpdatedState(onResult)
    DisposableEffect(engine) { onDispose { engine.close() } }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        pickerOpen = false
        if (uri != null && !isProcessing) scope.launch {
            isProcessing = true
            error = null
            try {
                val text = engine.processUri(context, uri)
                if (text.isNullOrBlank()) error = scanHelp else currentOnResult(text)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = "Не удалось прочитать фото. Выберите другое изображение."
            } finally { isProcessing = false }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (cameraPermissionState.status.isGranted) {
            CameraPreview(onResult = onResult, onClose = onClose, mathOcrEngine = engine,
                isProcessing = isProcessing, onProcessingChange = { isProcessing = it; if (it) error = null },
                onFailure = { error = it })
        } else {
            Column(Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Решить по фото", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                Text("Выберите снимок печатного примера или двух уравнений. Для съёмки разрешите доступ к камере.", color = Color.White)
                Button(onClick = {
                    if (permissionRequested && !cameraPermissionState.status.shouldShowRationale) {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")))
                    } else {
                        permissionRequested = true
                        cameraPermissionState.launchPermissionRequest()
                    }
                }, enabled = !isProcessing && !pickerOpen) {
                    Text(if (permissionRequested && !cameraPermissionState.status.shouldShowRationale) "Настройки камеры" else "Разрешить камеру")
                }
            }
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp)) {
                Icon(Icons.Rounded.Close, "Закрыть сканер", tint = Color.White)
            }
        }
        error?.let { message ->
            Surface(color = Color(0xEE20252C), shape = RoundedCornerShape(16.dp),
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                    .padding(horizontal = 24.dp).padding(bottom = 190.dp)) {
                Text(message, color = Color.White, modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        Button(onClick = {
            pickerOpen = true
            try { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            catch (_: android.content.ActivityNotFoundException) {
                pickerOpen = false
                error = "Не удалось открыть выбор фото на этом устройстве."
            }
        }, enabled = !isProcessing && !pickerOpen,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp)) {
            Text(if (isProcessing) "Распознавание…" else "Выбрать фото")
        }
    }
}

private const val scanHelp = "Не удалось разобрать пример. Нужны чёткий печатный текст и обе части уравнения. Дроби записывайте через /. Можно ввести задачу вручную после закрытия сканера."

@Composable
private fun CameraPreview(
    onResult: (String) -> Unit,
    onClose: () -> Unit,
    mathOcrEngine: MathOcrEngine,
    isProcessing: Boolean,
    onProcessingChange: (Boolean) -> Unit,
    onFailure: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraProviderFuture = remember(context) { ProcessCameraProvider.getInstance(context) }
    val disposed = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    DisposableEffect(lifecycleOwner) {
        disposed.set(false)
        onDispose {
            disposed.set(true)
            camera?.cameraControl?.enableTorch(false)
            if (cameraProviderFuture.isDone) {
                runCatching { cameraProviderFuture.get().unbindAll() }
            }
        }
    }
    
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
                
                cameraProviderFuture.addListener({
                    if (disposed.get()) return@addListener
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
                        android.widget.Toast.makeText(ctx, "Не удалось открыть камеру", android.widget.Toast.LENGTH_LONG).show()
                        android.util.Log.e("ScannerScreen", "Camera binding failed", e)
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
                text = "Печатный пример или два уравнения",
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
                    contentDescription = "Закрыть сканер",
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
                    contentDescription = "Фонарик",
                    tint = if (isTorchEnabled) Color.Yellow else Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        // Scan Button
        Button(
            onClick = {
                if (isProcessing) return@Button
                onProcessingChange(true)
                
                try { imageCapture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(imageProxy: ImageProxy) {
                            if (disposed.get()) { imageProxy.close(); return }
                            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                                try {
                                    val text = mathOcrEngine.processImage(imageProxy,
                                        previewViewRef?.width ?: 0, previewViewRef?.height ?: 0)
                                    if (!text.isNullOrBlank()) onResult(text) else onFailure(scanHelp)
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    onFailure("Не удалось обработать снимок. Попробуйте ещё раз.")
                                } finally { onProcessingChange(false) }
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            if (!disposed.get()) {
                                onProcessingChange(false)
                                onFailure("Не удалось сделать снимок. Проверьте доступ к камере.")
                            }
                        }
                    }
                ) } catch (_: IllegalStateException) {
                    onProcessingChange(false)
                    onFailure("Камера ещё не готова. Попробуйте через секунду.")
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .size(80.dp),
            enabled = !isProcessing,
            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
        ) {
            if (isProcessing) {
                CircularProgressIndicator(color = com.calculator.core.ui.theme.Background)
            } else {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = "Сфотографировать пример",
                    tint = com.calculator.core.ui.theme.Background,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

