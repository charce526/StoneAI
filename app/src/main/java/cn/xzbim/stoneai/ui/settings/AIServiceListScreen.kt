package cn.xzbim.stoneai.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.xzbim.stoneai.model.AIConfig
import cn.xzbim.stoneai.ui.components.EmptyState
import cn.xzbim.stoneai.ui.theme.AppShapes
import cn.xzbim.stoneai.ui.theme.AppSpacing
import cn.xzbim.stoneai.viewmodel.AIServiceListViewModel

/**
 * AI 服务配置列表页面 (Soft & Friendly 自然卡片风)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIServiceListScreen(
    onBackClick: () -> Unit,
    onAddConfigClick: () -> Unit,
    onEditConfigClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AIServiceListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onMessageShown()
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
                        text = "AI 服务",
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
        },
        floatingActionButton = {
            if (uiState.configs.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAddConfigClick,
                    shape = AppShapes.button,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = {
                        Text(
                            text = "添加 AI 服务",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                )
            }
        }
    ) { innerPadding ->
        if (uiState.configs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(AppSpacing.m)
                ) {
                    EmptyState(
                        icon = Icons.Rounded.SmartToy,
                        message = "尚未配置 AI 服务",
                        description = "添加并激活您的 AI 服务 API 配置，让 AI 帮你认识每一块石头 ✦",
                        actionLabel = "添加 AI 服务",
                        onActionClick = onAddConfigClick
                    )
                }
            }
        } else {
            val activeConfig = uiState.activeConfig
            val otherConfigs = uiState.configs.filter { !it.isActive }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(AppSpacing.m),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.m)
            ) {
                item {
                    Text(
                        text = "让 AI 帮你认识每一块石头 ✦",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = AppSpacing.xs)
                    )
                }

                if (activeConfig != null) {
                    item {
                        Text(
                            text = "当前使用",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = AppSpacing.xs)
                        )
                        AIServiceCard(
                            config = activeConfig,
                            onCardClick = { onEditConfigClick(activeConfig.id) },
                            onRadioClick = { /* 已是当前使用 */ }
                        )
                    }
                }

                if (otherConfigs.isNotEmpty()) {
                    item {
                        Text(
                            text = "其他服务",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = AppSpacing.s, bottom = AppSpacing.xs)
                        )
                    }

                    items(
                        items = otherConfigs,
                        key = { it.id }
                    ) { config ->
                        AIServiceCard(
                            config = config,
                            onCardClick = { onEditConfigClick(config.id) },
                            onRadioClick = { viewModel.setActiveConfig(config.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AIServiceCard(
    config: AIConfig,
    onCardClick: () -> Unit,
    onRadioClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        colors = if (config.isActive) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        } else {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        },
        border = if (config.isActive) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else null,
        shape = AppShapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.m),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = config.isActive,
                onClick = onRadioClick
            )

            Spacer(modifier = Modifier.width(AppSpacing.s))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = config.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (config.isActive) {
                        Spacer(modifier = Modifier.width(AppSpacing.s))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = AppShapes.chip
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.height(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "当前使用",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.xs))

                Text(
                    text = "${config.providerType.displayName} · ${config.model}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = config.baseUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "编辑",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
