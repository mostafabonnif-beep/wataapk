package com.elwataniatv.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.elwataniatv.app.R
import com.elwataniatv.app.data.model.RemoteStream
import com.elwataniatv.app.ui.theme.BrandAccent
import com.elwataniatv.app.ui.theme.BrandBg
import com.elwataniatv.app.ui.theme.BrandPanel
import com.elwataniatv.app.ui.theme.BrandPrimary
import com.elwataniatv.app.ui.theme.BrandRed

@Composable
fun PremiumLiveHero(
    streams: List<RemoteStream>,
    selectedStream: RemoteStream?,
    appName: String = "",
    appSlogan: String = "",
    logoUrl: String = "",
    currentProgramTitle: String? = null,
    backdropUrl: String? = null,
    onWatchLive: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenGuide: () -> Unit = {},
    onOpenArchive: () -> Unit = {},
    onOpenNews: () -> Unit = {}
) {
    val liveStatusDescription = stringResource(R.string.live_status)
    val watchLiveDescription = stringResource(R.string.watch_live_now)

    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "hero_live_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.3f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(750, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(750, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse_glow_alpha"
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isWideScreen = maxWidth >= 600.dp
        val horizontalGutter = if (isWideScreen) 24.dp else 14.dp
        val heroMinHeight = if (isWideScreen) 230.dp else 208.dp
        val heroMaxHeight = if (isWideScreen) 270.dp else 228.dp

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        // Hero Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalGutter)
                .heightIn(min = heroMinHeight, max = heroMaxHeight)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BrandPanel.copy(alpha = 0.95f),
                            BrandBg,
                            Color(0xFF040810)
                        ),
                        radius = 800f
                    )
                )
                .border(
                    width = 1.5.dp,
                    color = BrandAccent.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable(enabled = streams.isNotEmpty(), onClick = onWatchLive)
        ) {
            // Backdrop artwork / thumbnail if provided
            if (!backdropUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = backdropUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.28f)
                )
            } else if (logoUrl.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = logoUrl,
                    contentDescription = stringResource(R.string.official_logo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.12f),
                    error = {
                        Image(
                            painter = painterResource(R.drawable.watania_channel_logo),
                            contentDescription = stringResource(R.string.official_logo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().alpha(0.10f)
                        )
                    }
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.watania_channel_logo),
                    contentDescription = stringResource(R.string.official_logo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.10f)
                )
            }

            // Dark vignette gradient over backdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.75f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // Content Overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = if (isWideScreen) 22.dp else 16.dp,
                        vertical = if (isWideScreen) 16.dp else 14.dp
                    ),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: HD Tag on Start, Pulsing LIVE badge on End
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = BrandAccent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BrandAccent.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = stringResource(R.string.hd_quality_badge),
                            color = BrandAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Pulsing Red Live Badge
                    Surface(
                        color = BrandRed,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = pulseGlowAlpha)),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Text(
                                text = stringResource(R.string.live_status),
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Center / Bottom Texts (Right Aligned in RTL)
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                ) {
                    if (!currentProgramTitle.isNullOrBlank() && com.elwataniatv.app.util.ContentSanitizer.isUsable(currentProgramTitle)) {
                        Surface(
                            color = BrandPrimary.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, BrandAccent.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = currentProgramTitle,
                                color = BrandAccent,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.hero_live_quality_tagline),
                            modifier = Modifier.fillMaxWidth(),
                            color = BrandAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                    }

                    Text(
                        text = appName.ifBlank { stringResource(R.string.app_name) },
                        modifier = Modifier.fillMaxWidth(),
                        style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.Content),
                        color = Color.White,
                        fontSize = if (isWideScreen) 22.sp else 19.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = appSlogan.ifBlank { stringResource(R.string.official_live_tagline) },
                        modifier = Modifier.fillMaxWidth(),
                        style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.Content),
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = if (isWideScreen) 13.sp else 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }

                // Red Play Button
                Button(
                    onClick = onWatchLive,
                    enabled = streams.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isWideScreen) 50.dp else 46.dp)
                        .semantics { contentDescription = watchLiveDescription },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandRed,
                        contentColor = Color.White,
                        disabledContainerColor = BrandPanel,
                        disabledContentColor = Color.White.copy(alpha = 0.42f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(androidx.compose.material.icons.Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.watch_live_now), fontSize = 13.5.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        // Quick Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalGutter),
                horizontalArrangement = Arrangement.spacedBy(if (isWideScreen) 12.dp else 8.dp)
        ) {
            PremiumQuickAction(
                icon = Icons.AutoMirrored.Filled.Article,
                label = stringResource(R.string.news_bulletin_title),
                onClick = onOpenNews,
                testTagValue = "hero_quick_news",
                modifier = Modifier.weight(1f)
            )
            PremiumQuickAction(
                icon = Icons.Default.VideoLibrary,
                label = stringResource(R.string.archive),
                onClick = onOpenArchive,
                testTagValue = "hero_quick_archive",
                modifier = Modifier.weight(1f)
            )
            PremiumQuickAction(
                icon = Icons.Default.CalendarMonth,
                label = stringResource(R.string.program_guide),
                onClick = onOpenGuide,
                testTagValue = "hero_quick_guide",
                modifier = Modifier.weight(1f)
            )
        }
        }
    }
}

@Composable
private fun PremiumQuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    testTagValue: String,
    modifier: Modifier = Modifier
) {
    var isFocused by androidx.compose.runtime.remember { mutableStateOf(false) }
    val focusScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isFocused) 1.03f else 1f,
        animationSpec = androidx.compose.animation.core.tween(160),
        label = "quick_action_focus_scale"
    )

    Surface(
        modifier = modifier
            .heightIn(min = 76.dp)
            .graphicsLayer {
                scaleX = focusScale
                scaleY = focusScale
            }
            .testTag(testTagValue)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = label
                role = Role.Button
            },
        color = if (isFocused) BrandPrimary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isFocused) 1.5.dp else 1.dp,
            color = if (isFocused) BrandAccent else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
        ) {
            Icon(icon, contentDescription = null, tint = BrandAccent, modifier = Modifier.size(22.dp))
            Text(
                text = label,
                style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                lineHeight = 15.sp,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
