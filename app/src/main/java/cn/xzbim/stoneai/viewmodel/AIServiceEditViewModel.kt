package cn.xzbim.stoneai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.xzbim.stoneai.data.repository.AIConfigRepository
import cn.xzbim.stoneai.data.repository.AIConfigRepositoryImpl
import cn.xzbim.stoneai.data.repository.AIRepository
import cn.xzbim.stoneai.data.repository.AIRepositoryImpl
import cn.xzbim.stoneai.model.AIConfig
import cn.xzbim.stoneai.model.AIConnectionResult
import cn.xzbim.stoneai.model.AIModel
import cn.xzbim.stoneai.model.AIProviderType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 编辑/新增 AI 服务 UiState
 */
data class AIServiceEditUiState(
    val configId: String? = null,
    val name: String = "",
    val providerType: AIProviderType = AIProviderType.OPENAI_COMPATIBLE,
    val baseUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val hasExistingApiKey: Boolean = false,
    val model: String = "gpt-4o",
    val isEditMode: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val isTestingConnection: Boolean = false,
    val testConnectionResult: AIConnectionResult? = null,
    val isFetchingModels: Boolean = false,
    val availableModels: List<AIModel> = emptyList(),
    val showModelPicker: Boolean = false,
    val fetchModelsErrorDetail: String? = null,
    val errorMessage: String? = null,
    val userMessage: String? = null
)

/**
 * 编辑/新增 AI 服务 ViewModel
 */
