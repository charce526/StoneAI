package cn.xzbim.stoneai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import cn.xzbim.stoneai.model.CaptureMetadata
import cn.xzbim.stoneai.model.RecognitionConfidence
import cn.xzbim.stoneai.model.RockRecognitionResult
import cn.xzbim.stoneai.ui.theme.AppShapes
import cn.xzbim.stoneai.ui.theme.AppSpacing
import java.io.File

/**
 * 结构化岩石识别结果通用排版组件 (共享于实时识别结果与历史记录详情，保持 1:1 统一构图)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RockResultContent(
    result: RockRecognitionResult,
    imageModel: Any?,
    configSummary: String? = null,
    captureMetadata: CaptureMetadata? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.m)
    ) {
        // 顶部 Hero 识别主要信息卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.m)
            ) {
                // 与相机取景及照片预览一致：1:1 视口、居中裁切
                val fileExists = if (imageModel is File) imageModel.exists() && imageModel.length() > 0 else imageModel != null
                if (fileExists) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(STONE_PHOTO_ASPECT_RATIO)
                            .align(Alignment.CenterHorizontally),
                        shape = AppShapes.medium
                    ) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = "岩石照片",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.m))
                }

                // 模型与可信度 Tag 行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (!configSummary.isNullOrBlank()) {
                        Surface(
                            shape = AppShapes.chip,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "模型：$configSummary",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    ConfidenceBadge(confidence = result.confidence)
                }

                Spacer(modifier = Modifier.height(AppSpacing.s))

                Text(
                    text = result.name,
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!result.englishName.isNullOrBlank()) {
                    Text(
                        text = result.englishName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!result.category.isNullOrBlank() || !result.subCategory.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(AppSpacing.s))
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                        result.category?.let { cat ->
                            Surface(
                                shape = AppShapes.chip,
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        result.subCategory?.let { sub ->
                            Surface(
                                shape = AppShapes.chip,
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = sub,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. 科普简介与分析依据
        ResultSectionCard(title = "科普简介 ✦") {
            Text(
                text = result.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(AppSpacing.s))
            Text(
                text = "分析依据：${result.reasoning}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 2. 主要视觉特征
        if ((result.features ?: emptyList()).isNotEmpty()) {
            ResultSectionCard(title = "主要特征") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    result.features.forEach { feature ->
                        Surface(
                            shape = AppShapes.chip,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. 可能组成成分
        if ((result.composition ?: emptyList()).isNotEmpty()) {
            ResultSectionCard(title = "成分组成") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    result.composition.forEach { comp ->
                        Surface(
                            shape = AppShapes.chip,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = comp,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. 形成方式与常见环境
        if (!result.formation.isNullOrBlank() || !result.environment.isNullOrBlank()) {
            ResultSectionCard(title = "地质与环境") {
                result.formation?.let {
                    Text(
                        text = "形成方式：$it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (!result.formation.isNullOrBlank() && !result.environment.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                }
                result.environment?.let {
                    Text(
                        text = "地质环境：$it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 5. 常见用途
        if ((result.uses ?: emptyList()).isNotEmpty()) {
            ResultSectionCard(title = "常见用途") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    result.uses.forEach { use ->
                        Surface(
                            shape = AppShapes.chip,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = use,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 6. 其他混淆候选项
        if ((result.alternatives ?: emptyList()).isNotEmpty()) {
            ResultSectionCard(title = "其他可能选项") {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    result.alternatives.forEach { alt ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "• ${alt.name}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!alt.reason.isNullOrBlank()) {
                                Text(
                                    text = "：${alt.reason}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. 进一步安全鉴别建议
        if ((result.suggestedTests ?: emptyList()).isNotEmpty()) {
            ResultSectionCard(title = "进一步确认建议 ✦") {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    result.suggestedTests.forEachIndexed { index, test ->
                        Text(
                            text = "${index + 1}. $test",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 8. 拍摄信息卡片 (时间与地点)
        if (captureMetadata != null && captureMetadata.hasData()) {
            CaptureInfoCard(metadata = captureMetadata)
        }

        // 9. 鉴别提示卡片
        if (!result.warning.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.m),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.s))
                    Text(
                        text = result.warning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.m)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = AppSpacing.s)
            )
            content()
        }
    }
}

@Composable
private fun ConfidenceBadge(confidence: RecognitionConfidence?) {
    val safeConfidence = confidence ?: RecognitionConfidence.UNKNOWN
    val (bgColor, textColor) = when (safeConfidence) {
        RecognitionConfidence.HIGH -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        RecognitionConfidence.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        RecognitionConfidence.LOW -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        RecognitionConfidence.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = AppShapes.chip,
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = safeConfidence.displayName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
        }
    }
}
