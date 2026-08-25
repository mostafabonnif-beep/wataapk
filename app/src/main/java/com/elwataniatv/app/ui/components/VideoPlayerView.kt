@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.elwataniatv.app.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.elwataniatv.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.elwataniatv.app.data.model.RemoteStream
import com.elwataniatv.app.data.model.nextFallbackStream
import com.elwataniatv.app.ui.theme.BrandAccent
import com.elwataniatv.app.ui.theme.BrandPanel
import com.elwataniatv.app.ui.theme.BrandPrimary

enum class VideoAspectRatio(val labelRes: Int, val mode: Int) {
    FIT(R.string.player_aspect_fit, AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL(R.string.player_aspect_fill, AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM(R.string.player_aspect_zoom, AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
}

private fun mediaItemForUrl(rawUrl: String, type: String): MediaItem {
    val normalized = rawUrl.trim()
    val isHls = type.equals("m3u8", ignoreCase = true) ||
        normalized.substringBefore('?').endsWith(".m3u8", ignoreCase = true)
    val builder = MediaItem.Builder().setUri(normalized)
    if (isHls) {
        builder
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(4_000)
                    .setMinPlaybackSpeed(0.98f)
                    .setMaxPlaybackSpeed(1.02f)
                    .build()
            )
    }
    return builder.build()
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun VideoPlayerView(
    url: String,
    modifier: Modifier = Modifier,
    type: String = "m3u8",
    title: String = "",
    initialPositionMs: Long = 0L,
    onPositionUpdate: (Long) -> Unit = {},
    onDurationUpdate: (Long) -> Unit = {},
    currentStreamId: String? = null,
    fallbackStreams: List<RemoteStream> = emptyList(),
    onFallbackStream: (RemoteStream) -> Unit = {}
) {
    val context = LocalContext.current
    val playerContext = remember(context, url) {
        context.applicationContext
    }
    var isFullscreen by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var aspectRatio by remember { mutableStateOf(VideoAspectRatio.FIT) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var currentQuality by remember { mutableStateOf("auto") }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showRatioMenu by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var autoRetryCount by remember(url) { mutableIntStateOf(0) }
    var retryJob by remember(url) { mutableStateOf<Job?>(null) }
    var fallbackAttempted by remember(url) { mutableStateOf(false) }
    val maxAutoRetries = 3
    val latestCurrentStreamId by rememberUpdatedState(currentStreamId)
    val latestFallbackStreams by rememberUpdatedState(fallbackStreams)
    val latestOnFallbackStream by rememberUpdatedState(onFallbackStream)
    val coroutineScope = rememberCoroutineScope()

    val isYouTube = type == "youtube" || type == "web" || url.contains("youtube.com") || url.contains("youtu.be") || url.contains("facebook.com") || url.contains("dailymotion.com")

    // Helper to format YouTube and web URLs
    fun getYouTubeEmbedUrl(inputUrl: String): String {
        val trimmed = inputUrl.trim()
        return when {
            trimmed.contains("watch?v=") -> {
                val videoId = trimmed.substringAfter("v=").substringBefore("&")
                "https://www.youtube.com/embed/$videoId?autoplay=1&modestbranding=1&rel=0&enablejsapi=1&playsinline=1"
            }
            trimmed.contains("youtu.be/") -> {
                val videoId = trimmed.substringAfter("youtu.be/").substringBefore("?")
                "https://www.youtube.com/embed/$videoId?autoplay=1&modestbranding=1&rel=0&enablejsapi=1&playsinline=1"
            }
            trimmed.contains("youtube.com/live/") -> {
                val videoId = trimmed.substringAfter("live/").substringBefore("?")
                "https://www.youtube.com/embed/$videoId?autoplay=1&modestbranding=1&rel=0&enablejsapi=1&playsinline=1"
            }
            trimmed.contains("youtube.com/embed/") -> trimmed
            trimmed.contains("youtube.com") -> trimmed
            else -> trimmed
        }
    }

    // Single ExoPlayer instance tied to context & url (persists during fullscreen toggle)
    val exoPlayer = remember(playerContext, url, type) {
        if (isYouTube) null else {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()

            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(25000)
                .setReadTimeoutMs(25000)
                .setDefaultRequestProperties(
                    mapOf(
                        "Accept" to "*/*",
                        "User-Agent" to "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36"
                    )
                )

            val mediaSourceFactory = DefaultMediaSourceFactory(playerContext)
                .setDataSourceFactory(httpDataSourceFactory)

            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    2000, // minBufferMs
                    30000, // maxBufferMs
                    1500, // bufferForPlaybackMs
                    2000  // bufferForPlaybackAfterRebufferMs
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            val renderersFactory = DefaultRenderersFactory(playerContext)
                .setEnableDecoderFallback(true)

            ExoPlayer.Builder(playerContext, renderersFactory)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build().apply {
                    setMediaItem(mediaItemForUrl(url, type))
                    prepare()
                    if (initialPositionMs > 0) {
                        seekTo(initialPositionMs)
                    }
                    playWhenReady = true
                }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(exoPlayer, lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // Keep playing when the activity enters Picture-in-Picture;
                    // pause only on a real background stop.
                    if (!com.elwataniatv.app.util.PipController.isInPip.value) {
                        exoPlayer?.pause()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (exoPlayer != null && isPlaying && !hasError) {
                        exoPlayer.play()
                    }
                }
                else -> {}
            }
        }
        lifecycle.addObserver(observer)

        if (exoPlayer != null) {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isBuffering = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_READY) {
                        hasError = false
                        errorMessage = ""
                        autoRetryCount = 0
                        retryJob?.cancel()
                        retryJob = null
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    val cause = error.cause
                    val is404 = cause is HttpDataSource.InvalidResponseCodeException && cause.responseCode == 404
                    val isNoNetwork = cause is java.net.UnknownHostException || 
                            cause is java.net.SocketTimeoutException || 
                            cause is java.net.ConnectException || 
                            error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                            error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT

                    if (error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                        exoPlayer.seekToDefaultPosition()
                        exoPlayer.prepare()
                        exoPlayer.play()
                    } else if (!is404 && autoRetryCount < maxAutoRetries) {
                        retryJob?.cancel()
                        autoRetryCount++
                        val retryNumber = autoRetryCount
                        isBuffering = true
                        hasError = false
                        errorMessage = context.getString(R.string.player_reconnecting, retryNumber, maxAutoRetries)
                        retryJob = coroutineScope.launch {
                            delay(1_500L * (1L shl (retryNumber - 1)))
                            exoPlayer.setMediaItem(mediaItemForUrl(url, type))
                            exoPlayer.seekToDefaultPosition()
                            exoPlayer.prepare()
                            exoPlayer.play()
                        }
                    } else {
                        val fallback = if (!fallbackAttempted) {
                            nextFallbackStream(latestCurrentStreamId, latestFallbackStreams)
                        } else {
                            null
                        }
                        if (fallback != null) {
                            // Switch only to an active Firestore-configured entry;
                            // never manufacture or rewrite a stream URL.
                            fallbackAttempted = true
                            hasError = false
                            isBuffering = true
                            errorMessage = context.getString(R.string.player_switching_fallback)
                            latestOnFallbackStream(fallback)
                        } else {
                            hasError = true
                            isBuffering = false
                            errorMessage = when {
                                isNoNetwork -> context.getString(R.string.player_no_network)
                                is404 -> context.getString(R.string.player_stream_not_found)
                                else -> context.getString(R.string.player_connection_failed)
                            }
                        }
                    }
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                    // Report live playback to the PIP controller so
                    // MainActivity can enter PIP when the user leaves.
                    com.elwataniatv.app.util.PipController.setLivePlaying(playing && !hasError)
                    if (playing && !hasError && !com.elwataniatv.app.util.PipController.isInPip.value) {
                        // Android 12+ smooth transition: hint the source rect.
                        val activity = context as? android.app.Activity
                        if (activity != null) {
                            val display = activity.window?.decorView ?: return
                            val rect = android.graphics.Rect()
                            display.getGlobalVisibleRect(rect)
                            com.elwataniatv.app.util.PipController.updateAutoEnter(activity, rect)
                        }
                    }
                }
            }
            exoPlayer.addListener(listener)

            onDispose {
                lifecycle.removeObserver(observer)
                retryJob?.cancel()
                exoPlayer.removeListener(listener)
                // Only clear the PIP flag when this player is the live one.
                if (com.elwataniatv.app.util.PipController.livePlaying) {
                    com.elwataniatv.app.util.PipController.setLivePlaying(false)
                }
                exoPlayer.release()
            }
        } else {
            onDispose {
                lifecycle.removeObserver(observer)
            }
        }
    }

    // Audio & Speed property bindings
    LaunchedEffect(isMuted, exoPlayer) {
        exoPlayer?.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(currentSpeed, exoPlayer) {
        exoPlayer?.playbackParameters = PlaybackParameters(currentSpeed)
    }

    // Real quality selection: maps the chosen label to ExoPlayer
    // track-selection limits (max video resolution). "تلقائي" resets to
    // the default (no limit). Only takes effect on adaptive streams that
    // expose multiple renditions — honest behavior, no fake switches.
    LaunchedEffect(currentQuality, exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        val current = player.trackSelectionParameters
        val updated = when (currentQuality) {
            "1080" -> current.buildUpon().setMaxVideoSize(1920, 1080).build()
            "720" -> current.buildUpon().setMaxVideoSize(1280, 720).build()
            "480" -> current.buildUpon().setMaxVideoSize(854, 480).build()
            "360" -> current.buildUpon().setMaxVideoSize(640, 360).build()
            else -> current.buildUpon().setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE).build()
        }
        player.trackSelectionParameters = updated
    }

    // Playback progress reporter: every 5s while playing, push the current
    // position + duration so screens can persist "continue watching" state.
    LaunchedEffect(exoPlayer, isYouTube, isPlaying) {
        if (exoPlayer == null || isYouTube) return@LaunchedEffect
        while (isPlaying) {
            kotlinx.coroutines.delay(5_000)
            val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
            val dur = exoPlayer.duration.coerceAtLeast(0L)
            onPositionUpdate(pos)
            onDurationUpdate(dur)
        }
    }

    val RenderPlayerContent: @Composable (isDialog: Boolean) -> Unit = { isDialog ->
        // While the activity is in Picture-in-Picture the floating window is
        // tiny: hide all in-app overlays (spinner, error UI, controls) and
        // show only the video surface.
        val inPip by com.elwataniatv.app.util.PipController.isInPip
        Box(
            // The caller owns the player's geometry. A hard-coded 230dp height
            // distorted 16:9 containers on large phones and clipped the stream.
            modifier = if (isDialog) Modifier.fillMaxSize() else modifier.fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (isYouTube) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                                allowFileAccess = false
                                allowContentAccess = false
                                mediaPlaybackRequiresUserGesture = false
                                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                userAgentString = "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                            }
                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                    val target = request?.url ?: return true
                                    val host = target.host?.lowercase()
                                    val isAllowedYouTubeHost = host == "youtube.com" || host?.endsWith(".youtube.com") == true || host == "youtube-nocookie.com" || host?.endsWith(".youtube-nocookie.com") == true
                                    return target.scheme != "https" || !isAllowedYouTubeHost
                                }
                            }
                        }
                    },
                    update = { webView ->
                        val embedUrl = getYouTubeEmbedUrl(url)
                        if (embedUrl.contains("youtube.com/embed/")) {
                            val html = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                    <style>
                                        body, html { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #000; overflow: hidden; display: flex; justify-content: center; align-items: center; }
                                        iframe { width: 100%; height: 100%; border: none; }
                                    </style>
                                </head>
                                <body>
                                    <iframe src="$embedUrl" 
                                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
                                            allowfullscreen>
                                    </iframe>
                                </body>
                                </html>
                            """.trimIndent()
                            webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                        } else {
                            if (webView.url != embedUrl) {
                                webView.loadUrl(embedUrl)
                            }
                        }
                    }
                )
            } else if (exoPlayer != null) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { _ ->
                        PlayerView(playerContext).apply {
                            player = exoPlayer
                            useController = !isLocked
                            resizeMode = aspectRatio.mode
                        }
                    },
                    update = { playerView ->
                        if (playerView.player != exoPlayer) {
                            playerView.player = exoPlayer
                        }
                        playerView.resizeMode = aspectRatio.mode
                        playerView.useController = !isLocked && !inPip
                    }
                )

                // Buffering Spinner
                if (isBuffering && !hasError && !inPip) {
                    CircularProgressIndicator(
                        color = BrandAccent,
                        modifier = Modifier.size(44.dp)
                    )
                }

                // Connection Error & Retry UI Overlay
                if (hasError && !inPip) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f))
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SignalCellularConnectedNoInternet0Bar,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = stringResource(R.string.player_connection_failed_title),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (errorMessage.isNotBlank()) errorMessage else stringResource(R.string.player_retry_hint),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                        Button(
                            onClick = {
                                retryJob?.cancel()
                                hasError = false
                                isBuffering = true
                                autoRetryCount = 0
                                retryJob = coroutineScope.launch {
                                    exoPlayer.setMediaItem(mediaItemForUrl(url, type))
                                    exoPlayer.seekToDefaultPosition()
                                    exoPlayer.prepare()
                                    exoPlayer.play()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.retry), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Native YouTube/WebView pages already provide their own controls.
            // Showing the ExoPlayer toolbar over them creates large overlapping
            // buttons and obscures the video on small Arabic screens. The whole
            // toolbar is also hidden inside Picture-in-Picture (tiny window).
            if (!isYouTube && !inPip) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Overlay: Live Badge & Quick Tools
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Live Red Badge
                    Surface(
                        color = Color.Red.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Text(
                                text = stringResource(R.string.live_status),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Top Right Quick Controls Toolbar
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Mute Button
                        IconButton(
                            onClick = { isMuted = !isMuted },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = stringResource(R.string.player_audio),
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Aspect Ratio Switcher Button
                        IconButton(
                            onClick = { showRatioMenu = true },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = stringResource(R.string.player_aspect_ratio),
                                tint = BrandAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Playback Speed Button
                        IconButton(
                            onClick = { showSpeedMenu = true },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = stringResource(R.string.player_speed),
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Fullscreen Mode Switcher
                        IconButton(
                            onClick = { isFullscreen = !isFullscreen },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .testTag("fullscreen_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = stringResource(R.string.player_fullscreen),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Bottom Overlay Info Bar (Quality & Lock State)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { showQualityMenu = true }
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.player_quality_badge,
                                    stringResource(
                                        when (currentQuality) {
                                            "1080" -> R.string.player_quality_1080
                                            "720" -> R.string.player_quality_720
                                            "480" -> R.string.player_quality_480
                                            "360" -> R.string.player_quality_360
                                            else -> R.string.player_quality_auto
                                        }
                                    )
                                ),
                                color = BrandAccent,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (currentSpeed != 1.0f) {
                            Surface(
                                color = BrandPrimary.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${currentSpeed}x",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Screen Controller Lock Toggle
                    IconButton(
                        onClick = { isLocked = !isLocked },
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                if (isLocked) Color.Red.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.6f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = stringResource(R.string.player_lock),
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    }
                }
            }

            // Playback Speed Dialog
            if (showSpeedMenu) {
                AlertDialog(
                    onDismissRequest = { showSpeedMenu = false },
                    title = { Text(stringResource(R.string.player_speed_title), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currentSpeed = speed
                                            showSpeedMenu = false
                                        }
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (speed == 1.0f) "عادية (1.0x)" else "${speed}x",
                                        color = if (currentSpeed == speed) BrandAccent else Color.White
                                    )
                                    if (currentSpeed == speed) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = BrandAccent)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSpeedMenu = false }) {
                            Text(stringResource(R.string.close), color = BrandAccent)
                        }
                    },
                    containerColor = BrandPanel
                )
            }

            // Quality Dialog
            if (showQualityMenu) {
                AlertDialog(
                    onDismissRequest = { showQualityMenu = false },
                    title = { Text(stringResource(R.string.player_quality_title), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            listOf(
                                "auto" to R.string.player_quality_auto,
                                "1080" to R.string.player_quality_1080,
                                "720" to R.string.player_quality_720,
                                "480" to R.string.player_quality_480,
                                "360" to R.string.player_quality_360
                            ).forEach { (q, labelRes) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currentQuality = q
                                            showQualityMenu = false
                                        }
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(labelRes),
                                        color = if (currentQuality == q) BrandAccent else Color.White
                                    )
                                    if (currentQuality == q) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = BrandAccent)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showQualityMenu = false }) {
                            Text(stringResource(R.string.close), color = BrandAccent)
                        }
                    },
                    containerColor = BrandPanel
                )
            }

            // Aspect Ratio Dialog
            if (showRatioMenu) {
                AlertDialog(
                    onDismissRequest = { showRatioMenu = false },
                    title = { Text(stringResource(R.string.player_aspect_title), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            VideoAspectRatio.entries.forEach { ratio ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            aspectRatio = ratio
                                            showRatioMenu = false
                                        }
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(ratio.labelRes),
                                        color = if (aspectRatio == ratio) BrandAccent else Color.White
                                    )
                                    if (aspectRatio == ratio) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = BrandAccent)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showRatioMenu = false }) {
                            Text(stringResource(R.string.close), color = BrandAccent)
                        }
                    },
                    containerColor = BrandPanel
                )
            }
        }
    }

    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                RenderPlayerContent(true)
            }
        }
    } else {
        RenderPlayerContent(false)
    }
}
