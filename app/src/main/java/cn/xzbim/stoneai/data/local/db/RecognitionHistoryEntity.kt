package cn.xzbim.stoneai.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 岩石识别历史记录 Room 实体类 (包含版本 2 的拍摄时间与拍摄地点扩展字段)
 */
@Entity(tableName = "recognition_history")
data class RecognitionHistoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val englishName: String? = null,
    val category: String? = null,
    val subCategory: String? = null,
    val confidence: String,
    val summary: String,
    val reasoning: String,
    val resultJson: String,
    val imagePath: String? = null,
    val aiConfigName: String? = null,
    val modelName: String? = null,
    val capturedAt: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
