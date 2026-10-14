package cn.xzbim.stoneai.model

/**
 * 统一的 AI 异常/错误声明
 */
sealed class AIError {

    data object InvalidApiKey : AIError()

    data object InvalidBaseUrl : AIError()

    data object ModelNotFound : AIError()

    data object RateLimited : AIError()

    data object QuotaExceeded : AIError()

    data object VisionNotSupported : AIError()

    data object ImageTooLarge : AIError()

    data object Timeout : AIError()

    data object NetworkUnavailable : AIError()

    data class HttpError(
        val statusCode: Int,
        val message: String?
    ) : AIError()

    data class InvalidResponse(
        val message: String?
    ) : AIError()

    data class Unknown(
        val message: String?
    ) : AIError()

    /**
     * 转为用户友好的提示文案
     */
    fun toUserFriendlyMessage(): String {
        return when (this) {
            InvalidApiKey -> "API Key 无效，请检查对应的 API Key 是否正确"
            InvalidBaseUrl -> "无法连接 AI 服务，请检查 API 地址是否正确"
            ModelNotFound -> "指定模型不可用，请检查模型名称或重新获取模型列表"
            RateLimited -> "请求过于频繁，请稍后再试"
            QuotaExceeded -> "AI 服务账户额度不足或已欠费，请检查账户余额"
            VisionNotSupported -> "当前配置的模型可能不支持图片识别，请更换支持 Vision/多模态的模型"
            ImageTooLarge -> "图片数据体积过大，请尝试压缩或重新拍摄后重试"
            Timeout -> "连接超时，请检查网络状况或 AI 服务响应状态"
            NetworkUnavailable -> "当前无法连接到网络，请检查设备的网络设置"
            is HttpError -> "HTTP 错误 ($statusCode)${if (!message.isNullOrBlank()) ": $message" else ""}"
            is InvalidResponse -> "响应解析异常${if (!message.isNullOrBlank()) ": $message" else ""}"
            is Unknown -> "未知错误${if (!message.isNullOrBlank()) ": $message" else ""}"
        }
    }
}
