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
import kotlinx.coroutines.launch

/**
 * 记录页 UiState
 */
data class HistoryUiState(
    val records: List<RecognitionHistoryEntity> = emptyList(),
    val isLoading: Boolean = false,
    val userMessage: String? = null
)

/**
 * 历史记录 ViewModel (实时观察 Room 数据库)
 */
class HistoryViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: RecognitionHistoryRepository = RecognitionHistoryRepositoryImpl.getInstance(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        observeHistory()
    }

    private fun observeHistory() {
        repository.observeAll()
            .onEach { list ->
                _uiState.value = _uiState.value.copy(
                    records = list,
                    isLoading = false
                )
            }
            .launchIn(viewModelScope)
    }

    fun deleteRecord(id: String) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.deleteAllHistory()
        }
    }

    fun onUserMessageShown() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
