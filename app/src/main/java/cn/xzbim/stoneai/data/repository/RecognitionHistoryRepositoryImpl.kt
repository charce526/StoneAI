package cn.xzbim.stoneai.data.repository

import android.content.Context
import android.util.Log
import cn.xzbim.stoneai.data.local.db.AppDatabase
import cn.xzbim.stoneai.data.local.db.RecognitionHistoryDao
import cn.xzbim.stoneai.data.local.db.RecognitionHistoryEntity
import cn.xzbim.stoneai.model.CaptureMetadata
import cn.xzbim.stoneai.model.ProcessedImage
import cn.xzbim.stoneai.model.RockRecognitionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 识别历史记录 Repository 实现类
 */
class RecognitionHistoryRepositoryImpl(
    private val context: Context,
    private val dao: RecognitionHistoryDao = AppDatabase.getInstance(context).recognitionHistoryDao()
) : RecognitionHistoryRepository {

    companion object {
        private const val TAG = "HistoryRepo"

        @Volatile
        private var INSTANCE: RecognitionHistoryRepositoryImpl? = null

        fun getInstance(context: Context): RecognitionHistoryRepositoryImpl {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val instance = RecognitionHistoryRepositoryImpl(appContext)
                INSTANCE = instance
                instance
            }
        }
    }

    override suspend fun saveHistory(
        result: RockRecognitionResult,
        processedImage: ProcessedImage?,
        saveHistorySetting: Boolean,
        saveImageSetting: Boolean,
        configName: String?,
        modelName: String?,
        metadata: CaptureMetadata?
    ): Result<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "识别成功 -> 触发保存历史检查 [saveHistorySetting: $saveHistorySetting, saveImageSetting: $saveImageSetting]")

        if (!saveHistorySetting) {
            Log.d(TAG, "用户已关闭保存历史设置，跳过 Room 写入")
            return@withContext Result.success("save_history_disabled")
        }

        val historyId = UUID.randomUUID().toString()
        var savedImagePath: String? = null

        // 尝试保存图片本地文件
        if (saveImageSetting && processedImage != null && processedImage.bytes.isNotEmpty()) {
            try {
                val imageDir = File(context.filesDir, "history_images").apply { if (!exists()) mkdirs() }
                val imageFile = File(imageDir, "history_$historyId.jpg")
                FileOutputStream(imageFile).use { fos ->
                    fos.write(processedImage.bytes)
                }
                if (imageFile.exists() && imageFile.length() > 0) {
                    savedImagePath = imageFile.absolutePath
                    Log.d(TAG, "历史图片保存成功 [Size: ${imageFile.length()} bytes]")
                }
            } catch (e: Exception) {
                Log.w(TAG, "历史图片保存失败，回退保存无图纯文本历史记录: ${e.message}")
                savedImagePath = null
            }
        }

        // 序列化 RockRecognitionResult 为 JSON 字符串
        val jsonString = serializeResultToJson(result)

        // 写入 Room 数据库
        try {
            val entity = RecognitionHistoryEntity(
                id = historyId,
                name = result.name,
                englishName = result.englishName,
                category = result.category,
                subCategory = result.subCategory,
                confidence = result.confidence.name,
                summary = result.summary,
                reasoning = result.reasoning,
                resultJson = jsonString,
                imagePath = savedImagePath,
                aiConfigName = configName,
                modelName = modelName,
                capturedAt = metadata?.capturedAt,
                latitude = metadata?.latitude,
                longitude = metadata?.longitude,
                locationName = metadata?.locationName,
                createdAt = System.currentTimeMillis()
            )

            dao.insert(entity)
            Log.d(TAG, "识别历史保存成功")
            Result.success(historyId)
        } catch (e: Exception) {
            Log.e(TAG, "Room insert 异常: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun observeAll(): Flow<List<RecognitionHistoryEntity>> {
        return dao.observeAll()
    }

    override fun observeRecent(limit: Int): Flow<List<RecognitionHistoryEntity>> {
        return dao.observeRecent(limit)
    }

    override suspend fun getById(id: String): RecognitionHistoryEntity? = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext null
        dao.getById(id)
    }

    override suspend fun deleteById(id: String) {
        withContext(Dispatchers.IO) {
            if (id.isBlank()) return@withContext
            val entity = dao.getById(id)
            if (entity != null) {
                if (!entity.imagePath.isNullOrBlank()) {
                    val file = File(entity.imagePath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
                dao.deleteById(id)
            }
        }
    }

    override suspend fun deleteAllHistory() {
        withContext(Dispatchers.IO) {
            val imageDir = File(context.filesDir, "history_images")
            if (imageDir.exists()) {
                imageDir.listFiles()?.forEach { it.delete() }
            }
            dao.deleteAll()
        }
    }

    override suspend fun updateMetadata(historyId: String, metadata: CaptureMetadata) {
        withContext(Dispatchers.IO) {
            if (historyId.isBlank()) return@withContext
            dao.updateMetadata(
                id = historyId,
                capturedAt = metadata.capturedAt,
                latitude = metadata.latitude,
                longitude = metadata.longitude,
                locationName = metadata.locationName
            )
        }
    }

    private fun serializeResultToJson(result: RockRecognitionResult): String {
        return JSONObject().apply {
            put("name", result.name)
            put("englishName", result.englishName)
            put("category", result.category)
            put("subCategory", result.subCategory)
            put("confidence", result.confidence.name)
            put("summary", result.summary)
            put("reasoning", result.reasoning)
            put("features", JSONArray(result.features))
            put("composition", JSONArray(result.composition))
            put("formation", result.formation)
            put("environment", result.environment)
            put("uses", JSONArray(result.uses))
            put("alternatives", JSONArray().apply {
                result.alternatives.forEach { alt ->
                    put(JSONObject().apply {
                        put("name", alt.name)
                        put("reason", alt.reason)
                    })
                }
            })
            put("suggestedTests", JSONArray(result.suggestedTests))
            put("warning", result.warning)
        }.toString()
    }
}
