package cn.xzbim.stoneai.share

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xzbim.stoneai.model.RecognitionConfidence
import cn.xzbim.stoneai.ui.components.STONE_PHOTO_ASPECT_RATIO
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 专用于分享海报卡片的固定暖白与自然绿视觉 Token (不随 APP 深色模式变化，保持品牌视觉一致)
private val CardBgColor = Color(0xFFFAFBF7)
private val CardSurfaceColor = Color(0xFFFFFFFF)
private val PrimaryGreen = Color(0xFF68B984)
private val PrimaryGreenContainer = Color(0xFFE8F5E9)
private val SecondaryContainer = Color(0xFFFFF8E1)
private val OnSecondaryContainer = Color(0xFF8D6E63)
private val DarkTextColor = Color(0xFF2C3E50)
private val SubTextColor = Color(0xFF7F8C8D)
private val DividerColor = Color(0xFFE5E7EB)

/**
 * 专为社交分享设计的定高定宽海报卡片组件 (1080 x 1440 比例)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RockShareCard(
    shareData: RockShareData,
    preloadedBitmap: Bitmap? = null,
    modifier: Modifier = Modifier
) {
    val metadata = shareData.captureMetadata
    val currentLocale = LocalConfiguration.current.locales[0]
    val timeLabel = metadata?.capturedAt?.let { capturedAt ->
        runCatching {
            SimpleDateFormat("yyyy年M月d日 HH:mm", currentLocale).format(Date(capturedAt))
        }.getOrNull()
    }
    val locationLabel = metadata?.locationName?.ifBlank { null }
        ?: if (metadata?.hasValidGps() == true) {
            String.format(Locale.US, "%.2f°, %.2f°", metadata.latitude, metadata.longitude)
        } else {
            null
        }

    Surface(
        modifier = modifier
            .width(360.dp)
            .wrapContentHeight(),
        color = CardBgColor,
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // 1. 顶部 Header 品牌区
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreenContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Landscape,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI 识石",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "发现身边的小小地球故事",
                        fontSize = 11.sp,
                        color = SubTextColor
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. 主体海报内容白卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DividerColor, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurfaceColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    // 与全应用一致的 1:1 岩石照片视口
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(STONE_PHOTO_ASPECT_RATIO)
                            .align(Alignment.CenterHorizontally),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (preloadedBitmap != null) {
                            Image(
                                bitmap = preloadedBitmap.asImageBitmap(),
                                contentDescription = shareData.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (shareData.imageModel != null) {
                            AsyncImage(
                                model = shareData.imageModel,
                                contentDescription = shareData.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(PrimaryGreenContainer.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Landscape,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (locationLabel != null || timeLabel != null) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            locationLabel?.let {
                                ShareMetadataChip(
                                    icon = Icons.Rounded.Place,
                                    text = it
                                )
                            }
                            timeLabel?.let {
                                ShareMetadataChip(
                                    icon = Icons.Rounded.AccessTime,
                                    text = it
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 名称与分类 Header 行
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = shareData.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            // 可信度 Badge
                            val (badgeBg, badgeText) = when (shareData.confidence) {
                                RecognitionConfidence.HIGH -> PrimaryGreenContainer to PrimaryGreen
                                RecognitionConfidence.MEDIUM -> SecondaryContainer to OnSecondaryContainer
                                else -> Color(0xFFF3F4F6) to Color(0xFF6B7280)
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = badgeBg
                            ) {
                                Text(
                                    text = shareData.confidence.displayName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // 英文名
                        if (!shareData.englishName.isNullOrBlank()) {
                            Text(
                                text = shareData.englishName,
                                fontSize = 12.sp,
                                color = SubTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // 大类 & 子类 Tag
                        val categoryText = buildString {
                            if (!shareData.category.isNullOrBlank()) append(shareData.category)
                            if (!shareData.subCategory.isNullOrBlank()) {
                                if (isNotEmpty()) append(" · ")
                                append(shareData.subCategory)
                            }
                        }

                        if (categoryText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PrimaryGreenContainer.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = categoryText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 特征 Chips (最多 3 个)
                    if (shareData.features.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            shareData.features.take(3).forEach { feature ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF3F4F6)
                                ) {
                                    Text(
                                        text = "✦ $feature",
                                        fontSize = 11.sp,
                                        color = DarkTextColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // 科普一句话简介
                    Text(
                        text = shareData.summary,
                        fontSize = 12.sp,
                        color = DarkTextColor.copy(alpha = 0.9f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. 底部 Footer 免责与品牌落款
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = DividerColor, thickness = 1.dp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI 识石 · 自然探索科普助手",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                    Text(
                        text = "AI 识别结果仅供学习参考",
                        fontSize = 10.sp,
                        color = SubTextColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareMetadataChip(
    icon: ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = PrimaryGreenContainer.copy(alpha = 0.8f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 10.sp,
                color = DarkTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
