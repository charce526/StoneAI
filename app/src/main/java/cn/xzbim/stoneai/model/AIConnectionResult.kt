package cn.xzbim.stoneai.model

/**
 * AI 服务连接测试结果
 */
sealed interface AIConnectionResult {

    data class Success(
        val responseText: String? = null,
        val modelName: String? = null
    ) : AIConnectionResult

    data class Failure(
        val error: AIError,
        val rawMessage: String? = null
    ) : AIConnectionResult
}
