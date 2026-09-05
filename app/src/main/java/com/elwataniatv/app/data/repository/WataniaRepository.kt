package com.elwataniatv.app.data.repository

import com.elwataniatv.app.data.local.*
import com.elwataniatv.app.data.model.*
import com.elwataniatv.app.data.remote.FirebaseSync
import com.elwataniatv.app.data.remote.InAppNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import java.util.UUID
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WataniaRepository(private val db: AppDatabase, private val appContext: android.content.Context? = null) {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var streamHealthJob: Job? = null

    // التهيئة بالبيانات الحقيقية الافتراضية لقناة الوطنية TV
    private val _streams = MutableStateFlow<List<RemoteStream>>(DEFAULT_STREAMS)
    val streams: StateFlow<List<RemoteStream>> = _streams.asStateFlow()

    private val _breaking = MutableStateFlow(DEFAULT_BREAKING)
    val breaking: StateFlow<BreakingNews> = _breaking.asStateFlow()

    private val _appConfig = MutableStateFlow(DEFAULT_APP_CONFIG)
    val appConfig: StateFlow<RemoteAppConfig> = _appConfig.asStateFlow()

    private val _epgList = MutableStateFlow<List<EpgItem>>(DEFAULT_EPG)
    val epgList: StateFlow<List<EpgItem>> = _epgList.asStateFlow()

    private val _satelliteFrequencies = MutableStateFlow(DEFAULT_SATELLITE_FREQUENCIES)
    val satelliteFrequencies: StateFlow<List<SatelliteFrequency>> = _satelliteFrequencies.asStateFlow()

    private val _websites = MutableStateFlow<List<WebsiteItem>>(DEFAULT_WEBSITES)
    val websites: StateFlow<List<WebsiteItem>> = _websites.asStateFlow()

    private val _socialPages = MutableStateFlow<List<SocialPage>>(DEFAULT_SOCIAL)
    val socialPages: StateFlow<List<SocialPage>> = _socialPages.asStateFlow()

    private val _archivePrograms = MutableStateFlow<List<ArchiveProgram>>(DEFAULT_ARCHIVE)
    val archivePrograms: StateFlow<List<ArchiveProgram>> = _archivePrograms.asStateFlow()

    // In-App Popup Alert State
    private val _popupAlert = MutableStateFlow(
        PopupAlert(active = false, title = "", message = "", buttonText = "", alertType = "info")
    )
    val popupAlert: StateFlow<PopupAlert> = _popupAlert.asStateFlow()

    // Live viewer reactions (persisted in Firestore — real counters)
    private val _liveReactions = MutableStateFlow<Map<String, Int>>(emptyMap())
    val liveReactions: StateFlow<Map<String, Int>> = _liveReactions.asStateFlow()

    private val _myReaction = MutableStateFlow<String?>(null)
    val myReaction: StateFlow<String?> = _myReaction.asStateFlow()

    private val _inAppNotifications = MutableStateFlow<List<InAppNotification>>(emptyList())
    val inAppNotifications: StateFlow<List<InAppNotification>> = _inAppNotifications.asStateFlow()

    // Stream Health State & HLS / YouTube Live Monitor
    private val _streamHealthState = MutableStateFlow(StreamHealthState(isLiveActive = false, statusMessage = "جاري فحص حالة البث..."))
    val streamHealthState: StateFlow<StreamHealthState> = _streamHealthState.asStateFlow()

    // Banners & Promotional Campaign Manager
    private val _adBanners = MutableStateFlow<List<AdBanner>>(DEFAULT_AD_BANNERS)
    val adBanners: StateFlow<List<AdBanner>> = _adBanners.asStateFlow()

    init {
        // Direct Firebase synchronization enabled
    }

    // Admin Security & Operational Logs
    private val _adminSecurity = MutableStateFlow(AdminSecurityConfig())
    val adminSecurity: StateFlow<AdminSecurityConfig> = _adminSecurity.asStateFlow()

    fun addAdminLog(action: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val entry = "[$time] $action"
        val updated = listOf(entry) + _adminSecurity.value.logs
        _adminSecurity.value = _adminSecurity.value.copy(logs = updated.take(30))
    }

    fun addAdBanner(title: String, imageUrl: String, targetUrl: String) {
        val newBanner = AdBanner(
            id = "ad_${System.currentTimeMillis()}",
            title = title,
            imageUrl = imageUrl.ifBlank { "" },
            targetUrl = targetUrl.ifBlank { "" },
            isEnabled = true
        )
        _adBanners.value = _adBanners.value + newBanner
        firebaseSync.addAdBannerRemote(newBanner) { success, error ->
            if (!success) addAdminLog("فشل حفظ الإعلان: ${error ?: "خطأ غير معروف"}")
        }
        addAdminLog("إضافة إعلان/بنر جديد: $title 📢")
    }

    fun deleteAdBanner(id: String) {
        _adBanners.value = _adBanners.value.filter { it.id != id }
        firebaseSync.deleteAdBannerRemote(id) { success, error ->
            if (!success) addAdminLog("فشل حذف الإعلان: ${error ?: "خطأ غير معروف"}")
        }
        addAdminLog("حذف بنر إعلاني 🗑️")
    }

    fun toggleAdBanner(id: String) {
        val banner = _adBanners.value.firstOrNull { it.id == id } ?: return
        val enabled = !banner.isEnabled
        _adBanners.value = _adBanners.value.map {
            if (it.id == id) it.copy(isEnabled = enabled) else it
        }
        firebaseSync.toggleAdBannerRemote(id, enabled) { success, error ->
            if (!success) addAdminLog("فشل تحديث الإعلان: ${error ?: "خطأ غير معروف"}")
        }
    }


    fun runStreamHealthCheck() {
        val activeStreams = _streams.value.filter { it.isActive && it.url.isNotBlank() }
        if (activeStreams.isEmpty()) {
            _streamHealthState.value = StreamHealthState(
                isLiveActive = false,
                isYoutubeLiveActive = false,
                lastCheckTimestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                statusMessage = "لا توجد قناة بث مفعّلة للفحص",
                autoCheckEnabled = _streamHealthState.value.autoCheckEnabled
            )
            return
        }

        // Probe the real endpoints on a background thread — this is a real
        // reachability + latency check, not a config guess. Cancel a previous
        // slow probe so an old result cannot overwrite a newer health state.
        streamHealthJob?.cancel()
        streamHealthJob = repositoryScope.launch(Dispatchers.IO) {
            val results = activeStreams.map { stream ->
                async { stream.id to probeStream(stream.url) }
            }.awaitAll()

            val alive = results.filter { it.second != null }
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            val hasYoutubeStream = activeStreams.any { it.type.equals("youtube", ignoreCase = true) }
            val bestLatency = alive.minOfOrNull { it.second!! }

            val message = if (alive.isEmpty()) {
                "جميع قنوات البث (${activeStreams.size}) لا تستجيب حالياً — تحقق من رابط البث أو السيرفر."
            } else {
                "${alive.size} من ${activeStreams.size} قنوات تستجيب — أفضل استجابة $bestLatency مللي ثانية."
            }

            _streamHealthState.value = StreamHealthState(
                isLiveActive = alive.isNotEmpty(),
                isYoutubeLiveActive = hasYoutubeStream,
                hlsPingLatencyMs = bestLatency ?: -1,
                lastCheckTimestamp = timestamp,
                statusMessage = message,
                autoCheckEnabled = _streamHealthState.value.autoCheckEnabled
            )

            results.forEach { (streamId, latency) ->
                if (latency == null) addAdminLog("فشل فحص البث: $streamId")
            }
        }
    }

    /**
     * Real probe: opens the stream URL, reads a small chunk and measures
     * latency. Returns null when the endpoint is unreachable.
     */
    private fun probeStream(url: String): Long? {
        return try {
            val connection = (java.net.URI(url).toURL().openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36")
                requestMethod = "GET"
            }
            val start = System.currentTimeMillis()
            connection.connect()
            val code = connection.responseCode
            if (code in 200..399) {
                connection.inputStream.use { input ->
                    val buf = ByteArray(256)
                    if (input.read(buf) > 0) System.currentTimeMillis() - start else null
                }
            } else {
                null
            }.also { connection.disconnect() }
        } catch (e: Exception) {
            null
        }
    }

    fun toggleAutoStreamCheck(enabled: Boolean) {
        _streamHealthState.value = _streamHealthState.value.copy(autoCheckEnabled = enabled)
    }

    // Administrative Remote Control API Methods
    fun updatePopupAlert(active: Boolean, title: String, message: String, buttonText: String = "", alertType: String = "info") {
        val alert = PopupAlert(active, title, message, buttonText, alertType)
        _popupAlert.value = alert
        firebaseSync.updatePopupAlertRemote(alert) { success, error ->
            if (!success) addAdminLog("فشل تحديث التنبيه المنبثق: ${error ?: "خطأ غير معروف"}")
        }
    }

    fun dismissPopupAlert() {
        // Local-only dismiss: the alert is controlled by the admin panel and
        // config/popup is admin-write-only in the rules. A non-admin user must
        // NOT be able to disable the alert for everyone, and we must not
        // trigger a failing remote write + noisy admin log on every dismiss.
        _popupAlert.value = _popupAlert.value.copy(active = false)
    }

    fun updateMainStreamUrl(newUrl: String) {
        val normalizedUrl = newUrl.trim()
        val isHttp = normalizedUrl.startsWith("https://", ignoreCase = true) || normalizedUrl.startsWith("http://", ignoreCase = true)
        val isHls = normalizedUrl.contains(".m3u8", ignoreCase = true)
        if (!isHttp || !isHls) {
            addAdminLog("تم رفض رابط بث رئيسي غير صالح")
            return
        }
        val current = _streams.value.toMutableList()
        val index = current.indexOfFirst { it.id == "live_main" }
        if (index != -1) {
            val updated = current[index].copy(url = normalizedUrl)
            current[index] = updated
            _streams.value = current
            firebaseSync.updateStreamChannelRemote(updated) { success, error ->
                if (!success) addAdminLog("فشل تحديث رابط البث: ${error ?: "خطأ غير معروف"}")
            }
        }
    }

    fun addStreamChannel(title: String, url: String, type: String = "m3u8", logoUrl: String = "") {
        val normalizedUrl = url.trim()
        val isHttp = normalizedUrl.startsWith("https://", ignoreCase = true) || normalizedUrl.startsWith("http://", ignoreCase = true)
        val isYoutube = normalizedUrl.contains("youtube.com", ignoreCase = true) || normalizedUrl.contains("youtu.be", ignoreCase = true)
        val isHls = normalizedUrl.contains(".m3u8", ignoreCase = true)
        if (title.isBlank() || !isHttp || (!isYoutube && !isHls)) {
            addAdminLog("تم رفض إضافة قناة بث ببيانات غير صالحة")
            return
        }
        val newItem = RemoteStream(
            id = "stream_${System.currentTimeMillis()}",
            title = title.trim(),
            url = normalizedUrl,
            type = if (isYoutube) "youtube" else "m3u8",
            logoUrl = logoUrl.trim(),
            isActive = true,
            order = _streams.value.size
        )
        _streams.value = _streams.value + newItem
        firebaseSync.addStreamChannelRemote(newItem) { success, error ->
            if (!success) addAdminLog("فشل حفظ قناة البث: ${error ?: "خطأ غير معروف"}")
        }
    }

    fun deleteStreamChannel(id: String) {
        _streams.value = _streams.value.filter { it.id != id }
        firebaseSync.deleteStreamChannelRemote(id) { success, error ->
            if (!success) addAdminLog("فشل حذف قناة البث: ${error ?: "خطأ غير معروف"}")
        }
    }

    fun updateAppLogo(logoUrl: String, slogan: String = "") {
        val updated = _appConfig.value.copy(
            logoUrl = logoUrl,
            appSlogan = slogan.ifBlank { _appConfig.value.appSlogan }
        )
        _appConfig.value = updated
        firebaseSync.updateAppConfigRemote(updated) { success, error ->
            if (!success) addAdminLog("فشل حفظ شعار التطبيق: ${error ?: "خطأ غير معروف"}")
        }
    }

    fun updateBreakingNews(enabled: Boolean, text: String, youtubeUrl: String = "") {
        _breaking.value = BreakingNews(enabled = enabled, text = text, youtubeUrl = youtubeUrl.trim())
        firebaseSync.updateBreakingNewsRemote(enabled, text, youtubeUrl) { success, error ->
            if (!success) addAdminLog("فشل حفظ الخبر العاجل: ${error ?: "خطأ غير معروف"}")
        }
    }

    fun updateAppConfig(maintenance: Boolean, announcement: String) {
        val updated = _appConfig.value.copy(
            maintenanceMode = maintenance,
            maintenanceMessage = announcement
        )
        _appConfig.value = updated
        firebaseSync.updateAppConfigRemote(updated) { success, error ->
            if (!success) addAdminLog("فشل حفظ وضع الصيانة: ${error ?: "خطأ غير معروف"}")
        }
    }
    fun updatePreferences(darkModeEnabled: Boolean, pushEnabled: Boolean) {
        // User preferences must never overwrite the global config/app document.
        // FirebaseSync persists them under users/{uid}/preferences/app and overlays
        // them on the public defaults for this authenticated user only.
        val updated = _appConfig.value.copy(
            enableDarkMode = darkModeEnabled,
            enablePush = pushEnabled
        )
        _appConfig.value = updated
        firebaseSync.updateUserPreferences(darkModeEnabled, pushEnabled) { success, error ->
            if (!success) addAdminLog("فشل حفظ تفضيلات المستخدم: ${error ?: "خطأ غير معروف"}")
        }
    }

    fun addSocialPage(platform: String, name: String, url: String, description: String, emoji: String, color: String) {
        val newPage = SocialPage(
            id = "soc_${System.currentTimeMillis()}",
            platform = platform,
            name = name,
            url = url,
            description = description,
            emoji = emoji,
            color = color,
            order = _socialPages.value.size,
            isActive = true
        )
        _socialPages.value = _socialPages.value + newPage
        firebaseSync.addSocialPageRemote(newPage) { success, error ->
            if (!success) addAdminLog("فشل حفظ منصة التواصل: ${error ?: "خطأ غير معروف"}")
        }
    }

    fun deleteSocialPage(id: String) {
        _socialPages.value = _socialPages.value.filter { it.id != id }
        firebaseSync.deleteSocialPageRemote(id) { success, error ->
            if (!success) addAdminLog("فشل حذف منصة التواصل: ${error ?: "خطأ غير معروف"}")
        }
    }

    fun addWebsite(name: String, url: String, description: String, emoji: String, color: String) {
        val newWeb = WebsiteItem(
            id = "web_${System.currentTimeMillis()}",
            name = name,
            url = url,
            description = description,
            emoji = emoji,
            color = color,
            order = _websites.value.size,
            isActive = true
        )
        _websites.value = _websites.value + newWeb
        firebaseSync.addWebsiteRemote(newWeb) { success, error ->
            if (!success) addAdminLog("فشل حفظ الموقع: ${error ?: "خطأ غير معروف"}")
        }
    }

    fun deleteWebsite(id: String) {
        _websites.value = _websites.value.filter { it.id != id }
        firebaseSync.deleteWebsiteRemote(id) { success, error ->
            if (!success) addAdminLog("فشل حذف الموقع: ${error ?: "خطأ غير معروف"}")
        }
    }

    fun addArchiveProgram(title: String, description: String, category: String, youtubeUrl: String, duration: String, thumbnailUrl: String = "") {
        // غلاف مستخرج من رابط يوتيوب تلقائياً، أو فارغ (تُعرض أيقونة افتراضية)
        val finalThumb = if (thumbnailUrl.isNotBlank()) thumbnailUrl else ""
        val newArch = ArchiveProgram(
            id = "arch_${System.currentTimeMillis()}",
            title = title,
            description = description,
            category = category,
            youtubeUrl = youtubeUrl,
            thumbnailUrl = finalThumb,
            date = "تمت الإضافة حديثاً",
            duration = duration
        )
        _archivePrograms.value = listOf(newArch) + _archivePrograms.value
        firebaseSync.addArchiveProgramRemote(newArch) { success, error ->
            if (!success) addAdminLog("فشل حفظ برنامج الأرشيف: ${error ?: "خطأ غير معروف"}")
        }
    }

    fun deleteArchiveProgram(id: String) {
        _archivePrograms.value = _archivePrograms.value.filter { it.id != id }
        firebaseSync.deleteArchiveProgramRemote(id) { success, error ->
            if (!success) addAdminLog("فشل حذف برنامج الأرشيف: ${error ?: "خطأ غير معروف"}")
        }
    }

    fun addEpgProgram(title: String, time: String, category: String, duration: String, description: String) {
        val newItem = EpgItem(
            id = "epg_${System.currentTimeMillis()}",
            startTime = time,
            title = title,
            category = category,
            duration = duration,
            description = description
        )
        _epgList.value = _epgList.value + newItem
        firebaseSync.addEpgProgramRemote(newItem) { success, error ->
            if (!success) addAdminLog("فشل حفظ برنامج الدليل: ${error ?: "خطأ غير معروف"}")
        }
    }

    fun deleteEpgProgram(id: String) {
        _epgList.value = _epgList.value.filter { it.id != id }
        firebaseSync.deleteEpgProgramRemote(id) { success, error ->
            if (!success) addAdminLog("فشل حذف برنامج الدليل: ${error ?: "خطأ غير معروف"}")
        }
    }

    fun signInAdmin(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        firebaseSync.signInAdmin(email, password, onResult)
    }

    fun signOutAdmin() {
        firebaseSync.signOutAdmin()
    }

    // Room DB Exposures
    val favorites: Flow<List<FavoriteProgram>> = db.favoritesDao().getAllFavorites()
    fun isFavorite(id: String): Flow<Boolean> = db.favoritesDao().isFavorite(id)
    suspend fun toggleFavorite(program: ArchiveProgram, isFav: Boolean) {
        if (isFav) {
            db.favoritesDao().deleteFavorite(program.id)
        } else {
            db.favoritesDao().insertFavorite(
                FavoriteProgram(
                    id = program.id,
                    title = program.title,
                    description = program.description,
                    category = program.category,
                    youtubeUrl = program.youtubeUrl,
                    thumbnailUrl = program.thumbnailUrl,
                    date = program.date,
                    duration = program.duration
                )
            )
        }
    }

    val watchHistory: Flow<List<WatchHistoryItem>> = db.watchHistoryDao().getWatchHistory()

    suspend fun saveWatchProgress(program: ArchiveProgram, positionMs: Long = 0, durationMs: Long = 0) {
        db.watchHistoryDao().insertOrUpdate(
            WatchHistoryItem(
                id = program.id,
                title = program.title,
                category = program.category,
                youtubeUrl = program.youtubeUrl,
                positionMs = positionMs.coerceAtLeast(0),
                durationMs = durationMs.coerceAtLeast(0),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /** Updates the playback position of an existing history item. */
    suspend fun saveHistoryPosition(item: WatchHistoryItem, positionMs: Long) {
        db.watchHistoryDao().insertOrUpdate(
            item.copy(
                positionMs = positionMs.coerceAtLeast(0),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearWatchHistory() {
        db.watchHistoryDao().clearHistory()
    }

    private val _remoteComments = MutableStateFlow<List<CommentEntity>>(emptyList())
    val comments: Flow<List<CommentEntity>> = combine(
        db.commentsDao().getComments(),
        _remoteComments
    ) { local, remote ->
        if (remote.isNotEmpty()) remote else local
    }

    suspend fun addComment(author: String, text: String, onResult: ((Boolean, String?) -> Unit)? = null) {
        if (text.isBlank()) return

        // Client-side moderation gate (ProfanityFilter.kt): blocks obvious
        // spam BEFORE any local insert or network call. Client-side only —
        // the Firestore rules remain the authoritative server-side defense.
        val moderation = com.elwataniatv.app.util.ProfanityFilter.check(text)
        if (!moderation.ok) {
            val message = com.elwataniatv.app.util.ProfanityFilter.userMessage(moderation.reason)
            appContext?.let { ctx ->
                android.widget.Toast.makeText(ctx, message, android.widget.Toast.LENGTH_LONG).show()
            }
            onResult?.invoke(false, message)
            return
        }

        db.commentsDao().insertComment(
            CommentEntity(
                remoteId = "local-${UUID.randomUUID()}",
                authorName = if (author.isBlank()) "متابع الوطنية TV" else author,
                content = text
            )
        )
        firebaseSync.postComment("live", author, text) { success, err ->
            onResult?.invoke(success, err)
        }
    }

    fun submitFeedback(type: String = "other", message: String, email: String = "", onResult: (Boolean, String?) -> Unit) {
        firebaseSync.submitFeedback(type, message, email, onResult)
    }

    fun updateBreakingNewsRemote(
        enabled: Boolean,
        text: String,
        youtubeUrl: String = "",
        onResult: (Boolean, String?) -> Unit
    ) {
        firebaseSync.updateBreakingNewsRemote(enabled, text, youtubeUrl) { success, err ->
            if (success) _breaking.value = BreakingNews(enabled, text, youtubeUrl.trim())
            onResult(success, err)
        }
    }

    fun updateAppConfigRemote(config: RemoteAppConfig, onResult: (Boolean, String?) -> Unit) {
        firebaseSync.updateAppConfigRemote(config, onResult)
    }

    fun addStreamChannelRemote(stream: RemoteStream, onResult: (Boolean, String?) -> Unit) {
        firebaseSync.addStreamChannelRemote(stream, onResult)
    }

    fun deleteStreamChannelRemote(id: String, onResult: (Boolean, String?) -> Unit) {
        firebaseSync.deleteStreamChannelRemote(id, onResult)
    }

    val reminders: Flow<List<ProgramReminder>> = db.remindersDao().getReminders()
    suspend fun addReminder(item: EpgItem) {
        val reminder = ProgramReminder(id = item.id, programTitle = item.title, startTime = item.startTime)
        db.remindersDao().saveReminder(reminder)
        // REAL scheduling: an inexact daily alarm fires near the program start.
        // It is rescheduled after delivery and on boot.
        appContext?.let { ctx ->
            com.elwataniatv.app.notifications.ReminderScheduler.ensureChannel(ctx)
            com.elwataniatv.app.notifications.ReminderScheduler.schedule(ctx, reminder)
        }
    }

    suspend fun removeReminder(id: String) {
        val existing = db.remindersDao().getReminderByIdSync(id)
        db.remindersDao().deleteReminder(id)
        existing?.let { reminder ->
            appContext?.let { ctx ->
                com.elwataniatv.app.notifications.ReminderScheduler.cancel(ctx, reminder)
            }
        }
    }

    /** Re-schedules every stored reminder (safe to call on every app start). */
    suspend fun rescheduleAllReminders() {
        val ctx = appContext ?: return
        val reminders = db.remindersDao().getAllReminders()
        com.elwataniatv.app.notifications.ReminderScheduler.ensureChannel(ctx)
        com.elwataniatv.app.notifications.ReminderScheduler.scheduleAll(ctx, reminders)
    }

    fun sendHeartbeat() {
        firebaseSync.sendHeartbeat()
    }

    fun refreshActiveDevices() {
        firebaseSync.refreshActiveDevices()
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔥 مزامنة Firestore — البيانات الحية من لوحة التحكم
    // Firestore هو مصدر المحتوى التحريري. لا توجد قنوات أو برامج وهمية
    // مضمّنة محلياً؛ عند غياب الاتصال تبقى القوائم فارغة حتى وصول البيانات الرسمية.
    // ═══════════════════════════════════════════════════════════════
    private val firebaseSync by lazy { FirebaseSync() }
    private var firebaseSyncScope: CoroutineScope? = null

    // Real viewer telemetry (device heartbeats)
    val activeDevices: StateFlow<Int> = firebaseSync.activeDevices
    val syncError: StateFlow<String?> = firebaseSync.syncError
    val syncStatus: StateFlow<com.elwataniatv.app.data.remote.SyncStatus> = firebaseSync.syncStatus

    fun startFirebaseSync() {
        if (firebaseSyncScope?.isActive == true) return
        if (!firebaseSync.start()) return

        val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        firebaseSyncScope = syncScope
        firebaseSync.apply {
            // القنوات
            launchSync(streams) { list ->
                if (list != null) {
                    _streams.value = list
                }
            }
            // الخبر العاجل
            launchSync(breaking) { b ->
                if (b != null) _breaking.value = b
            }
            // إعدادات التطبيق
            launchSync(appConfig) { c ->
                if (c != null) _appConfig.value = c
            }
            // EPG
            launchSync(epg) { list ->
                if (list != null) {
                    _epgList.value = list
                    // Keep the home-screen widget in sync with the guide.
                    appContext?.let { ctx ->
                        com.elwataniatv.app.widget.NowOnAirWidget.refresh(
                            ctx,
                            list,
                            _appConfig.value.appName.ifBlank {
                                ctx.getString(com.elwataniatv.app.R.string.app_name)
                            }
                        )
                    }
                }
            }
            // ترددات الأقمار الصناعية
            launchSync(satelliteFrequencies) { list ->
                if (list != null) _satelliteFrequencies.value = list
            }
            // المواقع
            launchSync(websites) { list ->
                if (list != null) _websites.value = list
            }
            // التواصل
            launchSync(social) { list ->
                if (list != null) _socialPages.value = list
            }
            // الأرشيف
            launchSync(archive) { list ->
                if (list != null) _archivePrograms.value = list
            }
            // التعليقات الحية
            launchSync(comments) { list ->
                if (list != null) _remoteComments.value = list
            }
            // البنرات الإعلانية
            launchSync(adBanners) { list ->
                if (list != null) _adBanners.value = list
            }
            // تفاعلات المشاهدين المباشرة
            launchSync(liveReactions) { map ->
                if (map != null) _liveReactions.value = map
            }
            launchSync(myReaction) { emoji ->
                _myReaction.value = emoji
            }
            launchSync(inAppNotifications) { list ->
                if (list != null) _inAppNotifications.value = list
            }
        }
    }

    fun stopFirebaseSync() {
        firebaseSync.stop()
        firebaseSyncScope?.cancel()
        firebaseSyncScope = null
    }

    fun shutdown() {
        stopFirebaseSync()
        streamHealthJob?.cancel()
        repositoryScope.cancel()
    }

    /** يربط StateFlow من FirebaseSync مع نطاق المزامنة الحالي فقط. */
    private fun <T> launchSync(
        flow: kotlinx.coroutines.flow.StateFlow<T>,
        onValue: (T) -> Unit,
    ) {
        val scope = firebaseSyncScope ?: return
        flow
            .onEach { onValue(it) }
            .launchIn(scope)
    }

    companion object {
        // No stream is embedded locally. Active sources must be supplied by the channel operator through Firestore.
        val DEFAULT_STREAMS: List<RemoteStream> = emptyList()

        val DEFAULT_BREAKING = BreakingNews(
            enabled = false,
            text = ""
        )

        val DEFAULT_APP_CONFIG = RemoteAppConfig(
            appName = "",
            appSlogan = "",
            logoUrl = "",
            privacyUrl = "",
            contactEmail = "",
            officialWebsite = "",
            maintenanceMode = false,
            enableOnboarding = true,
            enableEpg = true,
            enableComments = true,
            enablePush = true,
            enableDarkMode = true
        )

        // Editorial content is owned by Firestore and the admin panel.
        // Empty fallbacks prevent stale or hardcoded Arabic content before the first snapshot.
        val DEFAULT_EPG = emptyList<EpgItem>()
        val DEFAULT_SATELLITE_FREQUENCIES = emptyList<SatelliteFrequency>()
        val DEFAULT_WEBSITES = emptyList<WebsiteItem>()
        val DEFAULT_SOCIAL = emptyList<SocialPage>()
        val DEFAULT_ARCHIVE = emptyList<ArchiveProgram>()
        val DEFAULT_AD_BANNERS = emptyList<AdBanner>()
    }
}
