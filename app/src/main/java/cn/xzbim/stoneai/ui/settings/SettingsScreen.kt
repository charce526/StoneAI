package cn.xzbim.stoneai.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.xzbim.stoneai.model.ImageQuality
import cn.xzbim.stoneai.model.RecognitionLanguage
import cn.xzbim.stoneai.ui.components.SettingGroup
import cn.xzbim.stoneai.ui.components.SettingItem
import cn.xzbim.stoneai.ui.theme.AppShapes
import cn.xzbim.stoneai.ui.theme.AppSpacing
import cn.xzbim.stoneai.viewmodel.HistoryViewModel
import cn.xzbim.stoneai.viewmodel.SettingsViewModel

/**
 * V1 完整设置主页 (Soft & Friendly 自然卡片风)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToAIService: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
    historyViewModel: HistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showQualityDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }

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
                        text = "设置",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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
            verticalArrangement = Arrangement.spacedBy(AppSpacing.l)
        ) {
            // AI 与识别设置组
            SettingGroup(title = "AI 与识别") {
                SettingItem(
                    title = "AI 服务配置",
                    subtitle = uiState.activeConfigSummary,
                    icon = Icons.Rounded.AutoAwesome,
                    onClick = onNavigateToAIService
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingItem(
                    title = "压缩图片质量",
                    subtitle = uiState.imageQuality.displayName,
                    icon = Icons.Rounded.Tune,
                    onClick = { showQualityDialog = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingItem(
                    title = "识别语言偏好",
                    subtitle = uiState.recognitionLanguage.displayName,
                    icon = Icons.Rounded.Language,
                    onClick = { showLanguageDialog = true }
                )
            }

            // 数据管理设置组
            SettingGroup(title = "数据管理") {
                SettingItem(
                    title = "自动保存识别历史",
                    subtitle = if (uiState.saveHistory) "识别成功后自动写入本地 Room 数据库" else "关闭后不自动记录识别结果",
                    icon = Icons.Rounded.Save,
                    onClick = { viewModel.setSaveHistory(!uiState.saveHistory) },
                    trailingContent = {
                        Switch(
                            checked = uiState.saveHistory,
                            onCheckedChange = { viewModel.setSaveHistory(it) }
                        )
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingItem(
                    title = "保存本地识别图片",
                    subtitle = if (uiState.saveImages) "识别图片保存至 App 本地私有目录" else "关闭后历史记录不保留本地图片文件",
                    icon = Icons.Rounded.Image,
                    onClick = { viewModel.setSaveImages(!uiState.saveImages) },
                    trailingContent = {
                        Switch(
                            checked = uiState.saveImages,
                            onCheckedChange = { viewModel.setSaveImages(it) }
                        )
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingItem(
                    title = "清理临时图片缓存",
                    subtitle = "清理选图与相机拍摄的临时文件",
                    icon = Icons.Rounded.CleaningServices,
                    onClick = { viewModel.clearImageCache() }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingItem(
                    title = "清空所有识别记录",
                    subtitle = "清空本地数据库历史及全部保存图片",
                    icon = Icons.Rounded.DeleteForever,
                    onClick = { showClearHistoryDialog = true }
                )
            }

            // 关于与隐私设置组
            SettingGroup(title = "关于与隐私") {
                SettingItem(
                    title = "关于 AI 识石",
                    subtitle = "版本 v${uiState.appVersion}",
                    icon = Icons.Rounded.Info,
                    onClick = onNavigateToAbout
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingItem(
                    title = "隐私与数据说明",
                    subtitle = "查看图片与 API 密钥数据隐私策略",
                    icon = Icons.Rounded.Policy,
                    onClick = { showPrivacyDialog = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingItem(
                    title = "开源组件许可",
                    subtitle = "查看引用的开源代码库与许可",
                    icon = Icons.Rounded.Code,
                    onClick = { showLicensesDialog = true }
                )
            }
        }
    }

    // 清空识别历史确认对话框
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            shape = AppShapes.large,
            title = {
                Text(
                    text = "确定清空所有识别记录吗？",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text("所有历史结果和对应的本地保存图片都会被彻底删除，无法恢复。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryDialog = false
                        historyViewModel.clearAllHistory()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("清空", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 图片质量选择对话框
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            shape = AppShapes.large,
            title = {
                Text(
                    text = "图片处理质量",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    ImageQuality.entries.forEach { quality ->
                        val selected = quality == uiState.imageQuality
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setImageQuality(quality)
                                    showQualityDialog = false
                                }
                                .padding(vertical = AppSpacing.s),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = {
                                    viewModel.setImageQuality(quality)
                                    showQualityDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.s))
                            Text(
                                text = quality.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 识别语言选择对话框
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            shape = AppShapes.large,
            title = {
                Text(
                    text = "识别语言偏好",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    RecognitionLanguage.entries.forEach { lang ->
                        val selected = lang == uiState.recognitionLanguage
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setRecognitionLanguage(lang)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = AppSpacing.s),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = {
                                    viewModel.setRecognitionLanguage(lang)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.s))
                            Text(
                                text = lang.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 隐私与数据说明对话框
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            shape = AppShapes.large,
            title = {
                Text(
                    text = "隐私与数据声明 ✦",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    Text("1. API 秘钥安全：API Key 仅加密保存在本机 Android Keystore 中，绝不上传至任何中间服务器。")
                    Text("2. 图片数据传输：仅在您点击识别时，图片加密发送给您自行配置的第三方 AI 服务商。")
                    Text("3. 本地存储：识别历史与图片默认保存在设备本地，应用本身不设账号与云端同步。")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("已知晓")
                }
            }
        )
    }

    // 开源许可说明对话框
    if (showLicensesDialog) {
        val licenses = remember {
            listOf(
                "Android Jetpack Compose" to "Apache License 2.0",
                "Android Room Database" to "Apache License 2.0",
                "AndroidX CameraX" to "Apache License 2.0",
                "Coil Image Loading" to "Apache License 2.0",
                "Square OkHttp" to "Apache License 2.0",
                "Kotlin Coroutines & DataStore" to "Apache License 2.0"
            )
        }

        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            shape = AppShapes.large,
            title = {
                Text(
                    text = "开源许可声明",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.s)
                ) {
                    items(items = licenses) { (lib, license) ->
                        Column {
                            Text(
                                text = lib,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = license,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
}
