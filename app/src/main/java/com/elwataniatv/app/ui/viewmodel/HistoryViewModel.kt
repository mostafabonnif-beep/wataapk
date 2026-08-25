package com.elwataniatv.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elwataniatv.app.data.local.WatchHistoryItem
import com.elwataniatv.app.data.repository.WataniaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: WataniaRepository
) : ViewModel() {
    val watchHistory = repository.watchHistory
    fun savePosition(item: WatchHistoryItem, positionMs: Long) {
        viewModelScope.launch { repository.saveHistoryPosition(item, positionMs) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearWatchHistory() }
    }
}
