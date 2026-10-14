package cn.xzbim.stoneai.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.xzbim.stoneai.data.local.AppPreferences
import cn.xzbim.stoneai.data.repository.AIConfigRepository
import cn.xzbim.stoneai.data.repository.AIConfigRepositoryImpl
import cn.xzbim.stoneai.data.repository.AIRepository
import cn.xzbim.stoneai.data.repository.AIRepositoryImpl
import cn.xzbim.stoneai.data.repository.RecognitionHistoryRepository
import cn.xzbim.stoneai.data.repository.RecognitionHistoryRepositoryImpl
import cn.xzbim.stoneai.model.AIImageResponse
import cn.xzbim.stoneai.model.CaptureMetadata
import cn.xzbim.stoneai.model.ImageSource
import cn.xzbim.stoneai.model.ProcessedImage
import cn.xzbim.stoneai.model.RockRecognitionResult
import cn.xzbim.stoneai.model.SelectedImage
import cn.xzbim.stoneai.share.RockShareData
import cn.xzbim.stoneai.share.ShareManager
import cn.xzbim.stoneai.util.CaptureMetadataExtractor
import cn.xzbim.stoneai.util.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 选图、拍照与结构化 AI 识图 UiState (包含拍摄时间与地点元数据)
 */
data class ImageSelectionUiState(
    val selectedImage: SelectedImage? = null,
    val isProcessingImage: Boolean = false,
    val isAnalyzingWithAI: Boolean = false,
    val analysisStatusMessage: String? = null,
    val processedImage: ProcessedImage? = null,
    val rockResult: RockRecognitionResult? = null,
    val aiImageResponse: AIImageResponse? = null,
    val captureMetadata: CaptureMetadata? = null,
    val isSharing: Boolean = false,
    val activeConfigSummary: String? = null,
    val showNoActiveConfigDialog: Boolean = false,
    val showNoApiKeyDialog: Boolean = false,
    val errorMessage: String? = null,
    val userMessage: String? = null
)

/**
 * 选图、拍照与结构化 AI 识别 ViewModel
 */
