package cn.xzbim.stoneai.model

import kotlin.math.abs

/**
 * 拍摄附加元数据 (拍摄时间与拍摄地点)
 */
data class CaptureMetadata(
    val capturedAt: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null
) {
    fun hasValidGps(): Boolean {
        if (latitude == null || longitude == null) return false
        if (abs(latitude) < 0.0001 && abs(longitude) < 0.0001) return false // 过滤 0.00, 0.00 (Null Island) 伪坐标
        if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0) return false
        return true
    }

    fun hasData(): Boolean = capturedAt != null || !locationName.isNullOrBlank() || hasValidGps()
}
