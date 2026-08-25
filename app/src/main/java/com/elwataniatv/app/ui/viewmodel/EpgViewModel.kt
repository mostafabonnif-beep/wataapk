package com.elwataniatv.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elwataniatv.app.data.model.EpgItem
import com.elwataniatv.app.data.repository.WataniaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class EpgViewModel @Inject constructor(
    private val repository: WataniaRepository
) : ViewModel() {
    val epgList = repository.epgList
    val reminders = repository.reminders
    fun toggleReminder(item: EpgItem, isSet: Boolean) {
        viewModelScope.launch {
            if (isSet) repository.removeReminder(item.id) else repository.addReminder(item)
        }
    }
}
