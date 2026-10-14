package cn.xzbim.stoneai.ui.camera

import android.content.Context
import android.net.Uri
import android.util.Rational
import android.view.Surface
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import cn.xzbim.stoneai.ui.theme.AppShapes
import cn.xzbim.stoneai.ui.theme.AppSpacing
import cn.xzbim.stoneai.ui.components.STONE_PHOTO_ASPECT_RATIO
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * CameraX 高性能拍照页面 (Soft & Friendly 自然重构风，含点按聚焦、双指缩放、闪光灯预曝光与 1:1 宽高比一致性)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onBackClick: () -> Unit,
    onImageCaptured: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var hasFlashUnit by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }

    // 点按聚焦状态
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var isFocusing by remember { mutableStateOf(false) }
    var focusSucceeded by remember { mutableStateOf<Boolean?>(null) }

    // 双指缩放状态
    var currentZoomRatio by remember { mutableFloatStateOf(1f) }
    var showZoomIndicator by remember { mutableStateOf(false) }

    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setFlashMode(flashMode)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    // 缩放提示自动隐藏
    LaunchedEffect(currentZoomRatio) {
        if (showZoomIndicator) {
            delay(1200)
            showZoomIndicator = false
        }
    }

    // 聚焦框自动淡出
    LaunchedEffect(focusPoint, focusSucceeded) {
        if (focusPoint != null) {
            delay(1200)
            focusPoint = null
            focusSucceeded = null
        }
    }

    DisposableEffect(lensFacing, flashMode) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // 1. 统一 4:3 架构 AspectRatio
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            imageCapture.flashMode = flashMode

            // 2. 统一 1:1 方形视口 (ViewPort) 约束 Crop Rect，保障“所见即所得”
            val viewPort = ViewPort.Builder(
                Rational(1, 1),
                previewView.display?.rotation ?: Surface.ROTATION_0
            ).setScaleType(ViewPort.FILL_CENTER).build()

            val useCaseGroup = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(imageCapture)
                .setViewPort(viewPort)
                .build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider.unbindAll()
                val boundCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    useCaseGroup
                )

                camera = boundCamera
                hasFlashUnit = boundCamera.cameraInfo.hasFlashUnit()
                currentZoomRatio = boundCamera.cameraInfo.zoomState.value?.zoomRatio ?: 1f
            } catch (_: Exception) {
                // 忽略镜头绑定异常
            }
        }, executor)

        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (_: Exception) {
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "拍摄石头",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // 闪光灯切换 (检测硬件是否支持)
                    if (hasFlashUnit) {
                        IconButton(
                            onClick = {
                                flashMode = when (flashMode) {
                                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                                    ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                                    else -> ImageCapture.FLASH_MODE_OFF
                                }
                            }
                        ) {
                            val icon = when (flashMode) {
                                ImageCapture.FLASH_MODE_ON -> Icons.Rounded.FlashOn
                                ImageCapture.FLASH_MODE_AUTO -> Icons.Rounded.FlashAuto
                                else -> Icons.Rounded.FlashOff
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = "闪光灯",
                                tint = Color.White
                            )
                        }
                    }

                    // 切换前后镜头
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cameraswitch,
                            contentDescription = "切换摄像头",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            // CameraPreview 画面容器 (固定 1:1 比例，与照片及照片预览页完全保持一致)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(STONE_PHOTO_ASPECT_RATIO)
                    .pointerInput(Unit) {
                        // 1. 手势 1：双指平滑缩放
                        detectTransformGestures { _, _, zoomChange, _ ->
                            if (zoomChange != 1f && camera != null) {
                                val zoomState = camera?.cameraInfo?.zoomState?.value
                                val minZoom = zoomState?.minZoomRatio ?: 1f
                                val maxZoom = zoomState?.maxZoomRatio ?: 10f
                                val currentZoom = zoomState?.zoomRatio ?: 1f

                                val targetZoom = (currentZoom * zoomChange).coerceIn(minZoom, maxZoom)
                                camera?.cameraControl?.setZoomRatio(targetZoom)
                                currentZoomRatio = targetZoom
                                showZoomIndicator = true
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        // 2. 手势 2：单指点击点按聚焦 (AF/AE/AWB)
                        detectTapGestures { tapOffset ->
                            val activeCamera = camera ?: return@detectTapGestures
                            val meteringFactory = previewView.meteringPointFactory
                            val point = meteringFactory.createPoint(tapOffset.x, tapOffset.y)

                            val action = FocusMeteringAction.Builder(
                                point,
                                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
                            ).setAutoCancelDuration(3, TimeUnit.SECONDS).build()

                            focusPoint = tapOffset
                            isFocusing = true
                            focusSucceeded = null

                            val executor = ContextCompat.getMainExecutor(context)
                            val future = activeCamera.cameraControl.startFocusAndMetering(action)
                            future.addListener({
                                try {
                                    val result = future.get()
                                    focusSucceeded = result.isFocusSuccessful
                                } catch (_: Exception) {
                                    focusSucceeded = false
                                } finally {
                                    isFocusing = false
                                }
                            }, executor)
                        }
                    }
            ) {
                AndroidView(
                    factory = {
                        previewView.apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 聚焦框视觉反馈
                focusPoint?.let { point ->
                    val ringSize by animateDpAsState(
                        targetValue = if (isFocusing) 64.dp else 48.dp,
                        label = "FocusRingSize"
                    )

                    val borderColor = when (focusSucceeded) {
                        true -> MaterialTheme.colorScheme.primary
                        false -> MaterialTheme.colorScheme.error
                        null -> Color.White
                    }

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = (point.x - (ringSize.toPx() / 2)).roundToInt(),
                                    y = (point.y - (ringSize.toPx() / 2)).roundToInt()
                                )
                            }
                            .size(ringSize)
                            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
                    )
                }
            }

            // 拍摄提示条
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = AppSpacing.m, start = AppSpacing.m, end = AppSpacing.m),
                shape = AppShapes.chip,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.6f)
                )
            ) {
                Text(
                    text = "点击画面点按聚焦，让石头纹理更清晰 ✦",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = AppSpacing.m, vertical = AppSpacing.s)
                )
            }

            // 缩放倍率悬浮提示
            AnimatedVisibility(
                visible = showZoomIndicator,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
            ) {
                Surface(
                    shape = AppShapes.chip,
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", currentZoomRatio)}×",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // 底部快门控制区域
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(vertical = AppSpacing.l),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 拍照按钮
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(if (isCapturing) Color.Gray else MaterialTheme.colorScheme.primary)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        enabled = !isCapturing,
                        onClick = {
                            isCapturing = true
                            capturePhoto(
                                context = context,
                                imageCapture = imageCapture,
                                executor = ContextCompat.getMainExecutor(context),
                                onSuccess = { uri ->
                                    isCapturing = false
                                    onImageCaptured(uri)
                                },
                                onError = {
                                    isCapturing = false
                                }
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.PhotoCamera,
                                contentDescription = "拍照",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: Executor,
    onSuccess: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val photoFile = File(
        context.cacheDir,
        "temp_stone_${System.currentTimeMillis()}.jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                onSuccess(savedUri)
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}
