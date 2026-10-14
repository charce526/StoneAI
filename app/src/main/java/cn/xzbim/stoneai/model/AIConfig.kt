package cn.xzbim.stoneai.model

import org.json.JSONObject

/**
 * AI 服务提供商类型
 */
enum class AIProviderType(val displayName: String) {
    OPENAI_COMPATIBLE("OpenAI Compatible")
}

/**
 * AI 服务配置数据模型
 * 注意：apiKey 不保存在此普通模型或明文存储中，由 Keystore 安全隔离。
 */
data class AIConfig(
    val id: String = "",
    val name: String,
    val providerType: AIProviderType = AIProviderType.OPENAI_COMPATIBLE,
    val baseUrl: String,
    val model: String,
    val isActive: Boolean = false
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("providerType", providerType.name)
            put("baseUrl", baseUrl)
            put("model", model)
            put("isActive", isActive)
        }
    }

    companion object {
        fun fromJsonObject(jsonObject: JSONObject): AIConfig {
            val providerTypeName = jsonObject.optString("providerType", AIProviderType.OPENAI_COMPATIBLE.name)
            val type = try {
                AIProviderType.valueOf(providerTypeName)
            } catch (_: Exception) {
                AIProviderType.OPENAI_COMPATIBLE
            }

            return AIConfig(
                id = jsonObject.optString("id", ""),
                name = jsonObject.optString("name", ""),
                providerType = type,
                baseUrl = jsonObject.optString("baseUrl", ""),
                model = jsonObject.optString("model", ""),
                isActive = jsonObject.optBoolean("isActive", false)
            )
        }
    }
}
