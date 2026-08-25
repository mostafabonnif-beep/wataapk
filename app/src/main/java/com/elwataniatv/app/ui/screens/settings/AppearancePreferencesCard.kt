package com.elwataniatv.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.elwataniatv.app.R
import com.elwataniatv.app.ui.theme.BrandAccent
import com.elwataniatv.app.ui.theme.BrandPanel

/** المظهر واللغة — dark mode / notifications toggles and language selector. */
@Composable
fun AppearancePreferencesCard(
    darkModeEnabled: Boolean,
    pushNotificationsEnabled: Boolean,
    selectedLanguage: String,
    onDarkModeChange: (Boolean) -> Unit,
    onPushChange: (Boolean) -> Unit,
    onUpdatePreferences: (Boolean, Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    breakingNotificationsEnabled: Boolean = true,
    programNotificationsEnabled: Boolean = true,
    streamNotificationsEnabled: Boolean = true,
    onNotificationCategoryChange: (String, Boolean) -> Unit = { _, _ -> },
    onRequestNotificationPermission: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BrandPanel),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.appearance_language),
                color = BrandAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            // Dark Theme Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.DarkMode, contentDescription = null, tint = Color.White)
                    Text(
                        text = stringResource(R.string.dark_mode_tv),
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                }
                Switch(
                    checked = darkModeEnabled,
                    onCheckedChange = {
                        onDarkModeChange(it)
                        onUpdatePreferences(it, pushNotificationsEnabled)
                    },
                    modifier = Modifier.testTag("dark_mode_switch")
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // Notifications Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = Color.White)
                    Text(
                        text = stringResource(R.string.all_notifications),
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        lineHeight = 20.sp
                    )
                }
                Switch(
                    checked = pushNotificationsEnabled,
                    onCheckedChange = {
                        if (it) onRequestNotificationPermission()
                        onPushChange(it)
                        onUpdatePreferences(darkModeEnabled, it)
                    },
                    modifier = Modifier.testTag("notifications_switch")
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            Text(
                text = stringResource(R.string.notification_categories),
                color = BrandAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
            NotificationCategorySwitchRow(
                icon = Icons.Default.Campaign,
                title = stringResource(R.string.notification_category_breaking),
                checked = breakingNotificationsEnabled,
                testTag = "notification_category_breaking",
                onCheckedChange = { onNotificationCategoryChange("breaking", it) }
            )
            NotificationCategorySwitchRow(
                icon = Icons.Default.Schedule,
                title = stringResource(R.string.notification_category_program),
                checked = programNotificationsEnabled,
                testTag = "notification_category_program",
                onCheckedChange = { onNotificationCategoryChange("program", it) }
            )
            NotificationCategorySwitchRow(
                icon = Icons.Default.LiveTv,
                title = stringResource(R.string.notification_category_stream),
                checked = streamNotificationsEnabled,
                testTag = "notification_category_stream",
                onCheckedChange = { onNotificationCategoryChange("stream", it) }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // Language Selector
            var languageMenuExpanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = Color.White)
                    Text(
                        text = stringResource(R.string.app_language),
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                }
                Box {
                    Text(
                        text = selectedLanguage,
                        color = BrandAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { languageMenuExpanded = true }
                            .testTag("language_selector")
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                    DropdownMenu(
                        expanded = languageMenuExpanded,
                        onDismissRequest = { languageMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            modifier = Modifier.testTag("language_option_ar"),
                            text = { Text(stringResource(R.string.language_arabic)) },
                            onClick = {
                                languageMenuExpanded = false
                                onLanguageChange("ar")
                            }
                        )
                        DropdownMenuItem(
                            modifier = Modifier.testTag("language_option_fr"),
                            text = { Text(stringResource(R.string.language_french)) },
                            onClick = {
                                languageMenuExpanded = false
                                onLanguageChange("fr")
                            }
                        )
                        DropdownMenuItem(
                            modifier = Modifier.testTag("language_option_en"),
                            text = { Text(stringResource(R.string.language_english)) },
                            onClick = {
                                languageMenuExpanded = false
                                onLanguageChange("en")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCategorySwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.85f))
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
