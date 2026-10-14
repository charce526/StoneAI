package cn.xzbim.stoneai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.xzbim.stoneai.data.repository.AIConfigRepository
import cn.xzbim.stoneai.data.repository.AIConfigRepositoryImpl
import cn.xzbim.stoneai.model.AIConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * AI 服务列表页面 UiState
 */
data class AIServiceListUiState(
    val isLoading: Boolean = true,
    val configs: List<AIConfig> = emptyList(),
    val activeConfig: AIConfig? = null,
    val userMessage: String? = null
)

/**
 * AI 服务列表 ViewModel
 */
class AIServiceListViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: AIConfigRepository = AIConfigRepositoryImpl.getInstance(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AIServiceListUiState())
    val uiState: StateFlow<AIServiceListUiState> = _uiState.asStateFlow()

    init {
        observeConfigs()
    }

    private fun observeConfigs() {
        repository.getConfigsFlow()
            .onEach { list ->
                val active = list.find { it.isActive }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    configs = list,
                    activeConfig = active
                )
            }
            .launchIn(viewModelScope)
    }

    fun setActiveConfig(id: String) {
        viewModelScope.launch {
            repository.setActiveConfig(id)
        }
    }

    fun onMessageShown() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
