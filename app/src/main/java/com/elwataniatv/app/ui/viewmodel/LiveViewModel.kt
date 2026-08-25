package com.elwataniatv.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elwataniatv.app.data.local.CommentEntity
import com.elwataniatv.app.data.local.ProgramReminder
import com.elwataniatv.app.data.model.BreakingNews
import com.elwataniatv.app.data.model.EpgItem
import com.elwataniatv.app.data.model.RemoteStream
import com.elwataniatv.app.data.model.StreamHealthState
import com.elwataniatv.app.data.repository.WataniaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LiveViewModel @Inject constructor(
    private val repository: WataniaRepository
) : ViewModel() {

    val streams: StateFlow<List<RemoteStream>> = repository.streams
    private val _selectedStream = MutableStateFlow<RemoteStream?>(null)
    val selectedStream: StateFlow<RemoteStream?> = _selectedStream.asStateFlow()
    private var pendingStreamId: String? = null

    init {
        viewModelScope.launch {
            streams.collect { streamList ->
                val pendingId = pendingStreamId
                val current = _selectedStream.value
                val resolvedPending = pendingId?.let { id -> streamList.firstOrNull { it.id == id } }
                when {
                    resolvedPending != null -> {
                        _selectedStream.value = resolvedPending
                        pendingStreamId = null
                    }
                    current == null && streamList.isNotEmpty() -> _selectedStream.value = streamList.first()
                    current != null && streamList.none { it.id == current.id } -> {
                        _selectedStream.value = streamList.firstOrNull()
                    }
                }
            }
        }
    }

    val breaking: StateFlow<BreakingNews> = repository.breaking
    val epgList: StateFlow<List<EpgItem>> = repository.epgList
    val comments: StateFlow<List<CommentEntity>> = repository.comments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val reminders: StateFlow<List<ProgramReminder>> = repository.reminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val liveReactions = repository.liveReactions
    val myReaction = repository.myReaction
    val streamHealthState: StateFlow<StreamHealthState> = repository.streamHealthState
    val adBanners = repository.adBanners
    val activeDevices = repository.activeDevices
    val syncError: StateFlow<String?> = repository.syncError

    fun runStreamHealthCheck() = repository.runStreamHealthCheck()

    fun selectStream(stream: RemoteStream) {
        if (_selectedStream.value?.id != stream.id) _selectedStream.value = stream
    }

    fun selectStreamById(id: String?) {
        val normalizedId = id?.trim().orEmpty()
        if (normalizedId.isBlank()) return
        repository.streams.value.firstOrNull { it.id == normalizedId }?.let {
            selectStream(it)
        } ?: run {
            pendingStreamId = normalizedId
        }
    }

    fun toggleAutoStreamCheck(enabled: Boolean) = repository.toggleAutoStreamCheck(enabled)

    fun toggleReminder(item: EpgItem, isSet: Boolean) {
        viewModelScope.launch {
            if (isSet) repository.removeReminder(item.id) else repository.addReminder(item)
        }
    }

    fun addComment(author: String, text: String, onResult: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch { repository.addComment(author, text, onResult) }
    }
}
