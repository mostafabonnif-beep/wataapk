package com.elwataniatv.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.elwataniatv.app.data.repository.WataniaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: WataniaRepository
) : ViewModel() {
    val appConfig = repository.appConfig
    val satelliteFrequencies = repository.satelliteFrequencies
    fun updatePreferences(darkModeEnabled: Boolean, pushEnabled: Boolean) =
        repository.updatePreferences(darkModeEnabled, pushEnabled)
    fun submitFeedback(
        type: String,
        message: String,
        email: String,
        onResult: (Boolean, String?) -> Unit
    ) = repository.submitFeedback(type, message, email, onResult)
}
