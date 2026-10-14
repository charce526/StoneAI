package cn.xzbim.stoneai.ai

import android.util.Log
import cn.xzbim.stoneai.model.AIConfig
import cn.xzbim.stoneai.model.AIConnectionResult
import cn.xzbim.stoneai.model.AIError
import cn.xzbim.stoneai.model.AIImageResponse
import cn.xzbim.stoneai.model.AIModel
import cn.xzbim.stoneai.model.ProcessedImage
import cn.xzbim.stoneai.network.AIErrorMapper
import cn.xzbim.stoneai.network.HttpClientFactory
import cn.xzbim.stoneai.util.UrlUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * OpenAI Compatible 接口规范的 AI Provider 实现 (含容错响应解析与网络诊断)
 */
class OpenAICompatibleProvider(
    private val client: OkHttpClient = HttpClientFactory.client
) : AIProvider {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val visionClient: OkHttpClient by lazy {
        client.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun testConnection(
        config: AIConfig,
        apiKey: String
    ): AIConnectionResult = withContext(Dispatchers.IO) {
        val url = UrlUtils.buildEndpointUrl(config.baseUrl, "/chat/completions")

        val requestJson = JSONObject().apply {
            put("model", config.model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "Reply only with OK.")
                })
            })
            put("temperature", 0)
            put("max_tokens", 10)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                val contentType = response.header("Content-Type", "unknown")

                if (response.isSuccessful) {
                    // 1. 检查是否为图片生成/绘图模型提示
                    if (isImageGenerationModelNotice(bodyString)) {
                        return@withContext AIConnectionResult.Failure(
                            error = AIError.InvalidResponse("当前模型属于图片生成/绘图模型，不适合用于图片理解或岩石识别"),
                            rawMessage = formatDiagnosticInfo(url, response, contentType, bodyString)
                        )
                    }

                    // 2. 尝试容错解析返回的文本
                    val parsedContent = parseChatCompletionResponse(bodyString)
                    if (!parsedContent.isNullOrBlank()) {
                        AIConnectionResult.Success(
                            responseText = parsedContent,
                            modelName = config.model
                        )
                    } else {
                        // HTTP 200 成功响应但非标准 Chat Completions 结构
                        Log.w("OpenAIProvider", "HTTP 200 响应但无法提取文本（响应内容已隐藏）")
                        AIConnectionResult.Failure(
                            error = AIError.InvalidResponse("AI 服务已响应(HTTP 200)，但返回数据非标准 Chat Completions 结构"),
                            rawMessage = formatDiagnosticInfo(url, response, contentType, bodyString)
                        )
                    }
                } else {
                    val aiError = AIErrorMapper.mapHttpResponse(response.code, bodyString)
                    AIConnectionResult.Failure(
                        error = aiError,
                        rawMessage = formatDiagnosticInfo(url, response, contentType, bodyString)
                    )
                }
            }
        } catch (e: Exception) {
            val aiError = AIErrorMapper.mapThrowable(e)
            AIConnectionResult.Failure(
                error = aiError,
                rawMessage = "网络调用异常: ${e.localizedMessage}"
            )
        }
    }

    override suspend fun getModels(
        config: AIConfig,
        apiKey: String
    ): Result<List<AIModel>> = withContext(Dispatchers.IO) {
        val url = UrlUtils.buildEndpointUrl(config.baseUrl, "/models")

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "application/json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (response.isSuccessful && !bodyString.isNullOrBlank()) {
                    val models = parseModelsResponse(bodyString)
                    Result.success(models)
                } else {
                    val aiError = AIErrorMapper.mapHttpResponse(response.code, bodyString)
                    Result.failure(Exception(aiError.toUserFriendlyMessage()))
                }
            }
        } catch (e: Exception) {
            val aiError = AIErrorMapper.mapThrowable(e)
            Result.failure(Exception(aiError.toUserFriendlyMessage()))
        }
    }

    override suspend fun analyzeImage(
        config: AIConfig,
        apiKey: String,
        image: ProcessedImage,
        prompt: String
    ): Result<AIImageResponse> = withContext(Dispatchers.IO) {
        val url = UrlUtils.buildEndpointUrl(config.baseUrl, "/chat/completions")

        val requestJson = JSONObject().apply {
            put("model", config.model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        // 文本 Prompt
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", prompt)
                        })
                        // 图片数据 (Data URI 格式)
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", "data:${image.mimeType};base64,${image.base64}")
                            })
                        })
                    })
                })
            })
            put("temperature", 0.2)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            visionClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (response.isSuccessful && !bodyString.isNullOrBlank()) {
                    val content = parseChatCompletionResponse(bodyString)
                    if (!content.isNullOrBlank()) {
                        Result.success(AIImageResponse(content = content, rawResponse = bodyString))
                    } else {
                        val aiError = AIError.InvalidResponse("AI 服务已响应，但未包含有效的分析文本")
                        Result.failure(Exception(aiError.toUserFriendlyMessage()))
                    }
                } else {
                    val aiError = AIErrorMapper.mapHttpResponse(response.code, bodyString)
                    Result.failure(Exception(aiError.toUserFriendlyMessage()))
                }
            }
        } catch (e: Exception) {
            val aiError = AIErrorMapper.mapThrowable(e)
            Result.failure(Exception(aiError.toUserFriendlyMessage()))
        }
    }

    /**
     * 容错解析 Chat Completions 响应，兼容：
     * 1. choices[0].message.content (String)
     * 2. choices[0].message.content (JSONArray，如 [{"type":"text","text":"..."}])
     * 3. choices[0].text (Legacy 格式)
     * 4. choices[0].delta.content (Stream 格式)
     */
    private fun parseChatCompletionResponse(jsonString: String?): String? {
        if (jsonString.isNullOrBlank()) return null
        return try {
            val json = JSONObject(jsonString)
            val choices = json.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            val firstChoice = choices.getJSONObject(0)

            // 1. 尝试从 choices[0].message 提取
            val messageObj = firstChoice.optJSONObject("message")
            if (messageObj != null) {
                // 1.1 content 为普通字符串
                val contentStr = messageObj.optString("content", "").trim()
                if (contentStr.isNotBlank() && !contentStr.startsWith("[")) {
                    return contentStr
                }

                // 1.2 content 为数组 (如 [{"type":"text","text":"..."}])
                val contentArray = messageObj.optJSONArray("content")
                if (contentArray != null && contentArray.length() > 0) {
                    val sb = StringBuilder()
                    for (i in 0 until contentArray.length()) {
                        val itemObj = contentArray.optJSONObject(i)
                        if (itemObj != null) {
                            val text = itemObj.optString("text", "").ifBlank { itemObj.optString("content", "") }
                            if (text.isNotBlank()) {
                                if (sb.isNotEmpty()) sb.append("\n")
                                sb.append(text)
                            }
                        } else {
                            val strItem = contentArray.optString(i, "")
                            if (strItem.isNotBlank()) {
                                if (sb.isNotEmpty()) sb.append("\n")
                                sb.append(strItem)
                            }
                        }
                    }
                    if (sb.isNotBlank()) return sb.toString()
                }

                if (contentStr.isNotBlank()) return contentStr
            }

            // 2. 尝试 choices[0].text (Legacy 格式)
            val textDirect = firstChoice.optString("text", "").trim()
            if (textDirect.isNotBlank()) return textDirect

            // 3. 尝试 choices[0].delta.content (Stream 格式)
            val deltaObj = firstChoice.optJSONObject("delta")
            if (deltaObj != null) {
                val deltaContent = deltaObj.optString("content", "").trim()
                if (deltaContent.isNotBlank()) return deltaContent
            }

            null
        } catch (_: Exception) {
            null
        }
    }

    private fun isImageGenerationModelNotice(bodyString: String?): Boolean {
        if (bodyString.isNullOrBlank()) return false
        val lower = bodyString.lowercase()
        return lower.contains("image generation") ||
                lower.contains("image model") ||
                lower.contains("images/generations") ||
                lower.contains("dall-e")
    }

    private fun formatDiagnosticInfo(url: String, response: Response, contentType: String?, bodyString: String?): String {
        val truncatedBody = if (!bodyString.isNullOrBlank() && bodyString.length > 2048) {
            bodyString.take(2048) + "... (截断)"
        } else {
            bodyString ?: "空响应体"
        }
        return """
            Endpoint: $url
            HTTP Code: ${response.code} ${response.message}
            Content-Type: $contentType
            Response Fragment: $truncatedBody
        """.trimIndent()
    }

    private fun parseModelsResponse(jsonString: String): List<AIModel> {
        val list = mutableListOf<AIModel>()
        try {
            val json = JSONObject(jsonString)
            val dataArray = json.optJSONArray("data") ?: return emptyList()
            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                val id = item.optString("id", "")
                if (id.isNotBlank()) {
                    list.add(AIModel(id = id))
                }
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return list.sortedBy { it.id }
    }
}
