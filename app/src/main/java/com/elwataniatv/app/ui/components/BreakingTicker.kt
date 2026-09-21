package com.elwataniatv.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elwataniatv.app.R

/**
 * Professional broadcast-grade Breaking News ribbon for Elwatania TV.
 * Designed with authentic TV news lower-third aesthetics:
 * - RTL layout with prominent glowing "عاجل" broadcast badge.
 * - Dynamic radar pulse animation and broadcast flash emblem.
 * - High-contrast editorial typography.
 * - Illuminated interactive play trigger to open the linked video report.
 */
@Composable
fun BreakingTicker(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breaking_pulse")

    // Radar pulse ring animation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    // Border and badge glow shimmer
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Container gradient: rich studio midnight obsidian with fiery crimson undertone
    val containerBackground = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF260508),
            Color(0xFF180305),
            Color(0xFF0D0204)
        )
    )

    val borderBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFFF2A3A).copy(alpha = glowAlpha),
            Color(0xFFDC2626).copy(alpha = 0.6f),
            Color(0xFF7F1D1D).copy(alpha = 0.35f)
        )
    )

    val openReportDesc = stringResource(R.string.open_news_video)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .testTag("breaking_ticker")
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = Color(0xFFDC2626),
                    spotColor = Color(0xFFEF4444)
                )
                .clip(RoundedCornerShape(14.dp))
                .background(containerBackground)
                .border(1.2.dp, borderBrush, RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .semantics { contentDescription = "خبر عاجل: $text - $openReportDesc" },
            color = Color.Transparent
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // TV Studio right edge broadcast accent ribbon
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFFFF3B30),
                                    Color(0xFFDC2626),
                                    Color(0xFF991B1B)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Right Side: Broadcast Breaking News Flag (شارة عاجل التلفزيونية)
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(9.dp),
                                spotColor = Color(0xFFFF1744)
                            )
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFFFF2233),
                                        Color(0xFFDC1424),
                                        Color(0xFF99000D)
                                    )
                                ),
                                RoundedCornerShape(9.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.28f),
                                shape = RoundedCornerShape(9.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Animated Radar Beacon
                            Box(
                                modifier = Modifier.size(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Outer pulsating wave
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = pulseAlpha))
                                )
                                // Inner solid beacon
                                Box(
                                    modifier = Modifier
                                        .size(6.5.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }

                            // Bold Broadcast "عاجل" typography
                            Text(
                                text = stringResource(R.string.breaking_news),
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.3.sp,
                                maxLines = 1,
                                softWrap = false
                            )

                            // Alert Bolt Icon
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFFFEB3B),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // 2. Center: News Headline & Editorial Meta
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = text,
                            style = androidx.compose.ui.text.TextStyle(
                                textDirection = TextDirection.Rtl
                            ),
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            // Live status dot indicator
                            Box(
                                modifier = Modifier
                                    .size(5.5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )

                            Text(
                                text = stringResource(R.string.breaking_live_update),
                                color = Color(0xFFE2E8F0).copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // 3. Left Side: Illuminated Broadcast Play / Watch Action Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.16f),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = 1.dp,
                                color = Color(0xFFEF4444).copy(alpha = 0.45f),
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = openReportDesc,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
