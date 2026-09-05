package com.elwataniatv.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.elwataniatv.app.ui.components.GlobalErrorBoundary
import com.elwataniatv.app.data.model.featuredArchivePreview
import com.elwataniatv.app.notifications.NewContentNotifier
import com.elwataniatv.app.ui.components.AppBottomBar
import com.elwataniatv.app.ui.components.AppTopBar
import com.elwataniatv.app.ui.components.AppNavHost
import com.elwataniatv.app.ui.components.PopupAlertDialog
import com.elwataniatv.app.ui.components.UpdateDialog
import com.elwataniatv.app.ui.navigation.Screen
import com.elwataniatv.app.ui.screens.*
import com.elwataniatv.app.ui.screens.archive.ArchiveScreen
import com.elwataniatv.app.ui.screens.archive.ALL_CATEGORY
import com.elwataniatv.app.ui.screens.settings.SettingsScreen
import com.elwataniatv.app.ui.theme.BrandAccent
import com.elwataniatv.app.ui.theme.BrandBg
import com.elwataniatv.app.ui.theme.BrandPanel
import com.elwataniatv.app.ui.theme.BrandPillBg
import com.elwataniatv.app.ui.theme.BrandPrimary
import com.elwataniatv.app.ui.theme.BrandRed
import com.elwataniatv.app.ui.theme.ElwataniaTVTheme
import com.elwataniatv.app.ui.theme.parseHexColor
import com.elwataniatv.app.util.applyAppLanguage
import com.elwataniatv.app.util.isAllowedHost
import com.elwataniatv.app.util.safeHttpUri
import com.elwataniatv.app.ui.viewmodel.MainViewModel
import com.elwataniatv.app.ui.viewmodel.LiveViewModel
import com.elwataniatv.app.ui.viewmodel.ArchiveViewModel
import com.elwataniatv.app.ui.viewmodel.EpgViewModel
import com.elwataniatv.app.ui.viewmodel.HistoryViewModel
import com.elwataniatv.app.ui.viewmodel.SettingsViewModel
import com.elwataniatv.app.ui.viewmodel.MoreViewModel
import com.elwataniatv.app.ui.viewmodel.NotificationsViewModel
import com.elwataniatv.app.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

