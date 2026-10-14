package cn.xzbim.stoneai.data.repository

import android.content.Context
import cn.xzbim.stoneai.data.local.AIConfigDataStore
import cn.xzbim.stoneai.data.local.SecureKeyStorage
import cn.xzbim.stoneai.model.AIConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

/**
 * AIConfigRepository 的具体实现
 */
class AIConfigRepositoryImpl(
    private val dataStore: AIConfigDataStore,
    private val secureKeyStorage: SecureKeyStorage
) : AIConfigRepository {

    override fun getConfigsFlow(): Flow<List<AIConfig>> {
        return dataStore.configsFlow
    }

    override suspend fun getConfigs(): List<AIConfig> {
        return dataStore.configsFlow.first()
    }

    override suspend fun getConfig(id: String): AIConfig? {
        if (id.isBlank()) return null
        return getConfigs().find { it.id == id }
    }

    override suspend fun getActiveConfig(): AIConfig? {
        return getConfigs().find { it.isActive }
    }

    override suspend fun saveConfig(config: AIConfig, apiKey: String?) {
        val currentList = getConfigs().toMutableList()
        val configId = config.id.ifBlank { UUID.randomUUID().toString() }

        val trimmedName = config.name.trim()
        val trimmedBaseUrl = config.baseUrl.trim()
        val trimmedModel = config.model.trim()

        // 如果是列表中第一个配置，自动设为 active
        val shouldBeActive = config.isActive || currentList.isEmpty()

        val updatedConfig = config.copy(
            id = configId,
            name = trimmedName,
            baseUrl = trimmedBaseUrl,
            model = trimmedModel,
            isActive = shouldBeActive
        )

        val existingIndex = currentList.indexOfFirst { it.id == configId }

        if (shouldBeActive) {
            // 将其他配置的 isActive 设为 false
            for (i in 0 until currentList.size) {
                currentList[i] = currentList[i].copy(isActive = false)
            }
        }

        if (existingIndex >= 0) {
            currentList[existingIndex] = updatedConfig
        } else {
            currentList.add(updatedConfig)
        }

        dataStore.saveConfigs(currentList)

        // 安全存储 API Key
        if (!apiKey.isNullOrBlank()) {
            secureKeyStorage.saveApiKey(configId, apiKey.trim())
        }
    }

    override suspend fun deleteConfig(id: String) {
        if (id.isBlank()) return
        val currentList = getConfigs().toMutableList()
        val targetIndex = currentList.indexOfFirst { it.id == id }
        if (targetIndex < 0) return

        val wasActive = currentList[targetIndex].isActive
        currentList.removeAt(targetIndex)
        secureKeyStorage.deleteApiKey(id)

        if (wasActive && currentList.isNotEmpty()) {
            currentList[0] = currentList[0].copy(isActive = true)
        }

        dataStore.saveConfigs(currentList)
    }

    override suspend fun setActiveConfig(id: String) {
        if (id.isBlank()) return
        val currentList = getConfigs()
        val updatedList = currentList.map { config ->
            config.copy(isActive = (config.id == id))
        }
        dataStore.saveConfigs(updatedList)
    }

    override suspend fun getApiKey(id: String): String? {
        return secureKeyStorage.getApiKey(id)
    }

    override fun hasApiKey(id: String): Boolean {
        return secureKeyStorage.hasApiKey(id)
    }

    companion object {
        @Volatile
        private var INSTANCE: AIConfigRepositoryImpl? = null

        fun getInstance(context: Context): AIConfigRepositoryImpl {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val instance = AIConfigRepositoryImpl(
                    dataStore = AIConfigDataStore(appContext),
                    secureKeyStorage = SecureKeyStorage(appContext)
                )
                INSTANCE = instance
                instance
            }
        }
    }
}
