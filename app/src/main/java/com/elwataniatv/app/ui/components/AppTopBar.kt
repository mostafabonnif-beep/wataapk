package com.elwataniatv.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.elwataniatv.app.R
import com.elwataniatv.app.ui.theme.BrandAccent
import com.elwataniatv.app.ui.theme.BrandPanel

@Composable
fun AppTopBar(
    appName: String,
    appSlogan: String,
    logoUrl: String,
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val settingsDescription = stringResource(R.string.tab_settings)
    val backDescription = stringResource(R.string.back)

    Surface(
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        com.elwataniatv.app.ui.theme.BrandBg.copy(alpha = 0.95f),
                        Color.Transparent
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo & title: keep a flexible text column so Arabic never
            // collides with the action button on narrow screens.
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (logoUrl.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = logoUrl,
                        contentDescription = stringResource(R.string.official_logo),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, BrandAccent.copy(alpha = 0.4f), CircleShape),
                        contentScale = ContentScale.Crop,
                        error = {
                            Image(
                                painter = painterResource(R.drawable.watania_channel_logo),
                                contentDescription = stringResource(R.string.official_logo),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.watania_channel_logo),
                        contentDescription = stringResource(R.string.official_logo),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, BrandAccent.copy(alpha = 0.4f), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = appName.ifBlank { stringResource(R.string.app_name) },
                        modifier = Modifier.fillMaxWidth(),
                        style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        lineHeight = 21.sp,
                        letterSpacing = 0.1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = appSlogan.ifBlank { stringResource(R.string.app_slogan) },
                        modifier = Modifier.fillMaxWidth(),
                        style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl),
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                }
            }

            // Action Button (Back button or Settings button) (Left in RTL)
            if (canNavigateBack) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BrandPanel)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .clickable(onClick = onNavigateBack)
                        .semantics {
                            contentDescription = backDescription
                            role = Role.Button
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = backDescription,
                        tint = BrandAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BrandPanel)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .clickable(onClick = onSettingsClick)
                        .semantics {
                            contentDescription = settingsDescription
                            role = Role.Button
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = settingsDescription,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
