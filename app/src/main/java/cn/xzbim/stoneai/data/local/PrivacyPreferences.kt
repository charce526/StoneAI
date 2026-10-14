package cn.xzbim.stoneai.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.privacyDataStore: DataStore<Preferences> by preferencesDataStore(name = "stone_ai_privacy_prefs")

/**
 * 用户图片识别隐私同意记录
 */
class PrivacyPreferences(private val context: Context) {

    companion object {
        private val HAS_AGREED_IMAGE_PRIVACY = booleanPreferencesKey("has_agreed_image_privacy")
    }

    val hasAgreedImagePrivacyFlow: Flow<Boolean> = context.privacyDataStore.data.map { prefs ->
        prefs[HAS_AGREED_IMAGE_PRIVACY] ?: false
    }

    suspend fun isAgreed(): Boolean {
        return hasAgreedImagePrivacyFlow.first()
    }

    suspend fun setAgreed(agreed: Boolean) {
        context.privacyDataStore.edit { prefs ->
            prefs[HAS_AGREED_IMAGE_PRIVACY] = agreed
        }
    }
}