class ImageSelectionViewModel @JvmOverloads constructor(
    application: Application,
    private val aiRepository: AIRepository = AIRepositoryImpl.getInstance(application),
    private val historyRepository: RecognitionHistoryRepository = RecognitionHistoryRepositoryImpl.getInstance(application),
    private val configRepository: AIConfigRepository = AIConfigRepositoryImpl.getInstance(application),
    private val appPreferences: AppPreferences = AppPreferences.getInstance(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ImageSelectionUiState())
    val uiState: StateFlow<ImageSelectionUiState> = _uiState.asStateFlow()

    private var analysisJob: Job? = null
    private var selectionVersion: Long = 0L

    fun setSelectedImage(uri: Uri, source: ImageSource) {
        setSelectedImage(getApplication(), uri, source)
    }

    fun setSelectedImage(context: Context, uri: Uri, source: ImageSource) {
        val version = ++selectionVersion
        _uiState.value = _uiState.value.copy(
            selectedImage = SelectedImage(uri = uri, source = source),
            processedImage = null,
            rockResult = null,
            aiImageResponse = null,
            captureMetadata = null,
            errorMessage = null
        )

        viewModelScope.launch(Dispatchers.IO) {
            // 相册 Uri 必须在转存前读取：Android 10+ 的原始媒体权限只对原 Uri 生效。
            val sourceMetadata = if (source == ImageSource.GALLERY) {
                CaptureMetadataExtractor.extractFromExif(context, uri)
            } else {
                null
            }

            try {
                val privateFile = ImageProcessor.copyUriToPrivateFile(context, uri)
                val privateUri = Uri.fromFile(privateFile)

                val metadata = if (source == ImageSource.GALLERY) {
                    val copiedMetadata = CaptureMetadataExtractor.extractFromExif(context, privateFile)
                    CaptureMetadata(
                        capturedAt = sourceMetadata?.capturedAt ?: copiedMetadata.capturedAt,
                        latitude = sourceMetadata?.latitude ?: copiedMetadata.latitude,
                        longitude = sourceMetadata?.longitude ?: copiedMetadata.longitude,
                        locationName = sourceMetadata?.locationName ?: copiedMetadata.locationName
                    )
                } else {
                    CaptureMetadataExtractor.extractFromCamera(context)
                }

                if (version != selectionVersion) return@launch
                _uiState.value = _uiState.value.copy(
                    selectedImage = SelectedImage(uri = privateUri, source = source),
                    processedImage = null,
                    rockResult = null,
                    aiImageResponse = null,
                    captureMetadata = metadata,
                    errorMessage = null
                )
            } catch (e: Exception) {
                val metadata = if (source == ImageSource.CAMERA) {
                    CaptureMetadataExtractor.extractFromCamera(context)
                } else {
                    sourceMetadata ?: CaptureMetadata()
                }
                if (version != selectionVersion) return@launch
                _uiState.value = _uiState.value.copy(
                    selectedImage = SelectedImage(uri = uri, source = source),
                    processedImage = null,
                    rockResult = null,
                    aiImageResponse = null,
                    captureMetadata = metadata,
                    errorMessage = null
                )
            }
        }
    }

    /**
     * 开始图片识别入口：进行配置检查并直接启动识别
     */
    fun startRecognitionFlow(context: Context, onSuccessNavigateToResult: () -> Unit) {
        val selected = _uiState.value.selectedImage
        if (selected == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "请先选择一张图片")
            return
        }

        viewModelScope.launch {
            val activeConfig = configRepository.getActiveConfig()
            if (activeConfig == null) {
                _uiState.value = _uiState.value.copy(showNoActiveConfigDialog = true)
                return@launch
            }

            val apiKey = configRepository.getApiKey(activeConfig.id)
            if (apiKey.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(showNoApiKeyDialog = true)
                return@launch
            }

            executeRecognition(context, onSuccessNavigateToResult)
        }
    }

    private fun executeRecognition(context: Context, onSuccessNavigateToResult: () -> Unit) {
        val selected = _uiState.value.selectedImage ?: return

        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            val activeConfigForSummary = configRepository.getActiveConfig()
            val activeSummary = activeConfigForSummary?.let { "${it.name} (${it.model})" }

            _uiState.value = _uiState.value.copy(
                isProcessingImage = true,
                errorMessage = null,
                activeConfigSummary = activeSummary
            )

            val processed = try {
                ImageProcessor.processWithPreferences(context, selected.uri)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessingImage = false,
                    errorMessage = e.localizedMessage ?: "图片处理失败，请重试"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isProcessingImage = false,
                isAnalyzingWithAI = true,
                processedImage = processed,
                analysisStatusMessage = "AI 正在观察岩石特征与纹理结构..."
            )

            val result = aiRepository.recognizeRock(processed)

            result.onSuccess { rockResult ->
                _uiState.value = _uiState.value.copy(
                    isAnalyzingWithAI = false,
                    analysisStatusMessage = null,
                    rockResult = rockResult
                )

                // 检查自动保存设置：若开启，异步写入历史
                viewModelScope.launch(Dispatchers.IO) {
                    val shouldSaveHist = appPreferences.shouldSaveHistory()
                    val shouldSaveImg = appPreferences.shouldSaveImages()
                    val activeConfig = configRepository.getActiveConfig()

                    if (shouldSaveHist) {
                        val saveResult = historyRepository.saveHistory(
                            result = rockResult,
                            processedImage = processed,
                            saveHistorySetting = true,
                            saveImageSetting = shouldSaveImg,
                            configName = activeConfig?.name,
                            modelName = activeConfig?.model,
                            metadata = _uiState.value.captureMetadata
                        )

                        saveResult.onSuccess { id ->
                            val currentMeta = _uiState.value.captureMetadata
                            if (currentMeta != null && currentMeta.hasData()) {
                                historyRepository.updateMetadata(id, currentMeta)
                            }
                        }.onFailure {
                            Log.w("ImageViewModel", "识别成功，但自动保存历史异常: ${it.message}")
                        }
                    }
                }

                onSuccessNavigateToResult()
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isAnalyzingWithAI = false,
                    analysisStatusMessage = null,
                    errorMessage = throwable.localizedMessage ?: "AI 分析失败，请重试"
                )
            }
        }
    }

    fun cancelAnalysis() {
        analysisJob?.cancel()
        analysisJob = null
        _uiState.value = _uiState.value.copy(
            isProcessingImage = false,
            isAnalyzingWithAI = false,
            analysisStatusMessage = null
        )
    }

    fun dismissNoActiveConfigDialog() {
        _uiState.value = _uiState.value.copy(showNoActiveConfigDialog = false)
    }

    fun dismissNoApiKeyDialog() {
        _uiState.value = _uiState.value.copy(showNoApiKeyDialog = false)
    }

    fun dismissAIResponseDialog() {
        _uiState.value = _uiState.value.copy(aiImageResponse = null)
    }

    fun clearSelectedImage() {
        selectionVersion++
        analysisJob?.cancel()
        analysisJob = null
        _uiState.value = ImageSelectionUiState()
    }

    fun onErrorMessageShown() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun onUserMessageShown() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }

    /**
     * 生成精美海报卡片并拉起系统分享面板
     */
    fun shareCurrentResult(context: Context) {
        val currentState = _uiState.value
        val result = currentState.rockResult ?: return
        val imageModel = currentState.selectedImage?.uri

        if (currentState.isSharing) return
        _uiState.value = _uiState.value.copy(isSharing = true)

        viewModelScope.launch {
            val shareData = RockShareData.fromResult(
                result = result,
                imageModel = imageModel,
                captureMetadata = currentState.captureMetadata
            )
            val shareResult = ShareManager.shareResult(context, shareData)

            shareResult.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.localizedMessage ?: "分享卡片生成失败，请重试"
                )
            }

            _uiState.value = _uiState.value.copy(isSharing = false)
        }
    }
}
