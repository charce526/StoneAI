package cn.xzbim.stoneai.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import cn.xzbim.stoneai.model.CaptureMetadata
import cn.xzbim.stoneai.ui.theme.AppShapes
import cn.xzbim.stoneai.ui.theme.AppSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 拍摄附加信息展示卡片 (拍摄时间与拍摄地点)
 */
@Composable
fun CaptureInfoCard(
    metadata: CaptureMetadata?,
    modifier: Modifier = Modifier
) {
    if (metadata == null || !metadata.hasData()) return

    val currentLocale = LocalConfiguration.current.locales[0]
    val timeFormatted = metadata.capturedAt?.let {
        try {
            SimpleDateFormat("yyyy年M月d日 HH:mm", currentLocale).format(Date(it))
        } catch (_: Exception) {
            null
        }
    }

    val displayLocation = metadata.locationName?.ifBlank { null }
        ?: if (metadata.hasValidGps()) {
            String.format(Locale.US, "%.2f°, %.2f°", metadata.latitude, metadata.longitude)
        } else null

    if (timeFormatted == null && displayLocation == null) return

    Card(
        modifier = modifier.fillMaxWidth(),
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
                text = "拍摄信息 ✦",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = AppSpacing.s)
            )

            if (!displayLocation.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Place,
                        contentDescription = "拍摄地点",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = AppSpacing.s)
                    )
                    Text(
                        text = displayLocation,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (!displayLocation.isNullOrBlank() && !timeFormatted.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(AppSpacing.s))
            }

            if (!timeFormatted.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AccessTime,
                        contentDescription = "拍摄时间",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(end = AppSpacing.s)
                    )
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
