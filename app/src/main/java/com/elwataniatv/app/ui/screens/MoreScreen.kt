package com.elwataniatv.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elwataniatv.app.R
import com.elwataniatv.app.data.remote.InAppNotification
import com.elwataniatv.app.ui.theme.BrandAccent
import com.elwataniatv.app.ui.theme.BrandPanel
import com.elwataniatv.app.ui.theme.BrandPrimary

@Composable
fun MoreScreen(
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit,
    onNavigateToSocial: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToGuide: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onShareApp: () -> Unit = {},
    onRateApp: () -> Unit = {},
    inAppNotifications: List<InAppNotification> = emptyList(),
    unreadNotifications: Int = 0,
    onMarkNotificationRead: (String) -> Unit = {},
    onMarkAllNotificationsRead: () -> Unit = {},
    onOpenNotification: (String) -> Unit = {},
    appVersion: String = ""
) {
    var showAboutDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(BrandPanel, Color(0xFF122A4C), Color(0xFF0A1C35))
                        )
                    )
                    .border(1.dp, BrandAccent.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.more_title), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    Text(
                        stringResource(R.string.more_subtitle),
                        color = Color.White.copy(alpha = 0.68f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        item {
            MoreOptionCard(
                title = stringResource(R.string.program_guide),
                subtitle = stringResource(R.string.epg_today),
                icon = Icons.Default.CalendarMonth,
                iconTint = Color(0xFF0E94C4),
                onClick = onNavigateToGuide
            )
        }

        item {
            MoreOptionCard(
                title = stringResource(R.string.more_history_title),
                subtitle = stringResource(R.string.more_history_subtitle),
                icon = Icons.Default.History,
                iconTint = BrandAccent,
                onClick = onNavigateToHistory
            )
        }

        item {
            MoreOptionCard(
                title = stringResource(R.string.more_favorites_title),
                subtitle = stringResource(R.string.more_favorites_subtitle),
                icon = Icons.Default.Favorite,
                iconTint = Color(0xFFEF4444),
                onClick = onNavigateToFavorites
            )
        }

        item {
            MoreOptionCard(
                title = stringResource(R.string.more_social_title),
                subtitle = stringResource(R.string.more_social_subtitle),
                icon = Icons.Default.People,
                iconTint = Color(0xFF1877F2),
                onClick = onNavigateToSocial
            )
        }

        item {
            MoreOptionCard(
                title = stringResource(R.string.share_app),
                subtitle = stringResource(R.string.share_app_subtitle),
                icon = Icons.Default.Share,
                iconTint = Color(0xFF16A34A),
                onClick = onShareApp
            )
        }

        item {
            MoreOptionCard(
                title = stringResource(R.string.rate_app),
                subtitle = stringResource(R.string.rate_app_subtitle),
                icon = Icons.Default.Star,
                iconTint = Color(0xFFF5B041),
                onClick = onRateApp
            )
        }

        item {
            MoreOptionCard(
                title = stringResource(R.string.more_notifications_title),
                subtitle = if (unreadNotifications > 0) {
                    pluralStringResource(R.plurals.more_notifications_unread_count, unreadNotifications, unreadNotifications)
                } else if (inAppNotifications.isEmpty()) {
                    stringResource(R.string.more_notifications_subtitle)
                } else {
                    pluralStringResource(R.plurals.more_notifications_count, inAppNotifications.size, inAppNotifications.size)
                },
                icon = Icons.Default.Notifications,
                iconTint = BrandAccent,
                onClick = { showNotificationsDialog = true }
            )
        }

        item {
            MoreOptionCard(
                title = stringResource(R.string.more_settings_title),
                subtitle = stringResource(R.string.more_settings_subtitle),
                icon = Icons.Default.Settings,
                iconTint = Color(0xFFFFB703),
                onClick = onNavigateToSettings
            )
        }

        item {
            MoreOptionCard(
                title = stringResource(R.string.more_privacy_title),
                subtitle = stringResource(R.string.more_privacy_subtitle),
                icon = Icons.Default.PrivacyTip,
                iconTint = Color(0xFF65A30D),
                onClick = onOpenPrivacy
            )
        }

        item {
            MoreOptionCard(
                title = stringResource(R.string.more_about_title),
                subtitle = if (appVersion.isNotBlank()) stringResource(R.string.more_version, appVersion) else stringResource(R.string.more_about_subtitle),
                icon = Icons.Default.Info,
                iconTint = BrandAccent,
                onClick = { showAboutDialog = true }
            )
        }
    }

    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = { Text(stringResource(R.string.more_notifications_title), color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                if (inAppNotifications.isEmpty()) {
                    Text(
                        text = stringResource(R.string.more_notifications_empty),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        inAppNotifications.take(5).forEach { notification ->
                            NotificationRow(
                                notification = notification,
                                onClick = {
                                    onMarkNotificationRead(notification.id)
                                    if (notification.youtubeUrl.isNotBlank()) onOpenNotification(notification.youtubeUrl)
                                    showNotificationsDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (unreadNotifications > 0) {
                        TextButton(onClick = onMarkAllNotificationsRead) {
                            Text(stringResource(R.string.more_notifications_mark_all_read), color = BrandAccent)
                        }
                    }
                    TextButton(onClick = { showNotificationsDialog = false }) {
                        Text(stringResource(R.string.close), color = BrandAccent)
                    }
                }
            },
            containerColor = BrandPanel
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.more_about_title), color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.more_about_title), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(
                        text = stringResource(R.string.more_about_body),
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.ContentOrRtl),
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.End
                    )
                    if (appVersion.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.more_version, appVersion),
                            color = BrandAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(R.string.more_copyright),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) { Text(stringResource(R.string.more_done)) }
            },
            dismissButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text(stringResource(R.string.close), color = BrandAccent) }
            },
            containerColor = BrandPanel
        )
    }
}

@Composable
private fun NotificationRow(
    notification: InAppNotification,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(enabled = notification.youtubeUrl.isNotBlank(), onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
            Text(
                text = notification.title,
                style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl),
                color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        if (notification.body.isNotBlank()) {
            Text(
                text = notification.body,
                style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl),
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 11.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (notification.youtubeUrl.isNotBlank()) {
            Text(
                text = stringResource(R.string.open_news_video),
                color = BrandAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MoreOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandPanel)
                    .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(16.dp))
            .heightIn(min = 68.dp)
            .clickable { onClick() }
            .semantics {
                contentDescription = title
                role = Role.Button
            }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(21.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleSmall.copy(textDirection = TextDirection.ContentOrRtl),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.ContentOrRtl),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}
