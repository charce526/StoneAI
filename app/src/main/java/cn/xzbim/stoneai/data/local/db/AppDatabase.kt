package cn.xzbim.stoneai.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 应用主 Room 数据库 (包含 Migration 1 -> 2 升级，绝对不破坏旧用户数据)
 */
@Database(
    entities = [RecognitionHistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recognitionHistoryDao(): RecognitionHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recognition_history ADD COLUMN capturedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE recognition_history ADD COLUMN latitude REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE recognition_history ADD COLUMN longitude REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE recognition_history ADD COLUMN locationName TEXT DEFAULT NULL")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stone_ai_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
