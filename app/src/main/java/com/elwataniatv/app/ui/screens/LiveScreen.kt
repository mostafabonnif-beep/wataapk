package com.elwataniatv.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.elwataniatv.app.R
import com.elwataniatv.app.data.local.ProgramReminder
import com.elwataniatv.app.data.model.AdBanner
import com.elwataniatv.app.data.model.ArchiveProgram
import com.elwataniatv.app.data.model.BreakingNews
import com.elwataniatv.app.data.model.EpgItem
import com.elwataniatv.app.data.model.RemoteStream
import com.elwataniatv.app.data.model.StreamHealthState
import com.elwataniatv.app.data.remote.SyncStatus
import com.elwataniatv.app.util.ContentSanitizer
import com.elwataniatv.app.util.safeHttpUri
import com.elwataniatv.app.ui.components.BreakingTicker
import com.elwataniatv.app.ui.components.PremiumLiveHero
import com.elwataniatv.app.ui.components.PremiumSectionHeader
import com.elwataniatv.app.ui.components.EpgStrip
import com.elwataniatv.app.ui.components.algeriaMinutesOfDay
import com.elwataniatv.app.ui.components.nextEpgItem
import com.elwataniatv.app.ui.components.VideoPlayerView
import com.elwataniatv.app.ui.theme.BrandAccent
import com.elwataniatv.app.ui.theme.BrandPanel
import com.elwataniatv.app.ui.theme.BrandPrimary
import com.elwataniatv.app.ui.theme.BrandRed
import com.elwataniatv.app.ui.theme.BrandBorder
import com.elwataniatv.app.ui.theme.BrandBg
import com.elwataniatv.app.ui.theme.BrandPillBg
import com.elwataniatv.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun LiveScreen(
    streams: List<RemoteStream>,
    selectedStream: RemoteStream?,
    appName: String = "",
    appSlogan: String = "",
    logoUrl: String = "",
    breaking: BreakingNews,
    epgList: List<EpgItem>,
    archivePrograms: List<ArchiveProgram> = emptyList(),
    reminders: List<ProgramReminder>,
    adBanners: List<AdBanner> = emptyList(),
    enableEpg: Boolean = true,
    inAppNotifications: List<com.elwataniatv.app.data.remote.InAppNotification> = emptyList(),
    streamHealthState: StreamHealthState? = null,
    syncError: String? = null,
    syncStatus: SyncStatus? = null,
    modifier: Modifier = Modifier,
    onSelectStream: (RemoteStream) -> Unit,
    onToggleReminder: (EpgItem, Boolean) -> Unit,
    onOpenGuide: () -> Unit = {},
    onOpenArchive: () -> Unit = {},
    onOpenYouTube: (String) -> Unit = {},
    onShareLive: (RemoteStream) -> Unit = {},
    onRetrySync: () -> Unit = {}
) {
    val context = LocalContext.current

    // Pulsing Live Dot Animation
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    var isRefreshing by remember { mutableStateOf(false) }
    var switchingStreamId by remember { mutableStateOf<String?>(null) }
    val streamRowState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(800L)
            isRefreshing = false
        }
    }
    LaunchedEffect(selectedStream?.id) {
        switchingStreamId = null
        val selectedIndex = streams.indexOfFirst { it.id == selectedStream?.id }
        if (selectedIndex >= 0) streamRowState.animateScrollToItem(selectedIndex)
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            onRetrySync()
        },
        state = pullToRefreshState,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        contentPadding = PaddingValues(top = 2.dp, bottom = 96.dp)
    ) {
        // Sync status: prefer the richer SyncStatusCard (connection state + last update
        // time + retry action) when available; fall back to the simple offline banner
        // if the view model hasn't produced a SyncStatus yet.
        if (syncStatus != null) {
            if (!syncStatus.isConnected) {
                item {
                    SyncStatusCard(
                        status = syncStatus,
                        onRetry = onRetrySync
                    )
                }
            }
        } else if (syncError != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF3A1C24))
                        .border(1.dp, BrandRed.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("!", color = BrandRed, fontWeight = FontWeight.Black)
                    Text(
                        text = stringResource(R.string.home_sync_offline),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Top Section: Either the Video Player OR the Hero/Summary
        item {
            if (selectedStream != null) {
                // ACTIVE PLAYER MODE: seamless channel handoff without showing internal health text
                val activeStream = selectedStream
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Keep exactly one Media3 player in the composition during a channel switch.
                    // AnimatedContent keeps the outgoing player alive during its exit animation,
                    // which can briefly run two ExoPlayer instances and cause a flash or stale frame.
                    key(activeStream.id) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(Color.Black)
                        ) {
                            VideoPlayerView(
                                url = activeStream.url,
                                type = activeStream.type,
                                currentStreamId = activeStream.id,
                                fallbackStreams = streams,
                                onFallbackStream = onSelectStream,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("live_video_player")
                            )
                        }
                    }

                    // Channel Stream Switcher: always RTL so ordering is stable across device locales.
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("channel_switcher")
                                .padding(top = 10.dp, bottom = 6.dp),
                            state = streamRowState,
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(streams, key = { it.id }) { item ->
                                val isSelected = item.id == selectedStream.id
                                val streamTitle = item.title.trim().ifBlank { stringResource(R.string.default_live_stream_title) }
                                val streamSelectionDescription = stringResource(R.string.select_stream, streamTitle)
                                Surface(
                                    modifier = Modifier
                                        .widthIn(min = 132.dp, max = 240.dp)
                                        .alpha(if (switchingStreamId != null && switchingStreamId != item.id) 0.72f else 1f)
                                        .clickable(enabled = switchingStreamId == null) {
                                            if (item.id != selectedStream.id) {
                                                switchingStreamId = item.id
                                                onSelectStream(item)
                                            }
                                        }
                                        .semantics {
                                            contentDescription = streamSelectionDescription
                                        },
                                    shape = RoundedCornerShape(50),
                                    color = if (isSelected) BrandPrimary else BrandPanel.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (isSelected) {
                                            Surface(
                                                color = Color.White,
                                                shape = CircleShape,
                                                modifier = Modifier.size(6.dp)
                                            ) {}
                                        }
                                        Text(
                                            text = streamTitle,
                                            style = LocalTextStyle.current.copy(textDirection = TextDirection.ContentOrRtl),
                                            color = if (isSelected) Color.White else TextSecondary,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Share the active live stream (respects RTL layout order).
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { onShareLive(activeStream) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.share_live),
                                tint = BrandAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.share_live),
                                color = BrandAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                // IDLE MODE: Show Hero and Summary
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PremiumLiveHero(
                        streams = streams,
                        selectedStream = selectedStream,
                        appName = appName,
                        appSlogan = appSlogan,
                        logoUrl = logoUrl,
                        onWatchLive = {
                            streams.firstOrNull()?.let(onSelectStream)
                        },
                        onOpenGuide = onOpenGuide,
                        onOpenArchive = onOpenArchive,
                        onOpenNews = {
                            val latestNewsUrl = archivePrograms.firstOrNull { it.youtubeUrl.isNotBlank() }?.youtubeUrl
                                ?: DEFAULT_YOUTUBE_CHANNEL_URL
                            onOpenYouTube(latestNewsUrl)
                        },
                        modifier = Modifier.testTag("premium_live_hero")
                    )
                }
            }
        }

        // Live stream health: only surfaced when a stream is actively selected and the
        // background health check has flagged a real issue (avoids noisy UI when everything
        // is working normally).
        if (selectedStream != null && streamHealthState != null && !streamHealthState.isLiveActive) {
            item {
                StreamHealthCard(
                    health = streamHealthState,
                    onRetry = onRetrySync
                )
            }
        }

        item {
            HomeNextProgramCard(
                nextProgram = nextEpgItem(epgList, algeriaMinutesOfDay()),
                logoUrl = logoUrl,
                onOpenGuide = onOpenGuide
            )
        }

        item {
            HomeLatestNewsSection(
                archivePrograms = archivePrograms,
                onOpenYouTube = onOpenYouTube
            )
        }

        // Player Section is now handled at the top
        // In-App Notifications from the admin panel
        val activeNotifications = inAppNotifications.filterNot { notification ->
            isPlaceholderContent(notification.title) || isPlaceholderContent(notification.body)
        }
        if (activeNotifications.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BrandPillBg)
                        .border(1.dp, BrandAccent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeNotifications.take(2).forEach { note ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = note.youtubeUrl.isNotBlank()) {
                                    if (note.youtubeUrl.isNotBlank()) onOpenYouTube(note.youtubeUrl)
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsNone,
                                contentDescription = null,
                                tint = BrandAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = note.title,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (note.body.isNotBlank()) {
                                    Text(
                                        text = note.body,
                                        color = Color.White.copy(alpha = 0.65f),
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (note.youtubeUrl.isNotBlank()) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.open_news_video),
                                    tint = BrandAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }



        // Breaking News Ticker with Animation
        if (breaking.enabled && breaking.text.isNotBlank()) {
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically()
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        BreakingTicker(
                            text = breaking.text,
                            onClick = {
                                val latestVideoUrl = breaking.youtubeUrl
                                    .takeIf { it.isNotBlank() }
                                    ?: archivePrograms
                                        .firstOrNull { it.youtubeUrl.isNotBlank() }
                                        ?.youtubeUrl
                                    ?: DEFAULT_YOUTUBE_CHANNEL_URL
                                onOpenYouTube(latestVideoUrl)
                            }
                        )
                    }
                }
            }
        }

        // Sponsor & Ad Banners Strip
        val activeBanners = adBanners.filter { it.isEnabled }
        if (activeBanners.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PremiumSectionHeader(
                        title = stringResource(R.string.live_sponsors),
                        subtitle = stringResource(R.string.official_sponsor_ad)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(activeBanners, key = { it.id }) { banner ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = BrandPanel),
                                shape = RoundedCornerShape(12.dp),
                                                                    modifier = Modifier
                                        .width(240.dp)
                                        .clickable {
                                        val targetUri = safeHttpUri(banner.targetUrl)
                                        if (targetUri != null) {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, targetUri))
                                            }
                                        }
                                    }

                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (banner.imageUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = banner.imageUrl,
                                            contentDescription = banner.title,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = banner.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(text = stringResource(R.string.official_sponsor_ad), color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // EPG Daily Program Strip
        if (enableEpg) {
            item {
                EpgStrip(
                    epgList = epgList,
                    reminders = reminders,
                    onToggleReminder = onToggleReminder
                )
            }
        }

        // Home Featured Archive Strip
        if (archivePrograms.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable { onOpenArchive() },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PremiumSectionHeader(
                            title = stringResource(R.string.archive_title),
                            subtitle = stringResource(R.string.archive_subtitle),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.tab_archive),
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(archivePrograms, key = { it.id }) { prog ->
                            HomeArchiveCard(
                                program = prog,
                                onClick = {
                                    if (prog.youtubeUrl.isNotBlank()) {
                                        onOpenYouTube(prog.youtubeUrl)
                                    } else {
                                        onOpenArchive()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Legal & Copyright Disclaimer Footer
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.official_broadcast_footer),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.broadcast_rights_reserved),
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun HomeNextProgramCard(
    nextProgram: EpgItem?,
    logoUrl: String = "",
    onOpenGuide: () -> Unit
) {
    if (nextProgram == null) return

    val programGuideDescription = stringResource(R.string.program_guide)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenGuide() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.epg_next_program),
                modifier = Modifier.weight(1f),
                style = LocalTextStyle.current.copy(textDirection = TextDirection.ContentOrRtl),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.program_guide),
                    color = BrandAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = BrandAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenGuide)
                .testTag("home_next_program")
                .semantics {
                    contentDescription = programGuideDescription
                },
            color = BrandPanel,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .size(width = 68.dp, height = 54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (logoUrl.isNotBlank()) {
                        AsyncImage(
                            model = logoUrl,
                            contentDescription = stringResource(R.string.official_logo),
                            modifier = Modifier.fillMaxSize().alpha(0.35f),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.watania_channel_logo),
                            contentDescription = stringResource(R.string.official_logo),
                            modifier = Modifier.fillMaxSize().alpha(0.35f),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = BrandAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Text info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                    text = "${stringResource(R.string.epg_today)} - ${nextProgram.startTime}",
                    style = LocalTextStyle.current.copy(textDirection = TextDirection.ContentOrRtl),
                    color = BrandAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = nextProgram.title
                            .takeIf { title -> title.isNotBlank() && title.any { character -> !character.isDigit() } }
                            ?: stringResource(R.string.epg_unknown_program),
                        style = LocalTextStyle.current.copy(textDirection = TextDirection.ContentOrRtl),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = nextProgram.description.ifBlank { stringResource(R.string.program_description_unavailable) },
                        style = LocalTextStyle.current.copy(textDirection = TextDirection.ContentOrRtl),
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Action Indicator
                Surface(
                    color = BrandAccent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = BrandAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncStatusCard(
    status: SyncStatus,
    onRetry: () -> Unit
) {
    val time = status.lastUpdatedAt?.let {
        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it))
    } ?: "—"
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        color = if (status.isConnected) BrandPanel.copy(alpha = 0.72f) else Color(0xFF3A1C24),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (status.isConnected) BrandBorder.copy(alpha = 0.45f) else BrandRed.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (status.isConnected) Color(0xFF22C55E) else BrandRed)
            )
            Text(
                text = stringResource(
                    if (status.isConnected) R.string.sync_status_connected else R.string.sync_status_offline
                ),
                modifier = Modifier.weight(1f),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.sync_status_last_update, time),
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 9.sp
            )
            TextButton(
                onClick = onRetry,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_stream_health_retry),
                    color = BrandAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StreamHealthCard(
    health: StreamHealthState,
    onRetry: () -> Unit
) {
    val isOnline = health.isLiveActive
    val statusColor = if (isOnline) Color(0xFF22C55E) else BrandRed
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        color = BrandPanel.copy(alpha = 0.75f),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(statusColor))
                Text(
                    text = stringResource(if (isOnline) R.string.home_stream_health_online else R.string.home_stream_health_unavailable),
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (health.hlsPingLatencyMs >= 0) {
                        stringResource(R.string.home_stream_health_latency, health.hlsPingLatencyMs)
                    } else {
                        health.lastCheckTimestamp
                    },
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 9.sp
                )
                TextButton(
                    onClick = onRetry,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_stream_health_retry),
                        color = BrandAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private const val DEFAULT_YOUTUBE_CHANNEL_URL = "https://www.youtube.com/@ElwataniaTV"

/** Rejects known placeholders and test records before they can reach any home card. */
private fun isPlaceholderContent(value: String): Boolean = !ContentSanitizer.isUsable(value)

@Composable
private fun HomeLatestNewsSection(
    archivePrograms: List<ArchiveProgram>,
    onOpenYouTube: (String) -> Unit
) {
    val playablePrograms = archivePrograms.filter { program ->
        program.youtubeUrl.isNotBlank() &&
            !isPlaceholderContent(program.title) &&
            !isPlaceholderContent(program.youtubeUrl)
    }
    val firstVideoUrl = playablePrograms.firstOrNull()?.youtubeUrl ?: DEFAULT_YOUTUBE_CHANNEL_URL
    val openNewsDescription = stringResource(R.string.open_news_video)
    val defaultNewsCategory = stringResource(R.string.home_news_category)
    val justNow = stringResource(R.string.home_news_just_now)
    val recent = stringResource(R.string.home_news_recent)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.news_bulletin_title),
                modifier = Modifier.weight(1f),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenYouTube(firstVideoUrl) }
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .semantics { contentDescription = openNewsDescription },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.news_bulletin_urgent),
                    color = BrandAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = BrandAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BrandPanel,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column {
                if (playablePrograms.isNotEmpty()) {
                    playablePrograms.take(2).forEachIndexed { index, program ->
                        NewsItemRow(
                            category = program.category.ifBlank { defaultNewsCategory },
                            title = program.title,
                            time = program.date.ifBlank {
                                if (index == 0) justNow else recent
                            },
                            thumbnailUrl = program.thumbnailUrl,
                            onClick = { onOpenYouTube(program.youtubeUrl) }
                        )
                        if (index == 0 && playablePrograms.size > 1) {
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.06f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_news_empty),
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.home_news_empty_hint),
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsItemRow(
    category: String,
    title: String,
    time: String,
    thumbnailUrl: String = "",
    onClick: () -> Unit
) {
    val openNewsDescription = stringResource(R.string.open_news_video)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = openNewsDescription }
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 50.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BrandBg),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.watania_channel_logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().alpha(0.35f),
                    contentScale = ContentScale.Crop
                )
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = BrandAccent.copy(alpha = 0.9f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = category, color = BrandAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = time, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
        }
    }
}

@Composable
fun HomeArchiveCard(
    program: com.elwataniatv.app.data.model.ArchiveProgram,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(260.dp)
            .height(160.dp)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (program.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = program.thumbnailUrl,
                    contentDescription = program.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BrandPanel),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = BrandAccent.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Premium Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // Category Badge
            Surface(
                color = BrandPrimary.copy(alpha = 0.95f),
                shape = RoundedCornerShape(bottomStart = 16.dp),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    text = program.category,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Details
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = program.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            blurRadius = 4f
                        )
                    )
                )
                if (program.date.isNotBlank()) {
                    Text(
                        text = program.date,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
