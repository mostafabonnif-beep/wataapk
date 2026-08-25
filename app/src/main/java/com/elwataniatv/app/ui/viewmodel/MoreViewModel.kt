package com.elwataniatv.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.elwataniatv.app.data.model.WebsiteItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.elwataniatv.app.data.repository.WataniaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val repository: WataniaRepository
) : ViewModel() {
    val appConfig = repository.appConfig
    val websites = repository.websites
    val socialPages = repository.socialPages

    private val _selectedWebsite = MutableStateFlow<WebsiteItem?>(null)
    val selectedWebsite = _selectedWebsite.asStateFlow()

    fun selectWebsite(website: WebsiteItem?) {
        _selectedWebsite.value = website
    }
}
