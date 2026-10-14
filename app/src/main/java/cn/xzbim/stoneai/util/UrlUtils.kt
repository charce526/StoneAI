package cn.xzbim.stoneai.util

/**
 * URL 拼接与处理工具类
 */
object UrlUtils {

    /**
     * 规范化并拼接 Base URL 与 Endpoint 路径
     * 例如：
     * baseUrl = "https://api.openai.com/v1", endpointPath = "/models" -> "https://api.openai.com/v1/models"
     * baseUrl = "https://api.openai.com/v1/", endpointPath = "models" -> "https://api.openai.com/v1/models"
     */
    fun buildEndpointUrl(baseUrl: String, endpointPath: String): String {
        val cleanBase = baseUrl.trim().trimEnd('/')
        val cleanPath = if (endpointPath.startsWith("/")) endpointPath else "/$endpointPath"
        return "$cleanBase$cleanPath"
    }
}
