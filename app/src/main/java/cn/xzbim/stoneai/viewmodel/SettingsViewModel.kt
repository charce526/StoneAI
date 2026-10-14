package cn.xzbim.stoneai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.xzbim.stoneai.data.local.AppPreferences
import cn.xzbim.stoneai.data.repository.AIConfigRepository
import cn.xzbim.stoneai.data.repository.AIConfigRepositoryImpl
import cn.xzbim.stoneai.model.AIConfig
import cn.xzbim.stoneai.model.ImageQuality
import cn.xzbim.stoneai.model.RecognitionLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

/**
 * 设置页 UiState
 */
data class SettingsUiState(
    val isFirstLaunch: Boolean = false,
    val activeConfigSummary: String = "尚未配置",
    val imageQuality: ImageQuality = ImageQuality.STANDARD,
    val recognitionLanguage: RecognitionLanguage = RecognitionLanguage.SYSTEM,
    val saveHistory: Boolean = true,
    val saveImages: Boolean = true,
    val appVersion: String = "1.0.0",
    val userMessage: String? = null
)

/**
 * 设置页 ViewModel
 */
class SettingsViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: AIConfigRepository = AIConfigRepositoryImpl.getInstance(application),
    private val appPreferences: AppPreferences = AppPreferences.getInstance(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadAppVersion()
        observeSettings()
    }

    private fun loadAppVersion() {
        try {
            val packageInfo = getApplication<Application>().packageManager.getPackageInfo(
                getApplication<Application>().packageName,
                0
            )
            val versionName = packageInfo.versionName ?: "1.0.0"
            _uiState.value = _uiState.value.copy(appVersion = versionName)
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(appVersion = "1.0.0")
        }
    }

    private fun observeSettings() {
        combine(
            repository.getConfigsFlow(),
            appPreferences.isFirstLaunchFlow,
            appPreferences.imageQualityFlow,
            appPreferences.recognitionLanguageFlow,
            appPreferences.saveHistoryFlow,
            appPreferences.saveImagesFlow
        ) { args: Array<Any> ->
            @Suppress("UNCHECKED_CAST")
            val configs = args[0] as List<AIConfig>
            val isFirstLaunch = args[1] as Boolean
            val quality = args[2] as ImageQuality
            val language = args[3] as RecognitionLanguage
            val saveHistory = args[4] as Boolean
            val saveImages = args[5] as Boolean

            val active = configs.find { it.isActive }
            val summary = if (active != null) {
                "${active.name} · ${active.model}"
            } else {
                "尚未配置"
            }
            _uiState.value = _uiState.value.copy(
                isFirstLaunch = isFirstLaunch,
                activeConfigSummary = summary,
                imageQuality = quality,
                recognitionLanguage = language,
                saveHistory = saveHistory,
                saveImages = saveImages
            )
        }.launchIn(viewModelScope)
    }

    fun completeFirstLaunch() {
        viewModelScope.launch {
            appPreferences.setFirstLaunchCompleted()
        }
    }

    fun setImageQuality(quality: ImageQuality) {
        viewModelScope.launch {
            appPreferences.setImageQuality(quality)
            _uiState.value = _uiState.value.copy(userMessage = "图片质量已切换为：${quality.displayName}")
        }
    }

    fun setRecognitionLanguage(language: RecognitionLanguage) {
        viewModelScope.launch {
            appPreferences.setRecognitionLanguage(language)
            _uiState.value = _uiState.value.copy(userMessage = "识图语言已切换为：${language.displayName}")
        }
    }

    fun setSaveHistory(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setSaveHistory(enabled)
            val msg = if (enabled) "开启历史自动保存" else "关闭历史自动保存"
            _uiState.value = _uiState.value.copy(userMessage = msg)
        }
    }

    fun setSaveImages(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setSaveImages(enabled)
            val msg = if (enabled) "开启本地识别图片保存" else "关闭本地识别图片保存"
            _uiState.value = _uiState.value.copy(userMessage = msg)
        }
    }

    fun clearImageCache() {
        viewModelScope.launch {
            val freedBytes = appPreferences.clearImageCache()
            val freedKb = freedBytes / 1024
            val msg = if (freedKb > 0) "已清理图片缓存 ${freedKb} KB" else "缓存已被清理完毕"
            _uiState.value = _uiState.value.copy(userMessage = msg)
        }
    }

    fun onUserMessageShown() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
