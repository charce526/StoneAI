package cn.xzbim.stoneai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.xzbim.stoneai.data.local.db.RecognitionHistoryEntity
import cn.xzbim.stoneai.data.repository.RecognitionHistoryRepository
import cn.xzbim.stoneai.data.repository.RecognitionHistoryRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * 首页/识别页 UiState
 */
data class HomeUiState(
    val recentRecords: List<RecognitionHistoryEntity> = emptyList(),
    val message: String? = null
)

/**
 * 首页 ViewModel (实时观察 Room 数据库最新 5 条识别记录)
 */
class HomeViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: RecognitionHistoryRepository = RecognitionHistoryRepositoryImpl.getInstance(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeRecentRecords()
    }

    private fun observeRecentRecords() {
        repository.observeRecent(5)
            .onEach { list ->
                _uiState.value = _uiState.value.copy(recentRecords = list)
            }
            .launchIn(viewModelScope)
    }

    fun onMessageShown() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
