package cn.xzbim.stoneai.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cn.xzbim.stoneai.model.AIConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_configs_prefs")

/**
 * 基于 DataStore 的 AI 配置列表本地持久化
 */
class AIConfigDataStore(private val context: Context) {

    companion object {
        private val AI_CONFIGS_KEY = stringPreferencesKey("ai_configs_json_list")
    }

    val configsFlow: Flow<List<AIConfig>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[AI_CONFIGS_KEY] ?: "[]"
        parseConfigsFromJson(jsonString)
    }

    suspend fun saveConfigs(configs: List<AIConfig>) {
        val jsonArray = JSONArray()
        configs.forEach { config ->
            jsonArray.put(config.toJsonObject())
        }
        context.dataStore.edit { preferences ->
            preferences[AI_CONFIGS_KEY] = jsonArray.toString()
        }
    }

    private fun parseConfigsFromJson(jsonString: String): List<AIConfig> {
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<AIConfig>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                list.add(AIConfig.fromJsonObject(jsonObject))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
