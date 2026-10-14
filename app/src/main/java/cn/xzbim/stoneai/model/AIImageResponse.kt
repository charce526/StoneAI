package cn.xzbim.stoneai.model

/**
 * AI 多模态图文分析响应
 */
data class AIImageResponse(
    val content: String,
    val rawResponse: String? = null
)
