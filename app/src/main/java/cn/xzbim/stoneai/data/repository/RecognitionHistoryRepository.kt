package cn.xzbim.stoneai.data.repository

import cn.xzbim.stoneai.data.local.db.RecognitionHistoryEntity
import cn.xzbim.stoneai.model.CaptureMetadata
import cn.xzbim.stoneai.model.ProcessedImage
import cn.xzbim.stoneai.model.RockRecognitionResult
import kotlinx.coroutines.flow.Flow

/**
 * 识别历史记录 Repository 接口
 */
interface RecognitionHistoryRepository {

    suspend fun saveHistory(
        result: RockRecognitionResult,
        processedImage: ProcessedImage?,
        saveHistorySetting: Boolean = true,
        saveImageSetting: Boolean = true,
        configName: String? = null,
        modelName: String? = null,
        metadata: CaptureMetadata? = null
    ): Result<String>

    suspend fun updateMetadata(historyId: String, metadata: CaptureMetadata)

    fun observeAll(): Flow<List<RecognitionHistoryEntity>>

    fun observeRecent(limit: Int = 5): Flow<List<RecognitionHistoryEntity>>

    suspend fun getById(id: String): RecognitionHistoryEntity?

    suspend fun deleteById(id: String)

    suspend fun deleteAllHistory()
}
