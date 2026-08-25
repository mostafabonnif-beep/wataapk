package com.elwataniatv.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elwataniatv.app.data.repository.WataniaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: WataniaRepository,
    @ApplicationContext context: Context
) : ViewModel() {
    private companion object {
        const val PREFS_NAME = "notification_state"
        const val READ_IDS_KEY = "read_notification_ids"
    }

    val notifications = repository.inAppNotifications
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val readIds = MutableStateFlow(
        preferences.getStringSet(READ_IDS_KEY, emptySet()).orEmpty()
    )

    val unreadCount: StateFlow<Int> = combine(notifications, readIds) { items, read ->
        items.count { it.id !in read }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun markRead(id: String) {
        if (id.isBlank() || id in readIds.value) return
        val updated = readIds.value + id
        readIds.value = updated
        preferences.edit().putStringSet(READ_IDS_KEY, updated).apply()
    }

    fun markAllRead() {
        val updated = notifications.value.map { it.id }.toSet()
        readIds.value = updated
        preferences.edit().putStringSet(READ_IDS_KEY, updated).apply()
    }

    fun startSync() = repository.startFirebaseSync()
}
