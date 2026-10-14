package cn.xzbim.stoneai.data.repository

import cn.xzbim.stoneai.model.AIConfig
import cn.xzbim.stoneai.model.AIConnectionResult
import cn.xzbim.stoneai.model.AIImageResponse
import cn.xzbim.stoneai.model.AIModel
import cn.xzbim.stoneai.model.ProcessedImage
import cn.xzbim.stoneai.model.RockRecognitionResult

const val DEFAULT_VISION_TEST_PROMPT = """请描述这张图片中最主要的物体和明显视觉特征。
如果你能够看到图片，请根据图片内容直接回答。不要猜测没有看到的内容。"""

/**
 * AI 网络功能 Repository 接口
 */
interface AIRepository {

    suspend fun testConnection(
        config: AIConfig,
        apiKey: String
    ): AIConnectionResult

    suspend fun getModels(
        config: AIConfig,
        apiKey: String
    ): Result<List<AIModel>>

    suspend fun analyzeImageWithActiveConfig(
        image: ProcessedImage,
        prompt: String = DEFAULT_VISION_TEST_PROMPT
    ): Result<AIImageResponse>

    suspend fun recognizeRock(
        image: ProcessedImage
    ): Result<RockRecognitionResult>
}
