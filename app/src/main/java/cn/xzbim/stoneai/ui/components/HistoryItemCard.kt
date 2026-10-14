package cn.xzbim.stoneai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import cn.xzbim.stoneai.data.local.db.RecognitionHistoryEntity
import cn.xzbim.stoneai.model.RecognitionConfidence
import cn.xzbim.stoneai.ui.theme.AppShapes
import cn.xzbim.stoneai.ui.theme.AppSpacing
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

/**
 * 清新风格历史记录卡片组件 (取消右侧 Badge，置信度移至第二行内联展示，标题获得最大横向空间)
 */
@Composable
fun HistoryItemCard(
    entity: RecognitionHistoryEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageFile = if (!entity.imagePath.isNullOrBlank()) File(entity.imagePath) else null
    val hasValidImage = (imageFile != null) && (imageFile.exists() && imageFile.length() > 0)
    val currentLocale = LocalConfiguration.current.locales[0]
    val timeFormatted = try {
        SimpleDateFormat("yyyy-MM-dd HH:mm", currentLocale).format(Date(entity.createdAt))
    } catch (_: Exception) {
        ""
    }

    val confidence = RecognitionConfidence.fromString(entity.confidence)
    val confidenceLabel = when (confidence) {
        RecognitionConfidence.HIGH -> "较明确"
        RecognitionConfidence.MEDIUM -> "有可能"
        else -> "不确定"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.m, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 历史缩略图同样保持 1:1 居中裁切
            if (hasValidImage) {
                AsyncImage(
                    model = imageFile,
                    contentDescription = entity.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(68.dp)
                        .aspectRatio(STONE_PHOTO_ASPECT_RATIO)
                        .clip(AppShapes.medium)
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(68.dp)
                        .aspectRatio(STONE_PHOTO_ASPECT_RATIO)
                        .clip(AppShapes.medium)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Landscape,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(AppSpacing.m))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // 第一行：岩石名称 (独占最大横向空间)
                Text(
                    text = entity.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 第二行：简短置信度 + " · " + summary 摘要
                val summaryText = entity.summary
                val secondLineAnnotated = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = when (confidence) {
                                RecognitionConfidence.HIGH -> MaterialTheme.colorScheme.primary
                                RecognitionConfidence.MEDIUM -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.outline
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(confidenceLabel)
                    }
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.outline)) {
                        append(" · ")
                    }
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                        append(summaryText)
                    }
                }

                Text(
                    text = secondLineAnnotated,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 第三行：时间
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.width(AppSpacing.xs))

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
