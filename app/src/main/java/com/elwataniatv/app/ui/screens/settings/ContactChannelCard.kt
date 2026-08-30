package com.elwataniatv.app.ui.screens.settings

import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.elwataniatv.app.R
import com.elwataniatv.app.data.model.RemoteAppConfig
import com.elwataniatv.app.ui.theme.BrandAccent
import com.elwataniatv.app.util.safeHttpUri
import com.elwataniatv.app.ui.theme.BrandPanel

/** التواصل مع الوطنية TV — website / privacy / email / feedback rows. */
@Composable
fun ContactChannelCard(
    appConfig: RemoteAppConfig,
    onShowFeedback: () -> Unit
) {
    val context = LocalContext.current
    val linkOpenError = stringResource(R.string.link_open_error)

    fun openExternalUrl(url: String) {
        val safeUri = safeHttpUri(url)
        if (safeUri == null) {
            Toast.makeText(context, linkOpenError, Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, safeUri))
        }.onFailure {
            Toast.makeText(context, linkOpenError, Toast.LENGTH_SHORT).show()
        }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = BrandPanel),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.contact_channel),
                color = BrandAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        openExternalUrl(appConfig.officialWebsite)
                    }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = Color.White)
                    Text(text = stringResource(R.string.official_website), color = Color.White, fontSize = 14.sp)
                }
                Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        openExternalUrl(appConfig.privacyUrl)
                    }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Policy, contentDescription = null, tint = Color.White)
                    Text(text = stringResource(R.string.privacy_policy), color = Color.White, fontSize = 14.sp)
                }
                Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (appConfig.contactEmail.isBlank()) {
                            Toast.makeText(context, linkOpenError, Toast.LENGTH_SHORT).show()
                        } else {
                            runCatching {
                                val email = appConfig.contactEmail.trim()
                                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                    throw IllegalArgumentException("invalid email")
                                }
                                context.startActivity(
                                    Intent(Intent.ACTION_SENDTO, "mailto:${android.net.Uri.encode(email)}".toUri())
                                )
                            }.onFailure {
                                Toast.makeText(context, linkOpenError, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color.White)
                    Text(text = stringResource(R.string.contact_email), color = Color.White, fontSize = 14.sp)
                }
                Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onShowFeedback)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Feedback, contentDescription = null, tint = Color.White)
                    Text(text = stringResource(R.string.send_feedback), color = Color.White, fontSize = 14.sp)
                }
                Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}
