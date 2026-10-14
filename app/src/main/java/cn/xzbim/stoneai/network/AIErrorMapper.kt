package cn.xzbim.stoneai.network

import cn.xzbim.stoneai.model.AIError
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 映射 HTTP 状态码、JSON 错误响应与网络异常到统一的 AIError
 */
object AIErrorMapper {

    fun mapHttpResponse(statusCode: Int, responseBodyString: String?): AIError {
        val errorMessage = parseErrorMessageFromJson(responseBodyString)
        val lowerMsg = (errorMessage ?: responseBodyString ?: "").lowercase()

        // 优先拦截多模态/图片识别不支持提示
        if (isVisionNotSupportedError(statusCode, lowerMsg)) {
            return AIError.VisionNotSupported
        }

        // 拦截图片数据包过大提示
        if (statusCode == 413 || isImageTooLargeError(lowerMsg)) {
            return AIError.ImageTooLarge
        }

        return when (statusCode) {
            401 -> AIError.InvalidApiKey
            403 -> {
                if (lowerMsg.contains("quota") || lowerMsg.contains("balance") || lowerMsg.contains("credit")) {
                    AIError.QuotaExceeded
                } else {
                    AIError.HttpError(403, errorMessage ?: "权限不足或 API Key 无访问权限")
                }
            }
            404 -> {
                if (lowerMsg.contains("model")) {
                    AIError.ModelNotFound
                } else {
                    AIError.InvalidBaseUrl
                }
            }
            429 -> {
                if (lowerMsg.contains("quota") || lowerMsg.contains("billing") || lowerMsg.contains("balance") || lowerMsg.contains("credit")) {
                    AIError.QuotaExceeded
                } else {
                    AIError.RateLimited
                }
            }
            408, 504 -> AIError.Timeout
            in 500..599 -> AIError.HttpError(statusCode, errorMessage ?: "AI 服务端暂不可用 ($statusCode)")
            else -> AIError.HttpError(statusCode, errorMessage)
        }
    }

    fun mapThrowable(throwable: Throwable): AIError {
        return when (throwable) {
            is UnknownHostException, is ConnectException -> AIError.InvalidBaseUrl
            is SocketTimeoutException -> AIError.Timeout
            is IOException -> AIError.NetworkUnavailable
            else -> AIError.Unknown(throwable.localizedMessage)
        }
    }

    private fun isVisionNotSupportedError(statusCode: Int, lowerMsg: String): Boolean {
        if (statusCode == 400 || statusCode == 422) {
            if (lowerMsg.contains("vision") ||
                lowerMsg.contains("image input") ||
                lowerMsg.contains("image_url") ||
                lowerMsg.contains("multimodal") ||
                lowerMsg.contains("unsupported content type") ||
                lowerMsg.contains("image_url is not supported") ||
                lowerMsg.contains("does not support image")
            ) {
                return true
            }
        }
        return false
    }

    private fun isImageTooLargeError(lowerMsg: String): Boolean {
        return lowerMsg.contains("payload too large") ||
                lowerMsg.contains("image too large") ||
                lowerMsg.contains("request entity too large")
    }

    private fun parseErrorMessageFromJson(jsonString: String?): String? {
        if (jsonString.isNullOrBlank()) return null
        return try {
            val json = JSONObject(jsonString)
            if (json.has("error")) {
                val errorObj = json.optJSONObject("error")
                if (errorObj != null) {
                    val msg = errorObj.optString("message", "")
                    if (msg.isNotBlank()) return msg
                    val code = errorObj.optString("code", "")
                    if (code.isNotBlank()) return code
                }
                val errStr = json.optString("error", "")
                if (errStr.isNotBlank()) return errStr
            }
            val msg = json.optString("message", "")
            if (msg.isNotBlank()) msg else null
        } catch (_: Exception) {
            null
        }
    }
}
