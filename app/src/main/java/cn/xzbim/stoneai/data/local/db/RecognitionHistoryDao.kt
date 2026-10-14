package cn.xzbim.stoneai.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 识别历史记录 Room DAO (同步 API，由 Repository 统一在 Dispatchers.IO 协程切换调度)
 */
@Dao
interface RecognitionHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: RecognitionHistoryEntity)

    @Query("UPDATE recognition_history SET capturedAt = :capturedAt, latitude = :latitude, longitude = :longitude, locationName = :locationName WHERE id = :id")
    fun updateMetadata(id: String, capturedAt: Long?, latitude: Double?, longitude: Double?, locationName: String?)

    @Query("SELECT * FROM recognition_history ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RecognitionHistoryEntity>>

    @Query("SELECT * FROM recognition_history ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RecognitionHistoryEntity>>

    @Query("SELECT * FROM recognition_history WHERE id = :id")
    fun getById(id: String): RecognitionHistoryEntity?

    @Query("DELETE FROM recognition_history WHERE id = :id")
    fun deleteById(id: String)

    @Query("DELETE FROM recognition_history")
    fun deleteAll()
}
