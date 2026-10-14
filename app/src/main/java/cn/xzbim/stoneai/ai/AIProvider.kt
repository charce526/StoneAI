package cn.xzbim.stoneai.ai

import cn.xzbim.stoneai.model.AIConfig
import cn.xzbim.stoneai.model.AIConnectionResult
import cn.xzbim.stoneai.model.AIImageResponse
import cn.xzbim.stoneai.model.AIModel
import cn.xzbim.stoneai.model.ProcessedImage

/**
 * 统一 AI Provider 抽象接口
 */
interface AIProvider {

    suspend fun testConnection(
        config: AIConfig,
        apiKey: String
    ): AIConnectionResult

    suspend fun getModels(
        config: AIConfig,
        apiKey: String
    ): Result<List<AIModel>>

    suspend fun analyzeImage(
        config: AIConfig,
        apiKey: String,
        image: ProcessedImage,
        prompt: String
    ): Result<AIImageResponse>
}
