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
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isWideScreen = maxWidth >= 600.dp
        val horizontalGutter = if (isWideScreen) 24.dp else 14.dp
        val heroMinHeight = if (isWideScreen) 220.dp else 198.dp
        val heroMaxHeight = if (isWideScreen) 258.dp else 218.dp

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
                    width = 1.dp,
                    color = BrandAccent.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            // Faint brand mark. The remote logo is controlled from Firebase;
            // the bundled asset remains the offline fallback.
            if (logoUrl.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = logoUrl,
                    contentDescription = stringResource(R.string.official_logo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.07f),
                    error = {
                        Image(
                            painter = painterResource(R.drawable.watania_channel_logo),
                            contentDescription = stringResource(R.string.official_logo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().alpha(0.06f)
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
                        .alpha(0.06f)
                )
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = if (isWideScreen) 22.dp else 14.dp,
                        vertical = if (isWideScreen) 16.dp else 12.dp
                    ),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Right Badge
                Surface(
                    color = BrandRed,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color.White,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(7.dp)
                                .scale(pulseScale)
                        ) {}
                        Text(stringResource(R.string.live_status), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Texts (Right Aligned)
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.hero_live_quality_tagline),
                            modifier = Modifier.weight(1f),
                            color = BrandAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                        Surface(
                            color = BrandAccent.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.hd_quality_badge),
                                color = BrandAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = appName.ifBlank { stringResource(R.string.app_name) },
                        modifier = Modifier.fillMaxWidth(),
                        style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.ContentOrRtl),
                        color = Color.White,
                                                    fontSize = if (isWideScreen) 22.sp else 19.sp,

                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = appSlogan.ifBlank { stringResource(R.string.official_live_tagline) },
                        modifier = Modifier.fillMaxWidth(),
                        style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.ContentOrRtl),
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = if (isWideScreen) 13.sp else 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
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
                    Text(stringResource(R.string.watch_live_now), fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
