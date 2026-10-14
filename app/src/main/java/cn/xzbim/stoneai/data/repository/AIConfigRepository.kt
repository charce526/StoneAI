package cn.xzbim.stoneai.data.repository

import cn.xzbim.stoneai.model.AIConfig
import kotlinx.coroutines.flow.Flow

/**
 * AI 服务配置 Repository 接口
 */
interface AIConfigRepository {
    fun getConfigsFlow(): Flow<List<AIConfig>>
    suspend fun getConfigs(): List<AIConfig>
    suspend fun getConfig(id: String): AIConfig?
    suspend fun getActiveConfig(): AIConfig?
    suspend fun saveConfig(config: AIConfig, apiKey: String?)
    suspend fun deleteConfig(id: String)
    suspend fun setActiveConfig(id: String)
    suspend fun getApiKey(id: String): String?
    fun hasApiKey(id: String): Boolean
}
