package com.elwataniatv.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.elwataniatv.app.R
import com.elwataniatv.app.ui.theme.BrandAccent
import com.elwataniatv.app.ui.theme.BrandBg
import com.elwataniatv.app.ui.theme.BrandPanel
import com.elwataniatv.app.ui.theme.BrandPrimary

@Composable
fun OnboardingScreen(
    onStart: () -> Unit,
    appName: String = "",
    appSlogan: String = "",
    logoUrl: String = "",
    onboardingBannerUrl: String = "",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BrandBg, BrandPanel, BrandBg)
                )
            )
            .padding(16.dp)
    ) {
        if (onboardingBannerUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = onboardingBannerUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(0.16f)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Logo & Slogan
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = BrandPrimary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                        if (logoUrl.isNotBlank()) {
                            SubcomposeAsyncImage(
                                model = logoUrl,
                                contentDescription = stringResource(R.string.onboarding_logo_description),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                                error = {
                                    Image(
                                        painter = painterResource(R.drawable.watania_channel_logo),
                                        contentDescription = stringResource(R.string.onboarding_logo_description),
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.watania_channel_logo),
                                contentDescription = stringResource(R.string.onboarding_logo_description),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Text(
                    text = appName.ifBlank { stringResource(R.string.app_name) },
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, textDirection = androidx.compose.ui.text.style.TextDirection.ContentOrRtl),
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Text(
                    text = appSlogan.ifBlank { stringResource(R.string.onboarding_tagline) },
                    style = MaterialTheme.typography.bodyLarge.copy(textDirection = androidx.compose.ui.text.style.TextDirection.ContentOrRtl),
                    color = BrandAccent,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // Highlights
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.LiveTv, contentDescription = null, tint = BrandAccent, modifier = Modifier.size(32.dp))
                    Column {
                        Text(text = stringResource(R.string.onboarding_live_title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = stringResource(R.string.onboarding_live_description), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.OndemandVideo, contentDescription = null, tint = BrandAccent, modifier = Modifier.size(32.dp))
                    Column {
                        Text(text = stringResource(R.string.onboarding_archive_title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = stringResource(R.string.onboarding_archive_description), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Newspaper, contentDescription = null, tint = BrandAccent, modifier = Modifier.size(32.dp))
                    Column {
                        Text(text = stringResource(R.string.onboarding_news_title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = stringResource(R.string.onboarding_news_description), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }

            // Action Button
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("onboarding_start_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.watch_live_now),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
