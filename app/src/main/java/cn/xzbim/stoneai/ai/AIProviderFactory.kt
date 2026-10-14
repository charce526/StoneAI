package cn.xzbim.stoneai.ai

import cn.xzbim.stoneai.model.AIProviderType

/**
 * AI Provider 工厂类
 */
class AIProviderFactory(
    private val openAICompatibleProvider: OpenAICompatibleProvider = OpenAICompatibleProvider()
) {
    fun getProvider(type: AIProviderType): AIProvider {
        return when (type) {
            AIProviderType.OPENAI_COMPATIBLE -> openAICompatibleProvider
        }
    }
}
