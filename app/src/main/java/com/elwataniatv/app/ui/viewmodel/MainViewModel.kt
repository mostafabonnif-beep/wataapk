package com.elwataniatv.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elwataniatv.app.data.repository.WataniaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: WataniaRepository,
    @ApplicationContext private val applicationContext: Context
) : ViewModel() {

    val appConfig = repository.appConfig
    val popupAlert = repository.popupAlert
    val adminSecurity = repository.adminSecurity
    val syncStatus = repository.syncStatus

    private val _pendingNavigation = MutableStateFlow<String?>(null)
    val pendingNavigation: StateFlow<String?> = _pendingNavigation.asStateFlow()

    private val onboardingPreferences = applicationContext.getSharedPreferences(
        "app_preferences",
        Context.MODE_PRIVATE
    )
    private val _onboardingCompleted = MutableStateFlow(
        onboardingPreferences.getBoolean("onboarding_completed", false)
    )
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    init {
        // Firebase listeners are started by the first composed frame in MainActivity.
        // Keeping network/bootstrap work out of the constructor prevents a cold-start
        // stall while the splash screen is still visible on slow devices.
        viewModelScope.launch(Dispatchers.IO) {
            repository.rescheduleAllReminders()
        }
        viewModelScope.launch {
            while (isActive) {
                delay(25_000)
                if (repository.streamHealthState.value.autoCheckEnabled) {
                    repository.runStreamHealthCheck()
                }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                repository.sendHeartbeat()
                delay(5 * 60_000L)
            }
        }
    }

    override fun onCleared() {
        // The repository is a Hilt singleton shared across ViewModels.
        // Never cancel its application-level work from a screen ViewModel:
        // rotation must not permanently disable health checks or heartbeat.
        super.onCleared()
    }

    fun startFirebaseSync() = repository.startFirebaseSync()

    fun openFromNotification(targetScreen: String?) {
        _pendingNavigation.value = when (targetScreen?.lowercase()) {
            "archive" -> "archive"
            "favorites" -> "favorites"
            "websites" -> "websites"
            "social" -> "social"
            "history" -> "history"
            "settings" -> "settings"
            "guide" -> "guide"
            else -> "live"
        }
    }

    fun consumePendingNavigation() {
        _pendingNavigation.value = null
    }

    fun completeOnboarding() {
        onboardingPreferences.edit().putBoolean("onboarding_completed", true).apply()
        _onboardingCompleted.value = true
    }

    fun updatePopupAlert(
        active: Boolean,
        title: String,
        message: String,
        buttonText: String = "",
        alertType: String = "info"
    ) = repository.updatePopupAlert(active, title, message, buttonText, alertType)

    fun dismissPopupAlert() = repository.dismissPopupAlert()

    fun updateMainStreamUrl(url: String) = repository.updateMainStreamUrl(url)

    fun addAdminLog(action: String) = repository.addAdminLog(action)

    fun signInAdmin(email: String, password: String, onResult: (Boolean, String?) -> Unit) =
        repository.signInAdmin(email, password, onResult)

    fun signOutAdmin() = repository.signOutAdmin()
}
