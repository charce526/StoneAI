package cn.xzbim.stoneai.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.xzbim.stoneai.data.local.db.RecognitionHistoryEntity
import cn.xzbim.stoneai.data.repository.RecognitionHistoryRepository
import cn.xzbim.stoneai.data.repository.RecognitionHistoryRepositoryImpl
import cn.xzbim.stoneai.model.CaptureMetadata
import cn.xzbim.stoneai.model.RockRecognitionResult
import cn.xzbim.stoneai.share.RockShareData
import cn.xzbim.stoneai.share.ShareManager
import cn.xzbim.stoneai.util.RockResultParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 历史记录详情 UiState
 */
data class HistoryDetailUiState(
    val isLoading: Boolean = true,
    val entity: RecognitionHistoryEntity? = null,
    val rockResult: RockRecognitionResult? = null,
    val captureMetadata: CaptureMetadata? = null,
    val isNotFound: Boolean = false,
    val isDeleted: Boolean = false,
    val isSharing: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 历史记录详情 ViewModel
 */
class HistoryDetailViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: RecognitionHistoryRepository = RecognitionHistoryRepositoryImpl.getInstance(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HistoryDetailUiState())
    val uiState: StateFlow<HistoryDetailUiState> = _uiState.asStateFlow()

    fun loadHistoryDetail(historyId: String) {
        if (historyId.isBlank()) {
            _uiState.value = HistoryDetailUiState(isLoading = false, isNotFound = true)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val entity = try {
                repository.getById(historyId)
            } catch (_: Exception) {
                null
            }

            if (entity == null) {
                _uiState.value = HistoryDetailUiState(isLoading = false, isNotFound = true)
                return@launch
            }

            val parsedResult = try {
                RockResultParser.parse(entity.resultJson)
            } catch (_: Exception) {
                null
            }

            val metadata = CaptureMetadata(
                capturedAt = entity.capturedAt,
                latitude = entity.latitude,
                longitude = entity.longitude,
                locationName = entity.locationName
            )

            if (parsedResult == null) {
                _uiState.value = HistoryDetailUiState(
                    isLoading = false,
                    entity = entity,
                    captureMetadata = metadata,
                    errorMessage = "识别结果解析故障，无法加载完整数据"
                )
            } else {
                _uiState.value = HistoryDetailUiState(
                    isLoading = false,
                    entity = entity,
                    rockResult = parsedResult,
                    captureMetadata = metadata
                )
            }
        }
    }

    /**
     * 生成精美海报卡片并拉起系统分享面板
     */
    fun shareHistoryRecord(context: Context) {
        val entity = _uiState.value.entity ?: return
        if (_uiState.value.isSharing) return

        _uiState.value = _uiState.value.copy(isSharing = true)

        viewModelScope.launch {
            val shareData = RockShareData.fromHistoryEntity(entity)
            val shareResult = ShareManager.shareResult(context, shareData)

            shareResult.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.localizedMessage ?: "分享卡片生成失败，请重试"
                )
            }

            _uiState.value = _uiState.value.copy(isSharing = false)
        }
    }

    fun deleteHistoryRecord(onSuccess: () -> Unit) {
        val entity = _uiState.value.entity ?: return
        viewModelScope.launch {
            try {
                repository.deleteById(entity.id)
                _uiState.value = _uiState.value.copy(isDeleted = true)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.localizedMessage ?: "删除失败，请稍后重试"
                )
            }
        }
    }

    fun onErrorMessageShown() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
