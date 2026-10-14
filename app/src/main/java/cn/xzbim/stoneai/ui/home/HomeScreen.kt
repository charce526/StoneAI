package cn.xzbim.stoneai.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.xzbim.stoneai.data.local.db.RecognitionHistoryEntity
import cn.xzbim.stoneai.model.ImageSource
import cn.xzbim.stoneai.ui.components.EmptyState
import cn.xzbim.stoneai.ui.components.HistoryItemCard
import cn.xzbim.stoneai.ui.theme.AppShapes
import cn.xzbim.stoneai.ui.theme.AppSpacing
import cn.xzbim.stoneai.viewmodel.HomeViewModel
import cn.xzbim.stoneai.viewmodel.ImageSelectionViewModel
import cn.xzbim.stoneai.viewmodel.SettingsViewModel

/**
 * 首页/识别页 (Soft & Friendly 自然探索风，高信息密度优化版)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToImagePreview: () -> Unit,
    onNavigateToAIService: () -> Unit,
    onHistoryItemClick: (RecognitionHistoryEntity) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    imageSelectionViewModel: ImageSelectionViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val settingsUiState by settingsViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPermissionDeniedDialog by remember { mutableStateOf(false) }

    // 使用 OpenDocument 获取用户明确选择的原始文件内容，避免 Photo Picker 脱敏 EXIF GPS。
    val photoDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            imageSelectionViewModel.setSelectedImage(uri, ImageSource.GALLERY)
            onNavigateToImagePreview()
        }
    }

    val galleryLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        // 定位 EXIF 权限是可选的；即使用户拒绝，也继续允许选择和识别照片。
        photoDocumentLauncher.launch(arrayOf("image/*"))
    }

    // Camera & Optional Location Permission Contract
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        if (cameraGranted) {
            onNavigateToCamera()
        } else {
            showPermissionDeniedDialog = true
        }
    }

    fun handleCameraClick() {
        val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (cameraGranted) {
            onNavigateToCamera()
        } else {
            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun handleGalleryClick() {
        val needsMediaLocationPermission =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_MEDIA_LOCATION
                ) != PackageManager.PERMISSION_GRANTED

        if (needsMediaLocationPermission) {
            galleryLocationPermissionLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
        } else {
            photoDocumentLauncher.launch(arrayOf("image/*"))
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI 识石",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 22.spMax,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.m, vertical = AppSpacing.s),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 主/次级两大操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)
            ) {
                // 拍照识别 (主按钮)
                Button(
                    onClick = { handleCameraClick() },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = AppShapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text(
                        text = "拍照识别",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // 从相册选择 (次按钮)
                Button(
                    onClick = { handleGalleryClick() },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = AppShapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text(
                        text = "从相册选择",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.l))

            // 最近发现 Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "最近发现",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.s))

            // 最近发现区域 (高信息密度列表)
            if (uiState.recentRecords.isEmpty()) {
                EmptyState(
                    message = "这里还没有发现记录",
                    description = "去拍一块有趣的石头吧，探索藏在它背后的地球秘密",
                    actionLabel = "去识别",
                    onActionClick = { handleCameraClick() }
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    uiState.recentRecords.forEach { entity ->
                        HistoryItemCard(
                            entity = entity,
                            onClick = { onHistoryItemClick(entity) }
                        )
                    }
                }
            }
        }
    }

    // 首次启动欢迎与配置引导对话框
    if (settingsUiState.isFirstLaunch) {
        AlertDialog(
            onDismissRequest = { settingsViewModel.completeFirstLaunch() },
            shape = AppShapes.large,
            title = {
                Text(
                    text = "欢迎使用 AI 识石 ✦",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "拍一张照片，看看这块石头藏着什么故事。\n\n使用前需要配置一个支持图片识别的 AI 服务。",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        settingsViewModel.completeFirstLaunch()
                        onNavigateToAIService()
                    },
                    shape = AppShapes.button
                ) {
                    Text("配置 AI")
                }
            },
            dismissButton = {
                TextButton(onClick = { settingsViewModel.completeFirstLaunch() }) {
                    Text("稍后再说")
                }
            }
        )
    }

    // 相机权限拒绝提示对话框
    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            shape = AppShapes.large,
            title = {
                Text(
                    text = "需要相机权限",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "拍摄石头需要相机权限。您可以允许权限以使用拍摄功能，或者直接从系统相册选择照片。",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDeniedDialog = false
                        handleGalleryClick()
                    }
                ) {
                    Text("从相册选择")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPermissionDeniedDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("前往设置")
                }
            }
        )
    }
}

private val Int.spMax get() = TextUnit(this.toFloat(), TextUnitType.Sp)
