package cn.chitanda.app.imovie.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.map
import cn.chitanda.app.imovie.core.data.repository.HistoryRepository
import cn.chitanda.app.imovie.core.data.repository.asHistoryResource
import cn.chitanda.app.imovie.core.model.HistoryResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * @author: Chen
 * @createTime: 2023/2/8 15:19
 * @description:
 **/
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val pager = Pager(
        config = PagingConfig(pageSize = 20), pagingSourceFactory = ::pagingSourceFactory
    )

    val data = pager.flow.map { it.map { h -> h.asHistoryResource() } }.cachedIn(viewModelScope)
    private val _uiState: MutableStateFlow<HistoryUiState> by lazy(mode = LazyThreadSafetyMode.NONE) {
        MutableStateFlow(HistoryUiState(data))
    }

    private val uiState: StateFlow<HistoryUiState> get() = _uiState

    private var searchQuery: String = ""
    private fun pagingSourceFactory() =
        if (searchQuery.isNotEmpty() && searchQuery.isNotBlank()) {
            historyRepository.getSearchHistoryPagingSource(
                searchQuery
            )
        } else {
            historyRepository.getHistoryPagingSource()
        }

    fun deleteHistory(history: HistoryResource) {
        viewModelScope.launch(Dispatchers.IO) {
            historyRepository.deleteHistory(history)
            withContext(Dispatchers.Main) {
                _uiState.emit(_uiState.value.copy(revoke = history))
            }
        }
    }
}