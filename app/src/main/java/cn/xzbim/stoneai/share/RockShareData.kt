package cn.xzbim.stoneai.share

import cn.xzbim.stoneai.data.local.db.RecognitionHistoryEntity
import cn.xzbim.stoneai.model.RecognitionConfidence
import cn.xzbim.stoneai.model.CaptureMetadata
import cn.xzbim.stoneai.model.RockRecognitionResult
import cn.xzbim.stoneai.util.RockResultParser
import java.io.File

/**
 * 识别结果分享卡片专用精简数据模型 (离线解耦，兼容实时识图与历史记录)
 */
data class RockShareData(
    val imageModel: Any?, // Uri, File, Bitmap 或 null (缺失时自动降级占位)
    val name: String,
    val englishName: String?,
    val category: String?,
    val subCategory: String?,
    val confidence: RecognitionConfidence,
    val features: List<String>,
    val summary: String,
    val captureMetadata: CaptureMetadata? = null
) {
    companion object {
        /**
         * 实时识别结果 -> RockShareData 转换器
         */
        fun fromResult(
            result: RockRecognitionResult,
            imageModel: Any?,
            captureMetadata: CaptureMetadata? = null
        ): RockShareData {
            return RockShareData(
                imageModel = imageModel,
                name = result.name,
                englishName = result.englishName?.ifBlank { null },
                category = result.category?.ifBlank { null },
                subCategory = result.subCategory?.ifBlank { null },
                confidence = result.confidence,
                features = (result.features ?: emptyList()).take(3),
                summary = result.summary,
                captureMetadata = captureMetadata
            )
        }

        /**
         * 历史记录实体 -> RockShareData 转换器
         */
        fun fromHistoryEntity(
            entity: RecognitionHistoryEntity
        ): RockShareData {
            val imageFile = entity.imagePath?.let { File(it) }
            val validImage = if (imageFile != null && imageFile.exists() && imageFile.length() > 0) imageFile else null

            val result = try {
                RockResultParser.parse(entity.resultJson)
            } catch (_: Exception) {
                null
            }

            return RockShareData(
                imageModel = validImage,
                name = entity.name,
                englishName = entity.englishName?.ifBlank { null },
                category = entity.category?.ifBlank { null },
                subCategory = entity.subCategory?.ifBlank { null },
                confidence = RecognitionConfidence.fromString(entity.confidence),
                features = (result?.features ?: emptyList()).take(3),
                summary = entity.summary,
                captureMetadata = CaptureMetadata(
                    capturedAt = entity.capturedAt,
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    locationName = entity.locationName
                ).takeIf { it.hasData() }
            )
        }
    }
}
