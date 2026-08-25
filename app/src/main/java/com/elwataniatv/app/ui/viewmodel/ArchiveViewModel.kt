package com.elwataniatv.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elwataniatv.app.data.local.FavoriteProgram
import com.elwataniatv.app.data.local.WatchHistoryItem
import com.elwataniatv.app.data.model.ArchiveProgram
import com.elwataniatv.app.ui.screens.archive.ALL_CATEGORY
import com.elwataniatv.app.data.repository.WataniaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val repository: WataniaRepository
) : ViewModel() {

    val archivePrograms = repository.archivePrograms
    val favorites: StateFlow<List<FavoriteProgram>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val watchHistory: StateFlow<List<WatchHistoryItem>> = repository.watchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedCategory = MutableStateFlow(ALL_CATEGORY)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredArchive: StateFlow<List<ArchiveProgram>> = combine(
        archivePrograms,
        selectedCategory,
        searchQuery
    ) { programs, category, query ->
        programs.filter { program ->
            val categoryMatches = category == ALL_CATEGORY || program.category == category
            val queryMatches = query.isBlank() ||
                program.title.contains(query, ignoreCase = true) ||
                program.description.contains(query, ignoreCase = true)
            categoryMatches && queryMatches
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectCategory(category: String) { _selectedCategory.value = category }

    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    fun toggleFavorite(program: ArchiveProgram, isFavorite: Boolean) {
        viewModelScope.launch { repository.toggleFavorite(program, isFavorite) }
    }

    fun saveWatchProgress(program: ArchiveProgram, positionMs: Long = 0, durationMs: Long = 0) {
        viewModelScope.launch { repository.saveWatchProgress(program, positionMs, durationMs) }
    }

    fun saveHistoryPosition(item: WatchHistoryItem, positionMs: Long) {
        viewModelScope.launch { repository.saveHistoryPosition(item, positionMs) }
    }

    fun clearWatchHistory() {
        viewModelScope.launch { repository.clearWatchHistory() }
    }
}
