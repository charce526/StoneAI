package cn.xzbim.stoneai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 统一圆角层级规范 (Soft & Friendly)
 */
object AppShapes {
    val small = RoundedCornerShape(10.dp)
    val medium = RoundedCornerShape(16.dp)
    val large = RoundedCornerShape(20.dp)
    val chip = RoundedCornerShape(12.dp)
    val button = RoundedCornerShape(16.dp)
}

/**
 * 统一间距规范 (高信息密度优化版)
 */
object AppSpacing {
    val xs: Dp = 4.dp     // 超小间距 (4dp)
    val s: Dp = 8.dp      // 小间距 (8dp)
    val m: Dp = 12.dp     // 普通紧凑间距 (12dp)
    val l: Dp = 16.dp     // 区块间距 (16dp)
    val xl: Dp = 20.dp    // 较大区块间距 (20dp)
}
