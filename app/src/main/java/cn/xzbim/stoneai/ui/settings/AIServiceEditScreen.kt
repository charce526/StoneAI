package cn.xzbim.stoneai.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.xzbim.stoneai.model.AIConnectionResult
import cn.xzbim.stoneai.model.AIModel
import cn.xzbim.stoneai.ui.components.StatusCard
import cn.xzbim.stoneai.ui.theme.AppShapes
import cn.xzbim.stoneai.ui.theme.AppSpacing
import cn.xzbim.stoneai.viewmodel.AIServiceEditViewModel

/**
 * 新增/编辑 AI 服务配置页面 (Soft & Friendly 自然重构风，高信息密度优化版)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIServiceEditScreen(
    configId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AIServiceEditViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(configId) {
        viewModel.loadConfig(configId)
    }

    LaunchedEffect(uiState.isSaved, uiState.isDeleted) {
        if (uiState.isSaved || uiState.isDeleted) {
            onNavigateBack()
        }
    }

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
                        text = if (uiState.isEditMode) "编辑 AI 服务" else "新增 AI 服务",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.m, vertical = AppSpacing.s),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "配置符合 OpenAI 规范的接口参数 ✦",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 配置名称
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("配置名称") },
                placeholder = { Text("例如：我的 AI") },
                singleLine = true,
                shape = AppShapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            // 接口类型
            OutlinedTextField(
                value = uiState.providerType.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("接口类型") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.ArrowDropDown,
                        contentDescription = null
                    )
                },
                shape = AppShapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // API 地址
            OutlinedTextField(
                value = uiState.baseUrl,
                onValueChange = { viewModel.updateBaseUrl(it) },
                label = { Text("API 地址") },
                placeholder = { Text("例如：https://api.openai.com/v1") },
                singleLine = true,
                shape = AppShapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                )
            )

            // API Key
            OutlinedTextField(
                value = uiState.apiKey,
                onValueChange = { viewModel.updateApiKey(it) },
                label = { Text("API Key") },
                placeholder = {
                    if (uiState.hasExistingApiKey) {
                        Text("已安全保存 (留空则保持原 Key)")
                    } else {
                        Text("请输入 API Key (例如：sk-...)")
                    }
                },
                singleLine = true,
                shape = AppShapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    val icon = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility
                    val description = if (isPasswordVisible) "隐藏 API Key" else "显示 API Key"
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(imageVector = icon, contentDescription = description)
                    }
                }
            )

            // 模型名称
            OutlinedTextField(
                value = uiState.model,
                onValueChange = { viewModel.updateModel(it) },
                label = { Text("模型名称") },
                placeholder = { Text("例如：gpt-4o") },
                singleLine = true,
                shape = AppShapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            // 功能按钮组：测试连接 / 获取模型
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)
            ) {
                OutlinedButton(
                    onClick = { viewModel.testConnection() },
                    enabled = !uiState.isTestingConnection && !uiState.isFetchingModels,
                    shape = AppShapes.button,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    if (uiState.isTestingConnection) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 4.dp),
                            strokeWidth = 2.dp
                        )
                        Text("测试中...")
                    } else {
                        Text("测试连接", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.fetchModels() },
                    enabled = !uiState.isTestingConnection && !uiState.isFetchingModels,
                    shape = AppShapes.button,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    if (uiState.isFetchingModels) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 4.dp),
                            strokeWidth = 2.dp
                        )
                        Text("获取中...")
                    } else {
                        Text("获取模型", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }

            // 测试连接结果简要卡片
            val testResult = uiState.testConnectionResult
            if (testResult != null) {
                StatusCard(
                    isSuccess = testResult is AIConnectionResult.Success,
                    title = if (testResult is AIConnectionResult.Success) "✓ AI 服务连接成功" else "✕ 暂时连接不上",
                    message = when (testResult) {
                        is AIConnectionResult.Success -> "API Key 与地址校验通过，当前模型“${testResult.modelName}”可正常响应 ✦"
                        is AIConnectionResult.Failure -> testResult.error.toUserFriendlyMessage()
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 主保存按钮
            Button(
                onClick = { viewModel.saveConfig() },
                enabled = !uiState.isSaving,
                shape = AppShapes.button,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = AppSpacing.s),
                        strokeWidth = 2.dp
                    )
                }
                Text(
                    text = if (uiState.isSaving) "保存中..." else "保存配置",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // 编辑模式下显示删除按钮
            if (uiState.isEditMode) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    shape = AppShapes.button,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "删除此 AI 服务",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }

    // 二次确认删除对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = AppShapes.large,
            title = {
                Text(
                    text = "确定删除“${uiState.name}”吗？",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text("删除后将同时移除保存在本机的 API Key。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteConfig()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 模型选择列表对话框
    if (uiState.showModelPicker) {
        ModelPickerDialog(
            models = uiState.availableModels,
            currentSelectedModel = uiState.model,
            onModelSelected = { viewModel.selectModel(it) },
            onDismiss = { viewModel.dismissModelPicker() }
        )
    }

    // 获取模型失败提示对话框
    val fetchModelsError = uiState.fetchModelsErrorDetail
    if (fetchModelsError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissFetchModelsError() },
            shape = AppShapes.large,
            title = {
                Text(
                    text = "无法获取模型列表",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    Text(
                        text = fetchModelsError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "提示：您仍可以在“模型名称”框中直接填写有效的模型 ID。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissFetchModelsError() }) {
                    Text("已知晓")
                }
            }
        )
    }
}

/**
 * 模型选择列表对话框
 */
@Composable
private fun ModelPickerDialog(
    models: List<AIModel>,
    currentSelectedModel: String,
    onModelSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredModels = remember(models, searchQuery) {
        if (searchQuery.isBlank()) {
            models
        } else {
            models.filter { it.id.contains(searchQuery, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AppShapes.large,
        title = {
            Text(
                text = "选择模型 (共 ${models.size} 个)",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.s)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索模型 ID...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "搜索") },
                    singleLine = true,
                    shape = AppShapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                if (filteredModels.isEmpty()) {
                    Text(
                        text = "未找到匹配的模型",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = AppSpacing.m)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        items(
                            items = filteredModels,
                            key = { it.id }
                        ) { model ->
                            val selected = model.id.equals(currentSelectedModel, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onModelSelected(model.id) }
                                    .padding(vertical = 6.dp, horizontal = AppSpacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = { onModelSelected(model.id) }
                                )
                                Spacer(modifier = Modifier.width(AppSpacing.s))
                                Text(
                                    text = model.id,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "已选择",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
