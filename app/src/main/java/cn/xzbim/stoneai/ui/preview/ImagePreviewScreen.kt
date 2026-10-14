package cn.xzbim.stoneai.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import cn.xzbim.stoneai.model.ImageSource
import cn.xzbim.stoneai.ui.theme.AppShapes
import cn.xzbim.stoneai.ui.theme.AppSpacing
import cn.xzbim.stoneai.ui.components.STONE_PHOTO_ASPECT_RATIO
import cn.xzbim.stoneai.viewmodel.ImageSelectionViewModel

/**
 * 统一图片预览、处理与多模态 AI 图文识别页面 (Soft & Friendly 自然重构风，高信息密度一屏显示优化版)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePreviewScreen(
    onBackClick: () -> Unit,
    onReselectClick: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToResult: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImageSelectionViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedImage = uiState.selectedImage
    val isRunning = uiState.isProcessingImage || uiState.isAnalyzingWithAI

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onErrorMessageShown()
        }
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onUserMessageShown()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "照片预览",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (selectedImage == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("未选择照片")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppSpacing.m, vertical = AppSpacing.s),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 与相机取景保持一致：1:1 视口、居中裁切，确保所见构图一致
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(STONE_PHOTO_ASPECT_RATIO)
                        .align(Alignment.CenterHorizontally),
                    shape = AppShapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = selectedImage.uri,
                            contentDescription = "选中石头预览图",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(AppShapes.large)
                        )
                    }
                }

                // AI 分析进度卡片
                if (isRunning) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppSpacing.m),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.s))
                            Text(
                                text = uiState.analysisStatusMessage ?: "AI 正在分析中...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.cancelAnalysis() }) {
                                Text("取消", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                // 拍摄小提示卡片 (紧凑型)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(AppSpacing.m),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.s))
                        Column {
                            Text(
                                text = "拍摄小提示 ✦",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "尽量保证光线充足，让石头的颜色、颗粒与纹理更清楚",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 操作按钮组：换一张 / 开始识别
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)
                ) {
                    // 换一张 / 重新拍摄 (次按钮)
                    OutlinedButton(
                        onClick = onReselectClick,
                        enabled = !isRunning,
                        shape = AppShapes.button,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                        Text(
                            text = if (selectedImage.source == ImageSource.CAMERA) "重新拍摄" else "换一张",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    // 开始识别 (主按钮)
                    Button(
                        onClick = { viewModel.startRecognitionFlow(context, onSuccessNavigateToResult = onNavigateToResult) },
                        enabled = !isRunning,
                        shape = AppShapes.button,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        if (isRunning) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = AppSpacing.xs),
                                strokeWidth = 2.dp
                            )
                            Text("分析中...")
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.xs))
                            Text(
                                text = "开始识别",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }

    // 未配置 AI 服务拦截对话框
    if (uiState.showNoActiveConfigDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNoActiveConfigDialog() },
            shape = AppShapes.large,
            title = {
                Text(
                    text = "还没有配置 AI 服务 ✦",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "识图需要调用支持图片识别的 AI 大模型服务。请先前往“设置 -> AI 服务”添加并激活一个 AI 服务配置。",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissNoActiveConfigDialog()
                        onNavigateToSettings()
                    },
                    shape = AppShapes.button
                ) {
                    Text("前往设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissNoActiveConfigDialog() }) {
                    Text("取消")
                }
            }
        )
    }

    // 缺少 API Key 拦截对话框
    if (uiState.showNoApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNoApiKeyDialog() },
            shape = AppShapes.large,
            title = {
                Text(
                    text = "AI 服务缺少 API Key",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "当前激活的 AI 服务未填写入有效的 API Key，请前往设置重新编辑并填写 API Key。",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissNoApiKeyDialog()
                        onNavigateToSettings()
                    },
                    shape = AppShapes.button
                ) {
                    Text("前往设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissNoApiKeyDialog() }) {
                    Text("取消")
                }
            }
        )
    }



    // AI 图文分析结果对话框/卡片
    val response = uiState.aiImageResponse
    if (response != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAIResponseDialog() },
            shape = AppShapes.large,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI 看到了什么？ ✦",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!uiState.activeConfigSummary.isNullOrBlank()) {
                        Surface(
                            shape = AppShapes.chip,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "模型：${uiState.activeConfigSummary}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = response.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(AppSpacing.m)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissAIResponseDialog() },
                    shape = AppShapes.button
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissAIResponseDialog()
                        onReselectClick()
                    }
                ) {
                    Text("换一张图片")
                }
            }
        )
    }
}
