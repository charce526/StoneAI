package cn.xzbim.stoneai.data.repository

import android.content.Context
import cn.xzbim.stoneai.ai.AIProviderFactory
import cn.xzbim.stoneai.data.local.AppPreferences
import cn.xzbim.stoneai.model.AIConfig
import cn.xzbim.stoneai.model.AIConnectionResult
import cn.xzbim.stoneai.model.AIImageResponse
import cn.xzbim.stoneai.model.AIModel
import cn.xzbim.stoneai.model.ProcessedImage
import cn.xzbim.stoneai.model.RockRecognitionResult
import cn.xzbim.stoneai.prompt.RockRecognitionPrompt
import cn.xzbim.stoneai.util.RockResultParser

/**
 * AIRepository 接口实现
 */
class AIRepositoryImpl(
    private val configRepository: AIConfigRepository,
    private val appPreferences: AppPreferences,
    private val providerFactory: AIProviderFactory = AIProviderFactory()
) : AIRepository {

    override suspend fun testConnection(
        config: AIConfig,
        apiKey: String
    ): AIConnectionResult {
        val provider = providerFactory.getProvider(config.providerType)
        return provider.testConnection(config, apiKey)
    }

    override suspend fun getModels(
        config: AIConfig,
        apiKey: String
    ): Result<List<AIModel>> {
        val provider = providerFactory.getProvider(config.providerType)
        return provider.getModels(config, apiKey)
    }

    override suspend fun analyzeImageWithActiveConfig(
        image: ProcessedImage,
        prompt: String
    ): Result<AIImageResponse> {
        val activeConfig = configRepository.getActiveConfig()
            ?: return Result.failure(IllegalStateException("尚未配置并激活 AI 服务，请先前往设置添加 AI 配置"))

        val apiKey = configRepository.getApiKey(activeConfig.id)
        if (apiKey.isNullOrBlank()) {
            return Result.failure(IllegalStateException("当前激活的 AI 配置缺少有效的 API Key，请重新编辑该服务并填写 API Key"))
        }

        val provider = providerFactory.getProvider(activeConfig.providerType)
        return provider.analyzeImage(activeConfig, apiKey, image, prompt)
    }

    override suspend fun recognizeRock(
        image: ProcessedImage
    ): Result<RockRecognitionResult> {
        val activeConfig = configRepository.getActiveConfig()
            ?: return Result.failure(IllegalStateException("尚未配置并激活 AI 服务，请先前往设置添加 AI 配置"))

        val apiKey = configRepository.getApiKey(activeConfig.id)
        if (apiKey.isNullOrBlank()) {
            return Result.failure(IllegalStateException("当前激活的 AI 配置缺少有效的 API Key，请重新编辑该服务并填写 API Key"))
        }

        val provider = providerFactory.getProvider(activeConfig.providerType)
        val langSetting = appPreferences.getRecognitionLanguage()
        val userPrompt = RockRecognitionPrompt.buildUserPrompt(langSetting.promptInstruction)
        val fullPrompt = "${RockRecognitionPrompt.SYSTEM_PROMPT}\n\n$userPrompt"

        val responseResult = provider.analyzeImage(
            config = activeConfig,
            apiKey = apiKey,
            image = image,
            prompt = fullPrompt
        )

        return responseResult.map { aiImageResponse ->
            RockResultParser.parse(aiImageResponse.content)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AIRepositoryImpl? = null

        fun getInstance(context: Context): AIRepositoryImpl {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val instance = AIRepositoryImpl(
                    configRepository = AIConfigRepositoryImpl.getInstance(appContext),
                    appPreferences = AppPreferences.getInstance(appContext)
                )
                INSTANCE = instance
                instance
            }
        }
    }
}
