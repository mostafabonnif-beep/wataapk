package com.elwataniatv.app.ui.screens.settings

import android.annotation.SuppressLint
import android.widget.Toast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.elwataniatv.app.R
import androidx.compose.ui.unit.dp
import com.elwataniatv.app.data.model.RemoteAppConfig
import com.elwataniatv.app.data.model.SatelliteFrequency
import com.elwataniatv.app.notifications.NotificationPreferencesStore

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun SettingsScreen(
    appConfig: RemoteAppConfig,
    satelliteFrequencies: List<SatelliteFrequency> = emptyList(),
    onUpdatePreferences: (Boolean, Boolean) -> Unit = { _, _ -> },
    onSubmitFeedback: (String, (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onLanguageChange: (String) -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    appVersion: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var darkModeEnabled by remember(appConfig.enableDarkMode) { mutableStateOf(appConfig.enableDarkMode) }
    var pushNotificationsEnabled by remember(appConfig.enablePush) {
        mutableStateOf(
            appConfig.enablePush && NotificationPreferencesStore.isGlobalEnabled(context)
        )
    }
    var breakingNotificationsEnabled by remember {
        mutableStateOf(NotificationPreferencesStore.isCategoryEnabled(context, NotificationPreferencesStore.CATEGORY_BREAKING))
    }
    var programNotificationsEnabled by remember {
        mutableStateOf(NotificationPreferencesStore.isCategoryEnabled(context, NotificationPreferencesStore.CATEGORY_PROGRAM))
    }
    var streamNotificationsEnabled by remember {
        mutableStateOf(NotificationPreferencesStore.isCategoryEnabled(context, NotificationPreferencesStore.CATEGORY_STREAM))
    }
    val configuration = LocalConfiguration.current
    val arabicLanguage = stringResource(R.string.language_arabic)
    val englishLanguage = stringResource(R.string.language_english)
    val selectedLanguage = remember(configuration.locales[0].language, arabicLanguage, englishLanguage) {
        when (configuration.locales[0].language) {
            "ar" -> arabicLanguage
            "en" -> englishLanguage
            else -> configuration.locales[0].displayLanguage
        }
    }

    var feedbackText by remember { mutableStateOf("") }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        item { SettingsHeaderItem() }
        // Appearance & Preferences Card
        item {
            AppearancePreferencesCard(
                darkModeEnabled = darkModeEnabled,
                pushNotificationsEnabled = pushNotificationsEnabled,
                selectedLanguage = selectedLanguage,
                onDarkModeChange = { darkModeEnabled = it },
                onPushChange = {
                    pushNotificationsEnabled = it
                    NotificationPreferencesStore.setGlobalEnabled(context, it)
                },
                onUpdatePreferences = onUpdatePreferences,
                onLanguageChange = onLanguageChange,
                breakingNotificationsEnabled = breakingNotificationsEnabled,
                programNotificationsEnabled = programNotificationsEnabled,
                streamNotificationsEnabled = streamNotificationsEnabled,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onNotificationCategoryChange = { category, enabled ->
                    NotificationPreferencesStore.setCategoryEnabled(context, category, enabled)
                    when (category) {
                        NotificationPreferencesStore.CATEGORY_BREAKING -> breakingNotificationsEnabled = enabled
                        NotificationPreferencesStore.CATEGORY_PROGRAM -> programNotificationsEnabled = enabled
                        NotificationPreferencesStore.CATEGORY_STREAM -> streamNotificationsEnabled = enabled
                    }
                }
            )
        }

        // Satellite Frequencies Card
        item { SatelliteFrequenciesCard(frequencies = satelliteFrequencies) }

        // Contact & Channel Card
        item { ContactChannelCard(appConfig = appConfig, onShowFeedback = { showFeedbackDialog = true }) }

        // Developer Credit Card (oussama_b)
        item { DeveloperCreditCard() }

        // Play Protect Guidance Card
        item { PlayProtectGuidanceCard() }
        // About & Version Info
        item {
            AboutVersionItem(
                appConfig = appConfig,
                appVersion = appVersion,
                onSecretTap = {}
            )
        }
    }

    // Feedback Dialog
    if (showFeedbackDialog) {
        FeedbackDialog(
            feedbackText = feedbackText,
            onFeedbackTextChange = { feedbackText = it },
            onDismiss = { showFeedbackDialog = false },
            onSubmit = { textToSend ->
                onSubmitFeedback(textToSend) { success, error ->
                    if (success) {
                        Toast.makeText(context, context.getString(R.string.feedback_success), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.feedback_failure, error ?: context.getString(R.string.connection_error)), Toast.LENGTH_LONG).show()
                    }
                }
                feedbackText = ""
                showFeedbackDialog = false
            }
        )
    }

}
