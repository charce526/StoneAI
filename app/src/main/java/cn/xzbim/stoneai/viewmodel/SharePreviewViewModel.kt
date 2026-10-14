package cn.xzbim.stoneai.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.xzbim.stoneai.data.repository.RecognitionHistoryRepository
import cn.xzbim.stoneai.data.repository.RecognitionHistoryRepositoryImpl
import cn.xzbim.stoneai.model.RockRecognitionResult
import cn.xzbim.stoneai.model.CaptureMetadata
import cn.xzbim.stoneai.share.RockShareData
import cn.xzbim.stoneai.share.ShareCardGenerator
import cn.xzbim.stoneai.share.ShareManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * 分享预览 UiState
 */
data class SharePreviewUiState(
    val isLoading: Boolean = true,
    val shareData: RockShareData? = null,
    val previewFile: File? = null,
    val previewAspectRatio: Float = 3f / 4f,
    val isSharing: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 分享预览 ViewModel
 */
class SharePreviewViewModel @JvmOverloads constructor(
    application: Application,
    private val historyRepository: RecognitionHistoryRepository = RecognitionHistoryRepositoryImpl.getInstance(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SharePreviewUiState())
    val uiState: StateFlow<SharePreviewUiState> = _uiState.asStateFlow()

    /**
     * 1. 来自刚识图完成的实时结果
     */
    fun initWithResult(
        context: Context,
        result: RockRecognitionResult,
        imageModel: Any?,
        captureMetadata: CaptureMetadata?
    ) {
        val data = RockShareData.fromResult(result, imageModel, captureMetadata)
        preparePreview(context, data)
    }

    /**
     * 2. 来自历史记录详情
     */
    fun initWithHistoryId(context: Context, historyId: String) {
        if (historyId.isBlank()) {
            _uiState.value = SharePreviewUiState(isLoading = false, errorMessage = "历史记录标识无效")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val entity = historyRepository.getById(historyId)

            if (entity == null) {
                _uiState.value = SharePreviewUiState(isLoading = false, errorMessage = "未找到对应的历史记录")
                return@launch
            }

            preparePreview(context, RockShareData.fromHistoryEntity(entity))
        }
    }

    private fun preparePreview(context: Context, data: RockShareData) {
        viewModelScope.launch {
            _uiState.value = SharePreviewUiState(isLoading = true, shareData = data)
            runCatching { ShareCardGenerator.generateCardFile(context, data) }
                .onSuccess { file ->
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, bounds)
                    val aspectRatio = if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                        bounds.outWidth.toFloat() / bounds.outHeight.toFloat()
                    } else {
                        3f / 4f
                    }
                    _uiState.value = SharePreviewUiState(
                        isLoading = false,
                        shareData = data,
                        previewFile = file,
                        previewAspectRatio = aspectRatio
                    )
                }
                .onFailure { error ->
                    _uiState.value = SharePreviewUiState(
                        isLoading = false,
                        shareData = data,
                        errorMessage = error.localizedMessage ?: "分享预览生成失败，请重试"
                    )
                }
        }
    }

    /**
     * 用户明确点击“立即分享”：触发离屏高清 1080x1440 图片渲染并拉起系统分享面板
     */
    fun performShare(context: Context) {
        val data = _uiState.value.shareData ?: return
        if (_uiState.value.isSharing) return

        _uiState.value = _uiState.value.copy(isSharing = true)

        viewModelScope.launch {
            val previewFile = _uiState.value.previewFile
            val result = if (previewFile != null) {
                ShareManager.shareCardFile(context, data, previewFile)
            } else {
                Result.failure(IllegalStateException("分享预览尚未生成"))
            }

            result.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.localizedMessage ?: "分享图片生成失败，请重试"
                )
            }

            _uiState.value = _uiState.value.copy(isSharing = false)
        }
    }

    fun onErrorMessageShown() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