class AIServiceEditViewModel @JvmOverloads constructor(
    application: Application,
    private val configRepository: AIConfigRepository = AIConfigRepositoryImpl.getInstance(application),
    private val aiRepository: AIRepository = AIRepositoryImpl.getInstance(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AIServiceEditUiState())
    val uiState: StateFlow<AIServiceEditUiState> = _uiState.asStateFlow()

    fun loadConfig(configId: String?) {
        if (configId.isNullOrBlank()) {
            _uiState.value = AIServiceEditUiState(
                isEditMode = false,
                baseUrl = "https://api.openai.com/v1",
                model = "gpt-4o"
            )
            return
        }

        viewModelScope.launch {
            val config = configRepository.getConfig(configId)
            if (config != null) {
                val hasKey = configRepository.hasApiKey(configId)
                _uiState.value = AIServiceEditUiState(
                    configId = config.id,
                    name = config.name,
                    providerType = config.providerType,
                    baseUrl = config.baseUrl,
                    model = config.model,
                    hasExistingApiKey = hasKey,
                    isEditMode = true
                )
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "未找到该 AI 配置")
            }
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name, errorMessage = null)
    }

    fun updateBaseUrl(baseUrl: String) {
        _uiState.value = _uiState.value.copy(baseUrl = baseUrl, errorMessage = null)
    }

    fun updateApiKey(apiKey: String) {
        _uiState.value = _uiState.value.copy(apiKey = apiKey, errorMessage = null)
    }

    fun updateModel(model: String) {
        _uiState.value = _uiState.value.copy(model = model, errorMessage = null)
    }

    fun testConnection() {
        val currentState = _uiState.value
        val trimmedBaseUrl = currentState.baseUrl.trim()
        val trimmedModel = currentState.model.trim()
        val trimmedApiKey = currentState.apiKey.trim()

        if (trimmedBaseUrl.isBlank()) {
            _uiState.value = currentState.copy(errorMessage = "API 地址不能为空")
            return
        }

        if (!trimmedBaseUrl.startsWith("http://", ignoreCase = true) &&
            !trimmedBaseUrl.startsWith("https://", ignoreCase = true)
        ) {
            _uiState.value = currentState.copy(errorMessage = "API 地址必须以 http:// 或 https:// 开头")
            return
        }

        if (trimmedModel.isBlank()) {
            _uiState.value = currentState.copy(errorMessage = "模型名称不能为空")
            return
        }

        viewModelScope.launch {
            var effectiveKey = trimmedApiKey
            if (effectiveKey.isBlank() && currentState.configId != null) {
                effectiveKey = configRepository.getApiKey(currentState.configId) ?: ""
            }

            if (effectiveKey.isBlank()) {
                _uiState.value = _uiState.value.copy(errorMessage = "请填写 API Key")
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isTestingConnection = true,
                testConnectionResult = null,
                errorMessage = null
            )

            val tempConfig = AIConfig(
                id = currentState.configId ?: "",
                name = currentState.name.ifBlank { "测试配置" },
                providerType = currentState.providerType,
                baseUrl = trimmedBaseUrl,
                model = trimmedModel
            )

            val result = aiRepository.testConnection(tempConfig, effectiveKey)
            _uiState.value = _uiState.value.copy(
                isTestingConnection = false,
                testConnectionResult = result
            )
        }
    }

    fun fetchModels() {
        val currentState = _uiState.value
        val trimmedBaseUrl = currentState.baseUrl.trim()
        val trimmedApiKey = currentState.apiKey.trim()

        if (trimmedBaseUrl.isBlank()) {
            _uiState.value = currentState.copy(errorMessage = "API 地址不能为空")
            return
        }

        if (!trimmedBaseUrl.startsWith("http://", ignoreCase = true) &&
            !trimmedBaseUrl.startsWith("https://", ignoreCase = true)
        ) {
            _uiState.value = currentState.copy(errorMessage = "API 地址必须以 http:// 或 https:// 开头")
            return
        }

        viewModelScope.launch {
            var effectiveKey = trimmedApiKey
            if (effectiveKey.isBlank() && currentState.configId != null) {
                effectiveKey = configRepository.getApiKey(currentState.configId) ?: ""
            }

            if (effectiveKey.isBlank()) {
                _uiState.value = _uiState.value.copy(errorMessage = "请填写 API Key")
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isFetchingModels = true,
                fetchModelsErrorDetail = null,
                errorMessage = null
            )

            val tempConfig = AIConfig(
                id = currentState.configId ?: "",
                name = currentState.name.ifBlank { "测试配置" },
                providerType = currentState.providerType,
                baseUrl = trimmedBaseUrl,
                model = currentState.model.ifBlank { "default" }
            )

            val result = aiRepository.getModels(tempConfig, effectiveKey)
            result.onSuccess { models ->
                if (models.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isFetchingModels = false,
                        availableModels = models,
                        showModelPicker = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isFetchingModels = false,
                        userMessage = "未获取到模型列表，您可以手动输入模型名称"
                    )
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isFetchingModels = false,
                    fetchModelsErrorDetail = error.localizedMessage ?: "无法获取模型列表，您可以手动输入模型名称"
                )
            }
        }
    }

    fun selectModel(modelId: String) {
        _uiState.value = _uiState.value.copy(
            model = modelId,
            showModelPicker = false
        )
    }

    fun dismissModelPicker() {
        _uiState.value = _uiState.value.copy(showModelPicker = false)
    }

    fun dismissTestConnectionResult() {
        _uiState.value = _uiState.value.copy(testConnectionResult = null)
    }

    fun dismissFetchModelsError() {
        _uiState.value = _uiState.value.copy(fetchModelsErrorDetail = null)
    }

    fun saveConfig() {
        val currentState = _uiState.value
        val trimmedName = currentState.name.trim()
        val trimmedBaseUrl = currentState.baseUrl.trim()
        val trimmedModel = currentState.model.trim()
        val trimmedApiKey = currentState.apiKey.trim()

        if (trimmedName.isBlank()) {
            _uiState.value = currentState.copy(errorMessage = "配置名称不能为空")
            return
        }

        if (trimmedBaseUrl.isBlank()) {
            _uiState.value = currentState.copy(errorMessage = "API 地址不能为空")
            return
        }

        if (!trimmedBaseUrl.startsWith("http://", ignoreCase = true) &&
            !trimmedBaseUrl.startsWith("https://", ignoreCase = true)
        ) {
            _uiState.value = currentState.copy(errorMessage = "API 地址必须以 http:// 或 https:// 开头")
            return
        }

        if (trimmedModel.isBlank()) {
            _uiState.value = currentState.copy(errorMessage = "模型名称不能为空")
            return
        }

        // 如果是新增配置且没有 API Key
        if (!currentState.isEditMode && trimmedApiKey.isBlank()) {
            _uiState.value = currentState.copy(errorMessage = "请填写 API Key")
            return
        }

        _uiState.value = currentState.copy(isSaving = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val config = AIConfig(
                    id = currentState.configId ?: "",
                    name = trimmedName,
                    providerType = currentState.providerType,
                    baseUrl = trimmedBaseUrl,
                    model = trimmedModel
                )
                configRepository.saveConfig(
                    config = config,
                    apiKey = trimmedApiKey.ifBlank { null }
                )
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = "保存配置失败，请重试")
            }
        }
    }

    fun deleteConfig() {
        val id = _uiState.value.configId ?: return
        viewModelScope.launch {
            try {
                configRepository.deleteConfig(id)
                _uiState.value = _uiState.value.copy(isDeleted = true)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "删除配置失败")
            }
        }
    }

    fun onUserMessageShown() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }

    fun onErrorMessageShown() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