private val youtubeHosts = setOf("youtube.com", "youtu.be", "youtube-nocookie.com")
private val notificationHosts = youtubeHosts + setOf(
    "elwataniatv.dz",
    "elwataniatvapp.web.app",
    "elwataniatvapp.firebaseapp.com"
)
private fun openYouTubeUrl(context: Context, rawUrl: String) {
    val uri = safeHttpUri(rawUrl) ?: return
    if (!isAllowedHost(uri, youtubeHosts)) return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

private fun openNewsUrl(context: Context, rawUrl: String) {
    val uri = safeHttpUri(rawUrl) ?: return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

private fun isVersionLessThan(current: String, required: String): Boolean {
    if (required.isBlank()) return false
    val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
    val requiredParts = required.split(".").map { it.toIntOrNull() ?: 0 }
    val size = maxOf(currentParts.size, requiredParts.size)
    for (index in 0 until size) {
        val currentPart = currentParts.getOrElse(index) { 0 }
        val requiredPart = requiredParts.getOrElse(index) { 0 }
        if (currentPart != requiredPart) return currentPart < requiredPart
    }
    return false
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val liveViewModel: LiveViewModel by viewModels()
    private val archiveViewModel: ArchiveViewModel by viewModels()
    private val epgViewModel: EpgViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val moreViewModel: MoreViewModel by viewModels()
    private val notificationsViewModel: NotificationsViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.elwataniatv.app.util.AppLanguage.wrapContext(newBase))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    // ── Picture-in-Picture (live stream) ─────────────────────────────
    // When the user leaves the app (home button / recents) while the live
    // stream is playing, the stream keeps playing in a small floating 16:9
    // window instead of stopping.
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        com.elwataniatv.app.util.PipController.enterIfLivePlaying(this)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        com.elwataniatv.app.util.PipController.onPipModeChanged(isInPictureInPictureMode)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    /**
     * Notification deep links: FcmMessageService packs target_screen,
     * stream_id and url into the notification intent — this is where they
     * are honored (navigate to the screen, select the stream, open a url).
     */
    private fun handleNotificationIntent(intent: Intent?) {
        val targetScreen = intent?.getStringExtra("target_screen")
        val streamId = intent?.getStringExtra("stream_id")
        val url = intent?.getStringExtra("url")
        if (targetScreen == null && streamId == null && url == null) return

        if (!url.isNullOrBlank()) {
            val safeUrl = safeHttpUri(url)
            if (safeUrl != null && isAllowedHost(safeUrl, notificationHosts)) {
                runCatching {
                    startActivity(Intent(Intent.ACTION_VIEW, safeUrl))
                }
            }
        }
        liveViewModel.selectStreamById(streamId)
        viewModel.openFromNotification(targetScreen)
    }

    /**
     * Home-screen icon shortcuts (long-press on the launcher icon):
     * jump straight to Live, Program Guide or Archive. Uses static-style
     * dynamic shortcuts so the deep links survive app updates.
     */
    private fun publishAppShortcuts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        runCatching {
            val shortcutManager = getSystemService(android.content.pm.ShortcutManager::class.java)
            val liveIntent = Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("target_screen", "live")
            }
            val guideIntent = Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("target_screen", "guide")
            }
            val archiveIntent = Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("target_screen", "archive")
            }
            val shortcuts = listOf(
                androidx.core.content.pm.ShortcutInfoCompat.Builder(this, "shortcut_live")
                    .setShortLabel(getString(R.string.tab_live))
                    .setLongLabel(getString(R.string.watch_live_now))
                    .setIcon(androidx.core.graphics.drawable.IconCompat.createWithResource(this, R.drawable.ic_shortcut_live))
                    .setIntent(liveIntent)
                    .build(),
                androidx.core.content.pm.ShortcutInfoCompat.Builder(this, "shortcut_guide")
                    .setShortLabel(getString(R.string.epg_today))
                    .setLongLabel(getString(R.string.program_guide))
                    .setIcon(androidx.core.graphics.drawable.IconCompat.createWithResource(this, R.drawable.ic_shortcut_guide))
                    .setIntent(guideIntent)
                    .build(),
                androidx.core.content.pm.ShortcutInfoCompat.Builder(this, "shortcut_archive")
                    .setShortLabel(getString(R.string.tab_archive))
                    .setLongLabel(getString(R.string.archive_title))
                    .setIcon(androidx.core.graphics.drawable.IconCompat.createWithResource(this, R.drawable.ic_shortcut_archive))
                    .setIntent(archiveIntent)
                    .build()
            )
            shortcutManager.dynamicShortcuts = shortcuts.map { it.toShortcutInfo() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        publishAppShortcuts()
        runCatching {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                // Debug builds use the App Check Debug provider so emulators and
                // unsigned builds keep working during development. Release builds
                // (the signed AAB) use Play Integrity. When App Check enforcement
                // is enabled in the console, register the debug token shown in
                // logcat (D DebugAppCheckProvider) under Console → App Check →
                // Apps → Debug tokens.
                if (BuildConfig.DEBUG) {
                    DebugAppCheckProviderFactory.getInstance()
                } else {
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                }
            )
        }
        enableEdgeToEdge()
        // Do not request POST_NOTIFICATIONS during cold start. The permission
        // controller is a system surface and must not cover the first screen.
        // In-app notifications remain available without this runtime prompt;
        // a later, user-initiated settings action can request it when needed.

        // Deep link from a tapped notification (app cold start)
        handleNotificationIntent(intent)

        setContent {
            val appConfig by viewModel.appConfig.collectAsState()
            val configPrimary = remember(appConfig.primaryColor) { parseHexColor(appConfig.primaryColor) }
            val configSecondary = remember(appConfig.secondaryColor) { parseHexColor(appConfig.secondaryColor) }
            val configAccent = remember(appConfig.accentColor) { parseHexColor(appConfig.accentColor) }
            ElwataniaTVTheme(
                darkTheme = appConfig.enableDarkMode,
                primaryColor = configPrimary,
                secondaryColor = configSecondary,
                accentColor = configAccent,
                dynamicColorEnabled = appConfig.enableDynamicColor
            ) {
                LaunchedEffect(Unit) {
                    withContext(Dispatchers.Default) {
                        viewModel.startFirebaseSync()
                    }
                }
                GlobalErrorBoundary(
                    onRetrySync = {
                        viewModel.startFirebaseSync()
                    }
                ) {
                    val onboardingDone by viewModel.onboardingCompleted.collectAsState()

                    if (appConfig.maintenanceMode) {
                        MaintenanceScreen(
                            message = appConfig.maintenanceMessage,
                            onRetry = { viewModel.startFirebaseSync() }
                        )
                    } else if (!onboardingDone && appConfig.enableOnboarding) {
                        OnboardingScreen(
                            onStart = { viewModel.completeOnboarding() },
                            appName = appConfig.appName.ifBlank { stringResource(R.string.app_name) },
                            appSlogan = appConfig.appSlogan.ifBlank { stringResource(R.string.onboarding_tagline) },
                            logoUrl = appConfig.logoUrl,
                            onboardingBannerUrl = appConfig.onboardingBannerUrl
                        )
                    } else {
                        MainAppShell(
                            viewModel = viewModel,
                            liveViewModel = liveViewModel,
                            archiveViewModel = archiveViewModel,
                            epgViewModel = epgViewModel,
                            historyViewModel = historyViewModel,
                            settingsViewModel = settingsViewModel,
                            moreViewModel = moreViewModel,
                            notificationsViewModel = notificationsViewModel,
                            onLanguageChange = { applyAppLanguage(this@MainActivity, it) },
                            onRequestNotificationPermission = { requestNotificationPermissionIfNeeded() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppShell(
    viewModel: MainViewModel,
    liveViewModel: LiveViewModel,
    archiveViewModel: ArchiveViewModel,
    epgViewModel: EpgViewModel,
    historyViewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel,
    moreViewModel: MoreViewModel,
    notificationsViewModel: NotificationsViewModel,
    onLanguageChange: (String) -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Notification deep-link navigation (set by MainActivity.onNewIntent)
    val pendingNav by viewModel.pendingNavigation.collectAsState()
    LaunchedEffect(pendingNav, navController) {
        pendingNav?.let { route ->
            navController.navigate(route) { launchSingleTop = true }
            viewModel.consumePendingNavigation()
        }
    }

    val streams by liveViewModel.streams.collectAsState()
    val selectedStream by liveViewModel.selectedStream.collectAsState()
    val breaking by liveViewModel.breaking.collectAsState()
    val epgList by epgViewModel.epgList.collectAsState()
    // val comments by liveViewModel.comments.collectAsState()
    val reminders by epgViewModel.reminders.collectAsState(initial = emptyList())

    val websites by moreViewModel.websites.collectAsState()
    val selectedWebsite by moreViewModel.selectedWebsite.collectAsState()

    val socialPages by moreViewModel.socialPages.collectAsState()

    val filteredArchive by archiveViewModel.filteredArchive.collectAsState()
    val rawArchive by archiveViewModel.archivePrograms.collectAsState()
    val favorites by archiveViewModel.favorites.collectAsState()
    val watchHistory by historyViewModel.watchHistory.collectAsState(initial = emptyList())
    val selectedCategory by archiveViewModel.selectedCategory.collectAsState()
    val searchQuery by archiveViewModel.searchQuery.collectAsState()

    val appConfig by settingsViewModel.appConfig.collectAsState()
    val inAppNotifications by notificationsViewModel.notifications.collectAsState()
    val unreadNotifications by notificationsViewModel.unreadCount.collectAsState()
    val syncError by liveViewModel.syncError.collectAsState(initial = null)
    val context = LocalContext.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // Captured at composable scope (lint-clean): labels used inside callbacks.
    val shareLiveAppName = stringResource(R.string.app_name)
    val shareLiveLabel = stringResource(R.string.share_live)
    val shareLiveTemplate = stringResource(R.string.share_live_text)
    val shareAppLabel = stringResource(R.string.share_app)
    val shareAppTemplate = stringResource(R.string.share_app_text)
    val forceUpdate = remember(appConfig.minVersion) {
        isVersionLessThan(BuildConfig.VERSION_NAME, appConfig.minVersion)
    }
    var updatePromptVisible by remember(appConfig.minVersion, appConfig.latestVersion) {
        mutableStateOf(true)
    }
    val optionalUpdate = appConfig.latestVersion.isNotBlank() &&
        isVersionLessThan(BuildConfig.VERSION_NAME, appConfig.latestVersion) &&
        !forceUpdate
    val popupAlert by viewModel.popupAlert.collectAsState()
    val streamHealthState by liveViewModel.streamHealthState.collectAsState()
    val adBanners by liveViewModel.adBanners.collectAsState()
    val newsItems by liveViewModel.newsItems.collectAsState()
    var knownArchiveIds by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(rawArchive) {
        knownArchiveIds = NewContentNotifier.notifyNewArchivePrograms(context, rawArchive, knownArchiveIds)
    }
    val adminSecurity by viewModel.adminSecurity.collectAsState()

    if (updatePromptVisible && (forceUpdate || optionalUpdate)) {
        UpdateDialog(
            forceUpdate = forceUpdate,
            message = appConfig.updateMessage.ifBlank {
                if (forceUpdate) stringResource(R.string.unsupported_version)
                else stringResource(R.string.new_version_message)
            },
            onConfirm = {
                val target = appConfig.updateUrl
                    .ifBlank { appConfig.appStoreUrl }
                    .ifBlank { appConfig.officialWebsite }
                safeHttpUri(target)?.let { safeTarget ->
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, safeTarget)) }
                    }
            },
            onDismiss = { updatePromptVisible = false }
        )
    }

    PopupAlertDialog(
        alert = popupAlert,
        onDismiss = { viewModel.dismissPopupAlert() }
    )

    val items = buildList {
        add(Screen.Live)
        if (appConfig.enableArchive) add(Screen.Archive)
        if (appConfig.enableSocial) add(Screen.Social)
        if (appConfig.enableWebsites) add(Screen.Websites)
        add(Screen.More)
    }

    Scaffold(
        // AppTopBar and AppBottomBar apply system-bar insets themselves.
        // Keeping Scaffold content insets empty prevents double top padding
        // and stops the first Arabic section title from sliding under the bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AppBottomBar(
                items = items,
                currentRoute = currentRoute,
                onNavigate = { screen ->
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        },
        containerColor = com.elwataniatv.app.ui.theme.BrandBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isSubScreen = currentRoute == Screen.Settings.route ||
                    currentRoute == Screen.Social.route ||
                    currentRoute == Screen.History.route ||
                    currentRoute == Screen.Favorites.route ||
                    currentRoute == Screen.Guide.route

            AppTopBar(
                appName = appConfig.appName.ifBlank { stringResource(R.string.app_name) },
                appSlogan = appConfig.appSlogan.ifBlank { stringResource(R.string.app_slogan) },
                logoUrl = appConfig.logoUrl,
                canNavigateBack = isSubScreen,
                onNavigateBack = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )

            AppNavHost(
                navController = navController,
                startDestination = Screen.Live.route,
                modifier = Modifier.weight(1f)
            ) {
                composable(Screen.Live.route) {
                LiveScreen(
                        streams = streams,
                        selectedStream = selectedStream,
                        appName = appConfig.appName.ifBlank { stringResource(R.string.app_name) },
                        appSlogan = appConfig.appSlogan.ifBlank { stringResource(R.string.app_slogan) },
                        logoUrl = appConfig.logoUrl,
                        breaking = breaking,
                    epgList = epgList,
                    archivePrograms = if (appConfig.showArchivePreview) featuredArchivePreview(rawArchive) else emptyList(),
                    reminders = reminders,
                    adBanners = if (appConfig.showPromotionalBanners) adBanners else emptyList(),
                    newsItems = newsItems,
                    enableEpg = appConfig.enableEpg,
                    inAppNotifications = inAppNotifications,
                    streamHealthState = streamHealthState,
                    syncError = syncError,
                    syncStatus = viewModel.syncStatus.collectAsState().value,
                    onSelectStream = { liveViewModel.selectStream(it) },
                    onToggleReminder = { item, isSet -> epgViewModel.toggleReminder(item, isSet) },
                    onOpenGuide = { navController.navigate(Screen.Guide.route) { launchSingleTop = true } },
                    onOpenArchive = { navController.navigate(Screen.Archive.route) { launchSingleTop = true } },
                    onOpenYouTube = { url -> openYouTubeUrl(context, url) },
                    onOpenNewsUrl = { url -> openNewsUrl(context, url) },
                    onShareLive = { stream ->
                        val streamTitle = stream.title.trim().ifBlank {
                            appConfig.appName.ifBlank { shareLiveAppName }
                        }
                        val shareText = shareLiveTemplate.format(streamTitle, stream.url)
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        runCatching {
                            context.startActivity(Intent.createChooser(shareIntent, shareLiveLabel))
                        }
                    },
                    onRetrySync = { viewModel.startFirebaseSync() }
                )
            }

            composable(Screen.Archive.route) {
                ArchiveScreen(
                    programs = filteredArchive,
                    favorites = favorites,
                    selectedCategory = selectedCategory,
                    searchQuery = searchQuery,
                    onSelectCategory = { archiveViewModel.selectCategory(it) },
                    onSearchQueryChange = { archiveViewModel.updateSearchQuery(it) },
                    onToggleFavorite = { program, isFav -> archiveViewModel.toggleFavorite(program, isFav) },
                    onSaveWatchProgress = { program, pos, dur -> archiveViewModel.saveWatchProgress(program, pos, dur) },
                    onRetrySync = { viewModel.startFirebaseSync() }
                )
            }

            composable(Screen.Favorites.route) {
                val favPrograms = rawArchive.filter { program -> favorites.any { it.id == program.id } }
                ArchiveScreen(
                    programs = favPrograms,
                    favorites = favorites,
                    selectedCategory = ALL_CATEGORY,
                    searchQuery = "",
                    onSelectCategory = { archiveViewModel.selectCategory(it) },
                    onSearchQueryChange = { archiveViewModel.updateSearchQuery(it) },
                    onToggleFavorite = { program, isFav -> archiveViewModel.toggleFavorite(program, isFav) },
                    onSaveWatchProgress = { program, pos, dur -> archiveViewModel.saveWatchProgress(program, pos, dur) },
                    onRetrySync = { viewModel.startFirebaseSync() }
                )
            }

            composable(Screen.More.route) {
                MoreScreen(
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToSocial = { navController.navigate(Screen.Social.route) },
                    onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onNavigateToGuide = { navController.navigate(Screen.Guide.route) { launchSingleTop = true } },
                    onOpenPrivacy = {
                        val privacyUrl = appConfig.privacyUrl.trim()
                        safeHttpUri(privacyUrl)?.let { safePrivacyUrl ->
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, safePrivacyUrl))
                            }
                        }
                    },
                    inAppNotifications = inAppNotifications,
                    unreadNotifications = unreadNotifications,
                    onMarkNotificationRead = notificationsViewModel::markRead,
                    onMarkAllNotificationsRead = notificationsViewModel::markAllRead,
                    onOpenNotification = { url -> openYouTubeUrl(context, url) },
                    onShareApp = {
                        val shareTarget = appConfig.officialWebsite
                            .ifBlank { appConfig.appStoreUrl }
                            .ifBlank { "https://elwataniatvapp.web.app/download" }
                        val text = shareAppTemplate.format(shareTarget)
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        runCatching {
                            context.startActivity(Intent.createChooser(shareIntent, shareAppLabel))
                        }
                    },
                    onRateApp = {
                        // Native Google Play in-app review sheet. No-op with a
                        // toast when Play services are unavailable (sideload).
                        val activity = context as? android.app.Activity
                        val reviewManager = com.google.android.play.core.review.ReviewManagerFactory.create(context)
                        reviewManager.requestReviewFlow()
                            .addOnCompleteListener { requestTask ->
                                if (requestTask.isSuccessful && activity != null) {
                                    val reviewInfo = requestTask.result
                                    reviewManager.launchReviewFlow(activity, reviewInfo)
                                        .addOnFailureListener {
                                            runCatching {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    R.string.rate_app_unavailable,
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                } else {
                                    runCatching {
                                        android.widget.Toast.makeText(
                                            context,
                                            R.string.rate_app_unavailable,
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                    },
                    appVersion = BuildConfig.VERSION_NAME
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    history = historyViewModel.watchHistory.collectAsState(initial = emptyList()).value,
                    onClear = { historyViewModel.clearHistory() },
                    onSavePosition = { item, pos -> historyViewModel.savePosition(item, pos) }
                )
            }

            composable(Screen.Websites.route) {
                WebsitesScreen(
                    websites = websites,
                    selectedWebsite = selectedWebsite,
                    onSelectWebsite = { moreViewModel.selectWebsite(it) },
                    onCloseWebsite = { moreViewModel.selectWebsite(null) },
                    onRetrySync = { viewModel.startFirebaseSync() }
                )
            }

            composable(Screen.Social.route) {
                SocialScreen(
                    socialPages = socialPages,
                    onRetrySync = { viewModel.startFirebaseSync() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    appConfig = appConfig,
                    satelliteFrequencies = settingsViewModel.satelliteFrequencies.collectAsState().value,
                    onUpdatePreferences = { darkMode, push ->
                        settingsViewModel.updatePreferences(darkMode, push)
                    },
                    onSubmitFeedback = { text, callback ->
                        settingsViewModel.submitFeedback("other", text, "", callback)
                    },
                    onLanguageChange = onLanguageChange,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    appVersion = BuildConfig.VERSION_NAME
                )
            }

            composable(Screen.Guide.route) {
                com.elwataniatv.app.ui.screens.guide.EpgGuideScreen(
                    epgList = epgList,
                    reminders = reminders,
                    onToggleReminder = { item, isSet -> epgViewModel.toggleReminder(item, isSet) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
}
