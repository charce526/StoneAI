package cn.xzbim.stoneai.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cn.xzbim.stoneai.model.ImageQuality
import cn.xzbim.stoneai.model.RecognitionLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "stone_ai_app_prefs")

/**
 * 应用偏好设置管理 (DataStore)
 */
class AppPreferences(private val context: Context) {

    companion object {
        private val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        private val IMAGE_QUALITY = stringPreferencesKey("image_quality")
        private val RECOGNITION_LANGUAGE = stringPreferencesKey("recognition_language")
        private val SAVE_HISTORY = booleanPreferencesKey("save_history")
        private val SAVE_IMAGES = booleanPreferencesKey("save_images")

        @Volatile
        private var INSTANCE: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = AppPreferences(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    val isFirstLaunchFlow: Flow<Boolean> = context.appPreferencesDataStore.data.map { prefs ->
        prefs[IS_FIRST_LAUNCH] ?: true
    }

    val imageQualityFlow: Flow<ImageQuality> = context.appPreferencesDataStore.data.map { prefs ->
        ImageQuality.fromName(prefs[IMAGE_QUALITY])
    }

    val recognitionLanguageFlow: Flow<RecognitionLanguage> = context.appPreferencesDataStore.data.map { prefs ->
        RecognitionLanguage.fromName(prefs[RECOGNITION_LANGUAGE])
    }

    val saveHistoryFlow: Flow<Boolean> = context.appPreferencesDataStore.data.map { prefs ->
        prefs[SAVE_HISTORY] ?: true
    }

    val saveImagesFlow: Flow<Boolean> = context.appPreferencesDataStore.data.map { prefs ->
        prefs[SAVE_IMAGES] ?: true
    }

    suspend fun setFirstLaunchCompleted() {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[IS_FIRST_LAUNCH] = false
        }
    }

    suspend fun setImageQuality(quality: ImageQuality) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[IMAGE_QUALITY] = quality.name
        }
    }

    suspend fun setRecognitionLanguage(language: RecognitionLanguage) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[RECOGNITION_LANGUAGE] = language.name
        }
    }

    suspend fun setSaveHistory(enabled: Boolean) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[SAVE_HISTORY] = enabled
        }
    }

    suspend fun setSaveImages(enabled: Boolean) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[SAVE_IMAGES] = enabled
        }
    }

    suspend fun getImageQuality(): ImageQuality = imageQualityFlow.first()

    suspend fun getRecognitionLanguage(): RecognitionLanguage = recognitionLanguageFlow.first()

    suspend fun shouldSaveHistory(): Boolean = saveHistoryFlow.first()

    suspend fun shouldSaveImages(): Boolean = saveImagesFlow.first()

    /**
     * 清理临时的选图与拍照缓存目录
     */
    fun clearImageCache(): Long {
        val cacheDir = File(context.cacheDir, "recognition_input")
        if (!cacheDir.exists()) return 0L
        var freedBytes = 0L
        cacheDir.listFiles()?.forEach { file ->
            freedBytes += file.length()
            file.delete()
        }
        return freedBytes
    }
}
