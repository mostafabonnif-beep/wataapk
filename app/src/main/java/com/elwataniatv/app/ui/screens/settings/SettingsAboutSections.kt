package com.elwataniatv.app.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.elwataniatv.app.R
import com.elwataniatv.app.data.model.RemoteAppConfig
import com.elwataniatv.app.ui.theme.BrandAccent
import com.elwataniatv.app.ui.theme.BrandPanel
import com.elwataniatv.app.ui.theme.BrandPillBg
import com.elwataniatv.app.ui.theme.BrandPrimary

/** Developer credit card (oussama_b). */
@Composable
fun DeveloperCreditCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = BrandPanel),
        border = BorderStroke(1.dp, BrandAccent.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = BrandAccent,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.developer_label),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = stringResource(R.string.developer_team_name),
                            color = BrandAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(R.string.developer_description),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/** Play Protect guidance card shown when distributing the APK outside Play Store. */
@Composable
fun PlayProtectGuidanceCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = BrandPanel.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, BrandAccent.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    modifier = Modifier.size(18.dp),
                    contentDescription = null,
                    tint = BrandAccent
                )
                Text(
                    text = stringResource(R.string.play_protect_title),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = stringResource(R.string.play_protect_message),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }
}

/**
 * About & version info row. Tapping it repeatedly unlocks the hidden admin
 * gate — the tap counting logic lives in [SettingsScreen] and is wired via
 * [onSecretTap].
 */
@Composable
fun AboutVersionItem(
    appConfig: RemoteAppConfig,
    appVersion: String,
    onSecretTap: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSecretTap)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val displayName = appConfig.appName.ifBlank { stringResource(R.string.app_name) }
        val displaySlogan = appConfig.appSlogan.ifBlank { stringResource(R.string.app_slogan) }
        Text(
            text = "$displayName — $displaySlogan",
            style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.ContentOrRtl),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.official_version, appVersion.ifBlank { stringResource(R.string.unknown_version) }),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
        Text(
            text = stringResource(R.string.rights_reserved),
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 11.sp
        )
    }
}
