package com.elwataniatv.app.data.remote

import android.util.Log
import com.elwataniatv.app.util.ContentSanitizer
import com.elwataniatv.app.data.local.CommentEntity
import com.elwataniatv.app.data.model.*
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * FirestoreContentSync.kt
 * ─────────────────────────────────────────────────────────────────
 * Firestore content sync extracted from the old FirebaseSync.kt.
 *
 * This class owns:
 *   - the snapshot listeners for every content collection
 *     (streams, breaking, config/app, epg, websites, social, archive,
 *     comments, ad_banners, config/popup, live reactions, notifications)
 *   - the StateFlows those listeners feed (consumed by the repository)
 *   - all Firestore writes (comments, feedback, admin CMS operations)
 *
 * Authentication is delegated to [FirebaseAuthSync] (anonymous sign-in +
 * admin sign-in). FCM token registration lives in [FcmTokenSync].
 * The public facade `FirebaseSync` composes the three classes and keeps
 * the exact same API as the original single file.
 * ─────────────────────────────────────────────────────────────────
 */
class FirestoreContentSync(private val authSync: FirebaseAuthSync) {

    private companion object {
        private const val TAG = "FirestoreContentSync"

        private fun isTestValue(value: String): Boolean = ContentSanitizer.isKnownTestValue(value)
    }

    internal val db: FirebaseFirestore? by lazy {
        runCatching { Firebase.firestore }
            .onFailure { Log.w(TAG, "Firebase Firestore غير متاح: ${it.message}") }
            .getOrNull()
    }

    // ─── StateFlows (تحدَّث من Firestore) ──────────────────────────
    private val _streams = MutableStateFlow<List<RemoteStream>?>(null)
    val streams: StateFlow<List<RemoteStream>?> = _streams.asStateFlow()

    private val _breaking = MutableStateFlow<BreakingNews?>(null)
    val breaking: StateFlow<BreakingNews?> = _breaking.asStateFlow()

    private val _appConfig = MutableStateFlow<RemoteAppConfig?>(null)
    val appConfig: StateFlow<RemoteAppConfig?> = _appConfig.asStateFlow()
    private var userDarkModeOverride: Boolean? = null
    private var userPushOverride: Boolean? = null

    private val _epg = MutableStateFlow<List<EpgItem>?>(null)
    val epg: StateFlow<List<EpgItem>?> = _epg.asStateFlow()

    private val _satelliteFrequencies = MutableStateFlow<List<SatelliteFrequency>?>(null)
    val satelliteFrequencies: StateFlow<List<SatelliteFrequency>?> = _satelliteFrequencies.asStateFlow()

    private val _websites = MutableStateFlow<List<WebsiteItem>?>(null)
    val websites: StateFlow<List<WebsiteItem>?> = _websites.asStateFlow()

    private val _news = MutableStateFlow<List<NewsItem>>(emptyList())
    val news: StateFlow<List<NewsItem>> = _news.asStateFlow()

    private val _social = MutableStateFlow<List<SocialPage>?>(null)
    val social: StateFlow<List<SocialPage>?> = _social.asStateFlow()

    private val _archive = MutableStateFlow<List<ArchiveProgram>?>(null)
    val archive: StateFlow<List<ArchiveProgram>?> = _archive.asStateFlow()

    private val _comments = MutableStateFlow<List<CommentEntity>?>(null)
    val comments: StateFlow<List<CommentEntity>?> = _comments.asStateFlow()

    private val _adBanners = MutableStateFlow<List<AdBanner>?>(null)
    val adBanners: StateFlow<List<AdBanner>?> = _adBanners.asStateFlow()

    private val _popupAlert = MutableStateFlow<PopupAlert?>(null)
    val popupAlert: StateFlow<PopupAlert?> = _popupAlert.asStateFlow()

    private val _liveReactions = MutableStateFlow<Map<String, Int>?>(null)
    val liveReactions: StateFlow<Map<String, Int>?> = _liveReactions.asStateFlow()

    private val _myReaction = MutableStateFlow<String?>(null)
    val myReaction: StateFlow<String?> = _myReaction.asStateFlow()

    private val _inAppNotifications = MutableStateFlow<List<InAppNotification>?>(null)
    val inAppNotifications: StateFlow<List<InAppNotification>?> = _inAppNotifications.asStateFlow()

    // Real viewer telemetry: number of devices seen in the last N minutes
    private val _activeDevices = MutableStateFlow<Int>(0)
    val activeDevices: StateFlow<Int> = _activeDevices.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private fun markSyncSuccess(collection: String) {
        _syncError.value = null
        _syncStatus.value = SyncStatus(
            isConnected = true,
            lastUpdatedAt = System.currentTimeMillis(),
            lastCollection = collection,
            errorMessage = null
        )
    }

    private fun markSyncFailure(message: String) {
        _syncError.value = message
        _syncStatus.value = _syncStatus.value.copy(
            isConnected = false,
            errorMessage = message
        )
    }

    private val listeners = mutableListOf<ListenerRegistration>()
    private var configuredDefaultStreamUrl: String = ""
    private var hasExplicitStreamsCollection: Boolean = false

    private fun configuredFallbackStream(): RemoteStream? {
        val url = configuredDefaultStreamUrl.trim()
        if (url.isBlank()) return null
        val isYoutube = url.contains("youtube.com", ignoreCase = true) || url.contains("youtu.be", ignoreCase = true)
        val isHls = url.contains(".m3u8", ignoreCase = true)
        if (!isYoutube && !isHls) return null
        return RemoteStream(
            id = "live_main",
            title = "",
            url = url,
            type = if (isYoutube) "youtube" else "m3u8",
            logoUrl = "",
            isActive = true,
            order = 0
        )
    }

    /** يوقف كل المستمعين (يُستدعى عند إغلاق التطبيق أو إعادة الاتصال). */
    fun stop() {
        listeners.forEach { runCatching { it.remove() } }
        listeners.clear()
    }

    /**
     * Writes this installation.s heartbeat to devices/{deviceId} (top-level collection)
     * (rate-limited by the rules to one write per 5 minutes).
     */
    fun sendHeartbeat() {
        val dbInstance = runCatching { db }.getOrNull() ?: return
        val authInstance = runCatching { authSync.auth }.getOrNull() ?: return
        authSync.ensureAnonymousAuth(authInstance) {
            val installationId = com.elwataniatv.app.ElWataniaApp.installationId
            val userUid = authInstance.currentUser?.uid ?: return@ensureAnonymousAuth
            dbInstance.document("devices/$userUid").set(
                mapOf(
                    "lastSeen" to com.google.firebase.Timestamp.now(),
                    "updatedAt" to com.google.firebase.Timestamp.now(),
                    "userUid" to userUid,
                    "installationId" to installationId,
                    "appVersion" to com.elwataniatv.app.BuildConfig.VERSION_NAME,
                    "platform" to "android"
                ),
                com.google.firebase.firestore.SetOptions.merge()
            ).addOnFailureListener { e ->
                Log.w(TAG, "heartbeat فشل (قد يكون داخل فترة التحديد): ${e.message}")
            }
        }
    }

    /**
     * Reads the privacy-preserving aggregate maintained at stats/active_viewers.
     * Devices remain admin-only; ordinary users never query the devices collection.
     * The dashboard can refresh this aggregate, while a scheduled function can
     * maintain it automatically after upgrading Firebase billing if required.
     */
    fun refreshActiveDevices(withinMinutes: Int = 15) {
        val dbInstance = runCatching { db }.getOrNull() ?: return
        dbInstance.document("stats/active_viewers")
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { snap ->
                _activeDevices.value = (snap.getLong("count") ?: 0L).coerceAtLeast(0L).toInt()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "فشل قراءة إحصاء المشاهدين النشطين: ${e.message}")
            }
    }

    // ─── المستمعون ────────────────────────────────────────────────

    fun listenStreams(db: FirebaseFirestore) {
        runCatching {
            val unsub = db.collection("streams")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) {
                        _syncError.value = err?.message ?: "Firestore snapshot unavailable"
                        return@addSnapshotListener
                    }
                    _syncError.value = null
                    val remoteStreams = snap.documents.mapNotNull { d ->
                        runCatching {
                            RemoteStream(
                                id = d.id,
                                title = d.getString("title") ?: "",
                                url = d.getString("url") ?: "",
                                type = d.getString("type") ?: "m3u8",
                                logoUrl = d.getString("logoUrl") ?: d.getString("icon") ?: "",
                                isActive = d.getBoolean("isActive") ?: true,
                                order = (d.getLong("order") ?: 0L).toInt(),
                            )
                        }.getOrNull()?.takeIf { stream ->
                            stream.isActive &&
                                stream.url.isNotBlank() &&
                                ContentSanitizer.isUsable(stream.title)
                        }
                    }.sortedWith(compareBy<RemoteStream> { it.order }.thenBy { it.title })
                    hasExplicitStreamsCollection = remoteStreams.isNotEmpty()
                    _streams.value = remoteStreams.ifEmpty { configuredFallbackStream()?.let(::listOf).orEmpty() }
                    markSyncSuccess("streams")
                }
            listeners += unsub
        }.onFailure { e -> Log.w(TAG, "فشل listenStreams: ${e.message}") }
    }

    fun listenBreaking(db: FirebaseFirestore) {
        runCatching {
            val unsub = db.document("config/breaking")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null || !snap.exists()) return@addSnapshotListener
                    _breaking.value = BreakingNews(
                        enabled = snap.getBoolean("enabled") ?: false,
                        text = snap.getString("text") ?: "",
                        youtubeUrl = snap.getString("youtubeUrl") ?: snap.getString("url") ?: "",
                    )
                }
            listeners += unsub
        }.onFailure { e -> Log.w(TAG, "فشل listenBreaking: ${e.message}") }
    }

    fun listenAppConfig(db: FirebaseFirestore) {
        runCatching {
            val unsub = db.document("config/app")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null || !snap.exists()) return@addSnapshotListener
                    val baseConfig = RemoteAppConfig(
                        appName = snap.getString("appName")?.trim().orEmpty(),
                        appSlogan = snap.getString("appSlogan")?.trim().orEmpty(),
                        logoUrl = snap.getString("logoUrl") ?: "",
                        onboardingBannerUrl = snap.getString("onboardingBannerUrl") ?: "",
                        accentColor = snap.getString("accentColor") ?: "",
                        privacyUrl = snap.getString("privacyUrl")?.trim().orEmpty(),
                        contactEmail = snap.getString("contactEmail")?.trim().orEmpty(),
                        officialWebsite = snap.getString("officialWebsite")?.trim().orEmpty(),
                        appStoreUrl = snap.getString("appStoreUrl")?.trim().orEmpty(),
                        iosStoreUrl = snap.getString("iosStoreUrl")?.trim().orEmpty(),
                        facebookUrl = snap.getString("facebookUrl")?.trim().orEmpty(),
                        youtubeUrl = snap.getString("youtubeUrl")?.trim().orEmpty(),
                        telegramUrl = snap.getString("telegramUrl")?.trim().orEmpty(),
                        tiktokUrl = snap.getString("tiktokUrl")?.trim().orEmpty(),
                        instagramUrl = snap.getString("instagramUrl")?.trim().orEmpty(),
                        twitterUrl = snap.getString("twitterUrl")?.trim().orEmpty(),
                        whatsappUrl = snap.getString("whatsappUrl")?.trim().orEmpty(),
                        defaultStreamUrl = snap.getString("defaultStreamUrl")?.trim().orEmpty(),
                        minVersion = snap.getString("minVersion") ?: "",
                        latestVersion = snap.getString("latestVersion") ?: "",
                        updateUrl = snap.getString("updateUrl") ?: "",
                        updateMessage = snap.getString("updateMessage") ?: "",
                        primaryColor = snap.getString("primaryColor")?.trim().orEmpty(),
                        secondaryColor = snap.getString("secondaryColor")?.trim().orEmpty(),
                        maintenanceMode = snap.getBoolean("maintenanceMode") ?: false,
                        maintenanceMessage = snap.getString("maintenanceMessage") ?: "",
                        enableOnboarding = snap.getBoolean("enableOnboarding") ?: true,
                        enableEpg = snap.getBoolean("enableEpg") ?: true,
                        showArchivePreview = snap.getBoolean("showArchivePreview") ?: true,
                        showPromotionalBanners = snap.getBoolean("showPromotionalBanners") ?: true,
                        enableArchive = snap.getBoolean("enableArchive") ?: true,
                        enableSocial = snap.getBoolean("enableSocial") ?: true,
                        enableWebsites = snap.getBoolean("enableWebsites") ?: true,
                        enableComments = snap.getBoolean("enableComments") ?: true,
                        enablePush = snap.getBoolean("enablePush") ?: true,
                        enableDarkMode = snap.getBoolean("enableDarkMode") ?: true,
                        enableDynamicColor = snap.getBoolean("enableDynamicColor") ?: false,
                    )
                    configuredDefaultStreamUrl = baseConfig.defaultStreamUrl
                    _appConfig.value = baseConfig.withUserPreferences()
                    if (!hasExplicitStreamsCollection) {
                        _streams.value = configuredFallbackStream()?.let(::listOf).orEmpty()
                    }
                    markSyncSuccess("config/app")
                }
            listeners += unsub
        }.onFailure { e -> Log.w(TAG, "فشل listenAppConfig: ${e.message}") }
    }

    /** Reads the owner-only preference document and overlays it on public config. */
    fun listenUserPreferences(db: FirebaseFirestore) {
        val authInstance = runCatching { authSync.auth }.getOrNull() ?: return
        authSync.ensureAnonymousAuth(authInstance) {
            val uid = authInstance.currentUser?.uid ?: return@ensureAnonymousAuth
            val unsub = db.document("users/$uid/preferences/app")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) return@addSnapshotListener
                    userDarkModeOverride = snap.getBoolean("darkModeEnabled")
                    userPushOverride = snap.getBoolean("pushEnabled")
                    _appConfig.value = _appConfig.value?.withUserPreferences()
                }
            listeners += unsub
        }
    }

    fun updateUserPreferences(
        darkModeEnabled: Boolean,
        pushEnabled: Boolean,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val dbInstance = runCatching { db }.getOrNull()
        val authInstance = runCatching { authSync.auth }.getOrNull()
        if (dbInstance == null || authInstance == null) {
            onResult(false, "Firebase غير متاح")
            return
        }
        authSync.ensureAnonymousAuth(authInstance) {
            val uid = authInstance.currentUser?.uid
            if (uid.isNullOrBlank()) {
                onResult(false, "تعذر تحديد المستخدم")
                return@ensureAnonymousAuth
            }
            dbInstance.document("users/$uid/preferences/app").set(
                mapOf(
                    "darkModeEnabled" to darkModeEnabled,
                    "pushEnabled" to pushEnabled,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).addOnSuccessListener {
                userDarkModeOverride = darkModeEnabled
                userPushOverride = pushEnabled
                _appConfig.value = _appConfig.value?.withUserPreferences()
                onResult(true, null)
            }.addOnFailureListener { error -> onResult(false, error.message) }
        }
    }

    private fun RemoteAppConfig.withUserPreferences(): RemoteAppConfig = copy(
        enableDarkMode = userDarkModeOverride ?: enableDarkMode,
        enablePush = userPushOverride ?: enablePush
    )

    fun listenSatelliteFrequencies(db: FirebaseFirestore) {
        runCatching {
            val unsub = db.collection("satellite_frequencies")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) {
                        _syncError.value = err?.message ?: "Satellite frequencies unavailable"
                        return@addSnapshotListener
                    }
                    val frequencies = snap.documents.mapNotNull { document ->
                        runCatching {
                            SatelliteFrequency(
                                id = document.id,
                                satelliteName = document.getString("satelliteName") ?: "",
                                orbitalPosition = document.getString("orbitalPosition") ?: "",
                                frequencyMhz = (document.getLong("frequencyMhz") ?: 0L).toInt(),
                                polarization = document.getString("polarization") ?: "",
                                symbolRate = (document.getLong("symbolRate") ?: 0L).toInt(),
                                fec = document.getString("fec") ?: "",
                                notes = document.getString("notes") ?: "",
                                isActive = document.getBoolean("isActive") ?: true,
                                order = (document.getLong("order") ?: 0L).toInt()
                            )
                        }.getOrNull()
                    }.filter { it.isActive && it.satelliteName.isNotBlank() }
                        .sortedWith(compareBy<SatelliteFrequency> { it.order }.thenBy { it.satelliteName })
                    _satelliteFrequencies.value = frequencies
                }
            listeners += unsub
        }.onFailure { e -> Log.w(TAG, "فشل listenSatelliteFrequencies: ${e.message}") }
    }

    fun listenEpg(db: FirebaseFirestore) {
        runCatching {
            val unsub = db.collection("epg")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) {
                        _syncError.value = err?.message ?: "Firestore snapshot unavailable"
                        return@addSnapshotListener
                    }
                    _syncError.value = null
                    val items = snap.documents.flatMap { d ->
                        runCatching {
                            val docTitle = d.getString("title") ?: ""
                            val docTime = d.getString("startTime") ?: "00:00"
                            val docActive = d.getBoolean("isActive") != false
                            val docOrder = (d.getLong("order") ?: 0L).toInt()
                            if (isTestValue(docTitle)) return@runCatching emptyList<EpgItem>()
                            val programs = d.get("programs") as? List<*>
                            if (programs != null && programs.isNotEmpty()) {
                                // Flat list of programs (time + title) maintained by the admin panel
                                programs.mapNotNull { raw ->
                                    val m = raw as? Map<*, *> ?: return@mapNotNull null
                                    val t = m["time"] as? String ?: return@mapNotNull null
                                    val title = m["title"] as? String ?: ""
                                    val active = m["isActive"] as? Boolean ?: docActive
                                    if (!active || title.isBlank() || isTestValue(title)) return@mapNotNull null
                                    EpgItem(
                                        id = d.id + "_" + t,
                                        startTime = t,
                                        title = title,
                                        category = d.getString("category") ?: "",
                                        duration = m["duration"] as? String ?: d.getString("duration") ?: "",
                                        description = d.getString("description") ?: "",
                                        order = (m["order"] as? Number)?.toInt() ?: docOrder,
                                    )
                                }
                            } else if (docActive) {
                                listOf(
                                    EpgItem(
                                        id = d.id,
                                        startTime = docTime,
                                        title = docTitle,
                                        category = d.getString("category") ?: "",
                                        duration = d.getString("duration") ?: "",
                                        description = d.getString("description") ?: "",
                                        order = docOrder,
                                    )
                                )
                            } else {
                                emptyList()
                            }
                        }.getOrElse { emptyList() }
                    }.sortedWith(compareBy<EpgItem> { it.order }.thenBy { it.startTime })
                    _epg.value = items
                    markSyncSuccess("epg")
                }
            listeners += unsub
        }.onFailure { e -> Log.w(TAG, "فشل listenEpg: ${e.message}") }
    }

    fun listenNews(db: FirebaseFirestore) {
        runCatching {
            val unsub = db.collection("news")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) {
                        _syncError.value = err?.message ?: "Firestore snapshot unavailable"
                        return@addSnapshotListener
                    }
                    _syncError.value = null
                    _news.value = snap.documents.mapNotNull { d ->
                        runCatching {
                            NewsItem(
                                id = d.id,
                                title = d.getString("title") ?: "",
                                summary = d.getString("summary") ?: "",
                                url = d.getString("url") ?: "",
                                imageUrl = d.getString("imageUrl") ?: "",
                                order = (d.getLong("order") ?: 0L).toInt(),
                                isActive = d.getBoolean("isActive") ?: true,
                                createdAt = d.getLong("createdAt") ?: 0L,
                            )
                        }.getOrNull()
                    }
                        .filter { it.isActive && it.title.isNotBlank() && !isTestValue(it.title) }
                        .sortedWith(compareBy<NewsItem> { it.order }.thenBy { it.createdAt })
                }
            listeners += unsub
        }.onFailure { e -> Log.w(TAG, "فشل listenNews: ${e.message}") }
    }

    fun listenWebsites(db: FirebaseFirestore) {
        runCatching {
            val unsub = db.collection("websites")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) {
                        _syncError.value = err?.message ?: "Firestore snapshot unavailable"
                        return@addSnapshotListener
                    }
                    _syncError.value = null
                    _websites.value = snap.documents.mapNotNull { d ->
                        runCatching {
                            WebsiteItem(
                                id = d.id,
                                name = d.getString("name") ?: "",
                                url = d.getString("url") ?: "",
                                description = d.getString("description") ?: "",
                                logoUrl = d.getString("logoUrl") ?: d.getString("icon") ?: "",
                                order = (d.getLong("order") ?: 0L).toInt(),
                                isActive = d.getBoolean("isActive") ?: true,
                                emoji = d.getString("emoji") ?: "",
                                color = d.getString("color") ?: "#0a7ea4",
                            )
                        }.getOrNull()
                    }
                        .filter { it.isActive }
                        .sortedBy { it.order }
                }
            listeners += unsub
        }.onFailure { e -> Log.w(TAG, "فشل listenWebsites: ${e.message}") }
    }

    fun listenSocial(db: FirebaseFirestore) {
        runCatching {
            val unsub = db.collection("social")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) {
                        _syncError.value = err?.message ?: "Firestore snapshot unavailable"
                        return@addSnapshotListener
                    }
                    _syncError.value = null
                    _social.value = snap.documents.mapNotNull { d ->
                        runCatching {
                            val rawPlatform = d.getString("platform").orEmpty()
                            val pageUrl = d.getString("url").orEmpty()
                            SocialPage(
                                id = d.id,
                                platform = rawPlatform.ifBlank { inferSocialPlatform(pageUrl) },
                                name = d.getString("name") ?: "",
                                url = pageUrl,
                                description = d.getString("description") ?: "",
                                logoUrl = d.getString("logoUrl") ?: d.getString("icon") ?: "",
                                order = (d.getLong("order") ?: 0L).toInt(),
                                isActive = d.getBoolean("isActive") ?: true,
                                emoji = d.getString("emoji") ?: "",
                                color = d.getString("color") ?: "#1877F2",
                            )
                        }.getOrNull()
                    }
                        .filter { it.isActive }
                        .sortedBy { it.order }
                }
            listeners += unsub
        }.onFailure { e -> Log.w(TAG, "فشل listenSocial: ${e.message}") }
    }

    fun listenArchive(db: FirebaseFirestore) {
        runCatching {
            // No orderBy on the query: if ANY archive doc lacks the "date"
            // field the whole query fails silently and the archive never
            // loads. Sorting is done client-side below instead.
            val unsub = db.collection("archive")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) {
                        markSyncFailure(err?.message ?: "Firestore snapshot unavailable")
                        return@addSnapshotListener
                    }
                    _archive.value = snap.documents.mapNotNull { d ->
                        runCatching {
                            ArchiveProgram(
                                id = d.id,
                                title = d.getString("title") ?: "",
                                youtubeUrl = d.getString("youtubeUrl") ?: d.getString("url") ?: "",
                                thumbnailUrl = d.getString("thumbnailUrl") ?: "",
                                category = d.getString("category") ?: "أخرى",
                                date = d.getString("date") ?: "",
                                duration = d.getString("duration") ?: "",
                                description = d.getString("description") ?: "",
                                isActive = d.getBoolean("isActive") ?: true,
                                isFeatured = d.getBoolean("isFeatured") ?: true,
                                editorialPriority = (d.getLong("editorialPriority") ?: 0L).toInt(),
                                isBreaking = d.getBoolean("isBreaking") ?: false,
                                publishAt = d.getTimestamp("publishAt")?.toDate()?.time,
                                expiresAt = d.getTimestamp("expiresAt")?.toDate()?.time
                            )
                        }.getOrNull()?.takeIf { item ->
                            ContentSanitizer.isUsable(item.title) &&
                                ContentSanitizer.isUsable(item.description) &&
                                ContentSanitizer.isUsable(item.category)
                        }
                    }.filter {
                        it.isActive &&
                            (it.publishAt == null || it.publishAt <= System.currentTimeMillis()) &&
                            (it.expiresAt == null || it.expiresAt > System.currentTimeMillis())
                    }
                        .sortedByDescending { it.date }
                    markSyncSuccess("archive")
                }
            listeners += unsub
        }.onFailure { e ->
            markSyncFailure(e.message ?: "Firestore listener unavailable")
            Log.w(TAG, "فشل listenArchive: ${e.message}")
        }
    }

    fun listenComments(db: FirebaseFirestore, programId: String = "live") {
        runCatching {
            val unsub = db.collection("programs").document(programId)
                .collection("comments")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) {
                        _syncError.value = err?.message ?: "Firestore snapshot unavailable"
                        return@addSnapshotListener
                    }
                    _syncError.value = null
                    val parsed = snap.documents.mapNotNull { d ->
                        runCatching {
                            val author = d.getString("author") ?: "متابع"
                            val rawText = d.getString("text") ?: ""
                            val moderation = d.get("moderation") as? Map<*, *>
                            val isHidden = moderation != null && moderation["ok"] == false
                            val finalText = if (isHidden) "تم إخفاء هذا التعليق لمخالفته شروط النشر" else rawText

                            val reactions = d.get("reactions") as? Map<*, *>
                            val likes = (reactions?.get("like") as? Long ?: 0L).toInt()

                            val createdAt = d.getTimestamp("createdAt")
                            val timeMs = createdAt?.toDate()?.time ?: System.currentTimeMillis()

                            CommentEntity(
                                // The Firestore document ID is the authoritative stable key.
                                remoteId = d.id,
                                authorName = author,
                                content = finalText,
                                timestamp = timeMs,
                                likesCount = likes
                            )
                        }.getOrNull()
                    }
                    _comments.value = parsed.sortedByDescending { comment -> comment.timestamp }
                }
            listeners += unsub
        }.onFailure { e -> Log.w(TAG, "فشل listenComments: ${e.message}") }
    }

    fun listenAdBanners(db: FirebaseFirestore) {
        runCatching {
            // No orderBy on the query: a doc missing the "order" field would
            // break the whole listener. Sorting is done client-side instead.
            val unsub = db.collection("ad_banners")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) {
                        _syncError.value = err?.message ?: "Firestore snapshot unavailable"
                        return@addSnapshotListener
                    }
                    _syncError.value = null
                    _adBanners.value = snap.documents.mapNotNull { d ->
                        runCatching {
                            AdBanner(
                                id = d.id,
                                title = d.getString("title") ?: "",
                                imageUrl = d.getString("imageUrl") ?: "",
                                targetUrl = d.getString("targetUrl") ?: "",
                                isEnabled = d.getBoolean("isEnabled") ?: true,
                                order = (d.getLong("order") ?: 0L).toInt()
                            )
                        }.getOrNull()
                    }.sortedBy { it.order }
                }
            listeners += unsub
        }.onFailure { e -> Log.w(TAG, "فشل listenAdBanners: ${e.message}") }
    }

    fun listenPopupAlert(db: FirebaseFirestore) {
        runCatching {
            val unsub = db.document("config/popup").addSnapshotListener { snap, err ->
                if (err == null && snap != null && snap.exists()) {
                    _popupAlert.value = PopupAlert(
                        active = snap.getBoolean("active") ?: false,
                        title = snap.getString("title") ?: "",
                        message = snap.getString("message") ?: "",
                        buttonText = snap.getString("buttonText")?.trim().orEmpty(),
                        alertType = snap.getString("alertType") ?: "info"
                    )
                }
            }
            listeners += unsub
        }.onFailure { e -> Log.w(TAG, "فشل listenPopupAlert: ${e.message}") }
    }

    // ─── تفاعلات البث المباشر (عدادات حقيقية من Firestore) ─────────

    fun listenLiveReactions(db: FirebaseFirestore) {
        runCatching {
            val unsub = db.document("live/reactions").addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val counts = FirebaseSync.REACTION_EMOJIS.associateWith { emoji ->
                    (snap.getLong(emoji) ?: 0L).toInt()
                }
                _liveReactions.value = counts
            }
            listeners += unsub
        }.onFailure { e -> Log.w(TAG, "فشل listenLiveReactions: ${e.message}") }
    }

    fun listenMyReaction(db: FirebaseFirestore) {
        runCatching {
            val userUid = authSync.auth?.currentUser?.uid ?: return@runCatching
            // Subcollection of live/reactions keyed by Firebase Auth UID.
            val unsub = db.document("live/reactions/user_reactions/$userUid").addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                _myReaction.value = if (snap.exists()) snap.getString("emoji") else null
            }
            listeners += unsub
        }.onFailure { e -> Log.w(TAG, "فشل listenMyReaction: ${e.message}") }
    }


    fun listenNotifications(db: FirebaseFirestore) {
        runCatching {
            val unsub = db.collection("notifications")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5)
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) {
                        _syncError.value = err?.message ?: "Firestore snapshot unavailable"
                        return@addSnapshotListener
                    }
                    _syncError.value = null
                    _inAppNotifications.value = snap.documents.mapNotNull { d ->
                        runCatching {
                            val data = d.data ?: return@runCatching null
                            // Test notifications and known placeholder records never reach the UI.
                            val rawTitle = d.getString("title") ?: ""
                            val rawBody = d.getString("body") ?: ""
                            if (data["test"] == true || isTestValue(rawTitle) || isTestValue(rawBody)) {
                                return@runCatching null
                            }
                            InAppNotification(
                                id = d.id,
                                title = d.getString("title") ?: "تنبيه من الوطنية TV",
                                body = d.getString("body") ?: "",
                                youtubeUrl = d.getString("youtubeUrl") ?: d.getString("url") ?: "",
                                isActive = d.getBoolean("isActive") != false,
                                expiresAt = d.getTimestamp("expiresAt")?.toDate()?.time,
                                createdAt = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                            )
                        }.getOrNull()
                    }
                        .filter { it.isActive && (it.expiresAt == null || it.expiresAt > System.currentTimeMillis()) }
                        .sortedByDescending { it.createdAt }
                }
            listeners += unsub
        }.onFailure { e -> Log.w(TAG, "فشل listenNotifications: ${e.message}") }
    }

    // Client reactions are intentionally read-only. Firestore rules reserve
    // writes to live/reactions and comment reactions for trusted server/admin
    // paths, so this class exposes only the listeners above and comment creation.

    /** إرسال تعليق إلى Firestore مع التحقق من الهوية والأذونات. */
    fun postComment(
        programId: String = "live",
        authorName: String,
        text: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        // Client-side moderation gate — runs BEFORE any network call
        // (including anonymous sign-in) so rejected comments cost nothing.
        // Client-side only: the Firestore rules are the authoritative gate.
        val moderation = com.elwataniatv.app.util.ProfanityFilter.check(text)
        if (!moderation.ok) {
            onResult(false, com.elwataniatv.app.util.ProfanityFilter.userMessage(moderation.reason))
            return
        }

        val dbInstance = runCatching { db }.getOrNull()

        if (dbInstance == null) {
            onResult(false, "خدمات Firebase غير متوفرة")
            return
        }

        authSync.ensureAnonymousAuth(authSync.auth ?: run {
            onResult(false, "مصادقة Firebase غير متوفرة")
            return
        }) {
            sendCommentDoc(dbInstance, programId, authorName, text, onResult)
        }
    }

    private fun sendCommentDoc(
        db: FirebaseFirestore,
        programId: String,
        authorName: String,
        text: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val uid = authSync.auth?.currentUser?.uid ?: ""
        // Server-generated timestamps: the Firestore rules require
        // createdAt == request.time (and lastCommentAt == request.time
        // on the user doc) so clients cannot backdate or forge times.
        val payload = hashMapOf(
            "author" to (if (authorName.isBlank()) "متابع الوطنية TV" else authorName.take(50)),
            "text" to text.take(500),
            // NOTE: must match the field name the Firestore rules validate
            // (rules hasOnly: author, text, deviceId, userUid, parentId, createdAt, reactions)
            "deviceId" to com.elwataniatv.app.ElWataniaApp.installationId,
            "userUid" to uid,
            "parentId" to "",
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "reactions" to mapOf(
                "like" to 0,
                "love" to 0,
                "laugh" to 0,
                "wow" to 0,
                "sad" to 0,
                "dislike" to 0
            )
        )

        val commentRef = db.collection("programs").document(programId)
            .collection("comments").document()

        val batch = db.batch()
        batch.set(commentRef, payload)
        // Keep the per-user posting timestamp fresh so the Firestore
        // rules' rate-limit gate (users/{uid}.lastCommentAt vs
        // request.time) can enforce a minimum interval between comments.
        // Writes are atomic: if the comment is rejected by the rules, the
        // timestamp is not advanced either.
        if (uid.isNotEmpty()) {
            batch.set(
                db.collection("users").document(uid),
                hashMapOf("lastCommentAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()),
                SetOptions.merge()
            )
        }

        batch.commit()
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }

    /** إرسال الاقتراحات والملاحظات إلى مجموعة feedback مع التحقق من الهوية والقواعد. */
    fun submitFeedback(
        type: String = "other",
        message: String,
        email: String = "",
        onResult: (Boolean, String?) -> Unit
    ) {
        val dbInstance = runCatching { db }.getOrNull()

        if (dbInstance == null) {
            onResult(false, "خدمات Firebase غير متوفرة")
            return
        }

        authSync.ensureAnonymousAuth(authSync.auth ?: run {
            onResult(false, "مصادقة Firebase غير متوفرة")
            return
        }) {
            sendFeedbackDoc(dbInstance, type, message, email, onResult)
        }
    }

    private fun sendFeedbackDoc(
        db: FirebaseFirestore,
        type: String,
        message: String,
        email: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val validTypes = listOf("bug", "feature", "content", "other")
        val payload = hashMapOf(
            "type" to if (type in validTypes) type else "other",
            "message" to message.take(500),
            "email" to email,
            // NOTE: must match the field name the Firestore rules validate
            // (rules hasOnly: type, message, email, deviceId, userUid, appVersion, platform, createdAt)
            "deviceId" to com.elwataniatv.app.ElWataniaApp.installationId,
            "userUid" to (authSync.auth?.currentUser?.uid ?: ""),
            "appVersion" to com.elwataniatv.app.BuildConfig.VERSION_NAME,
            "platform" to "android",
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("feedback").add(payload)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }

    // ─── العمليات الإدارية المباشرة مع Firestore (تتطلب حساب مسؤول) ───

    fun updateBreakingNewsRemote(
        enabled: Boolean,
        text: String,
        youtubeUrl: String = "",
        onResult: (Boolean, String?) -> Unit
    ) {
        val dbInstance = runCatching { db }.getOrNull()
        if (dbInstance == null) { onResult(false, "Firebase غير متوفر"); return }
        dbInstance.document("config/breaking").set(
            mapOf(
                "enabled" to enabled,
                "text" to text,
                "youtubeUrl" to youtubeUrl.trim(),
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
        ).addOnSuccessListener {
            _breaking.value = BreakingNews(enabled, text, youtubeUrl.trim())
            onResult(true, null)
        }.addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }

    fun updateAppConfigRemote(config: RemoteAppConfig, onResult: (Boolean, String?) -> Unit) {
        val dbInstance = runCatching { db }.getOrNull()
        if (dbInstance == null) { onResult(false, "Firebase غير متوفر"); return }
        val data = mapOf(
            "appName" to config.appName,
            "appSlogan" to config.appSlogan,
            "logoUrl" to config.logoUrl,
            "onboardingBannerUrl" to config.onboardingBannerUrl,
            "accentColor" to config.accentColor,
            "privacyUrl" to config.privacyUrl,
            "contactEmail" to config.contactEmail,
            "officialWebsite" to config.officialWebsite,
            "appStoreUrl" to config.appStoreUrl,
            "iosStoreUrl" to config.iosStoreUrl,
            "facebookUrl" to config.facebookUrl,
            "youtubeUrl" to config.youtubeUrl,
            "telegramUrl" to config.telegramUrl,
            "tiktokUrl" to config.tiktokUrl,
            "instagramUrl" to config.instagramUrl,
            "twitterUrl" to config.twitterUrl,
            "whatsappUrl" to config.whatsappUrl,
            "defaultStreamUrl" to config.defaultStreamUrl,
            "minVersion" to config.minVersion,
            "latestVersion" to config.latestVersion,
            "updateUrl" to config.updateUrl,
            "updateMessage" to config.updateMessage,
            "primaryColor" to config.primaryColor,
            "secondaryColor" to config.secondaryColor,
            "maintenanceMode" to config.maintenanceMode,
            "maintenanceMessage" to config.maintenanceMessage,
            "enableOnboarding" to config.enableOnboarding,
            "enableEpg" to config.enableEpg,
            "showArchivePreview" to config.showArchivePreview,
            "showPromotionalBanners" to config.showPromotionalBanners,
            "enableArchive" to config.enableArchive,
            "enableSocial" to config.enableSocial,
            "enableWebsites" to config.enableWebsites,
            "enableComments" to config.enableComments,
            "enablePush" to config.enablePush,
            "enableDarkMode" to config.enableDarkMode,
            "enableDynamicColor" to config.enableDynamicColor
        )
        dbInstance.document("config/app").set(data)
            .addOnSuccessListener {
                _appConfig.value = config
                onResult(true, null)
            }
            .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }

    fun addStreamChannelRemote(stream: RemoteStream, onResult: (Boolean, String?) -> Unit) {
        val normalizedUrl = stream.url.trim()
        val isHttp = normalizedUrl.startsWith("https://", ignoreCase = true) || normalizedUrl.startsWith("http://", ignoreCase = true)
        val isYoutube = normalizedUrl.contains("youtube.com", ignoreCase = true) || normalizedUrl.contains("youtu.be", ignoreCase = true)
        val isHls = normalizedUrl.contains(".m3u8", ignoreCase = true)
        if (!isHttp || (!isYoutube && !isHls)) {
            onResult(false, "رابط البث غير صالح")
            return
        }
        val normalizedStream = stream.copy(url = normalizedUrl, type = if (isYoutube) "youtube" else "m3u8")
        val dbInstance = runCatching { db }.getOrNull()
        if (dbInstance == null) { onResult(false, "Firebase غير متوفر"); return }
        val data = mapOf(
            "title" to normalizedStream.title.trim(),
            "url" to normalizedStream.url,
            "type" to normalizedStream.type,
            "logoUrl" to normalizedStream.logoUrl.trim(),
            "isActive" to normalizedStream.isActive,
            "order" to normalizedStream.order
        )
        dbInstance.collection("streams").document(normalizedStream.id).set(data)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }

    fun deleteStreamChannelRemote(id: String, onResult: (Boolean, String?) -> Unit) {
        val dbInstance = runCatching { db }.getOrNull()
        if (dbInstance == null) { onResult(false, "Firebase غير متوفر"); return }
        dbInstance.collection("streams").document(id).delete()
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }

    fun updateStreamChannelRemote(stream: RemoteStream, onResult: (Boolean, String?) -> Unit) {
        val dbInstance = runCatching { db }.getOrNull()
        if (dbInstance == null) { onResult(false, "Firebase غير متوفر"); return }
        val data = mapOf(
            "title" to stream.title.trim(), "url" to stream.url.trim(), "type" to stream.type,
            "logoUrl" to stream.logoUrl.trim(), "isActive" to stream.isActive, "order" to stream.order
        )
        dbInstance.collection("streams").document(stream.id).set(data)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }

    fun addSocialPageRemote(page: SocialPage, onResult: (Boolean, String?) -> Unit) {
        writeCollectionDocument("social", page.id, mapOf(
            "platform" to page.platform, "name" to page.name, "url" to page.url,
            "description" to page.description, "logoUrl" to page.logoUrl,
            "emoji" to page.emoji, "color" to page.color,
            "order" to page.order, "isActive" to page.isActive
        ), onResult)
    }

    fun deleteSocialPageRemote(id: String, onResult: (Boolean, String?) -> Unit) = deleteDocument("social", id, onResult)

    fun addWebsiteRemote(item: WebsiteItem, onResult: (Boolean, String?) -> Unit) {
        writeCollectionDocument("websites", item.id, mapOf(
            "name" to item.name, "url" to item.url, "description" to item.description,
            "logoUrl" to item.logoUrl, "emoji" to item.emoji, "color" to item.color,
            "order" to item.order, "isActive" to item.isActive
        ), onResult)
    }

    fun deleteWebsiteRemote(id: String, onResult: (Boolean, String?) -> Unit) = deleteDocument("websites", id, onResult)

    fun addArchiveProgramRemote(item: ArchiveProgram, onResult: (Boolean, String?) -> Unit) {
        writeCollectionDocument("archive", item.id, mapOf(
            "title" to item.title, "description" to item.description, "category" to item.category,
            "youtubeUrl" to item.youtubeUrl, "thumbnailUrl" to item.thumbnailUrl,
            "date" to item.date, "duration" to item.duration,
            "isActive" to item.isActive, "isFeatured" to item.isFeatured,
            "publishAt" to item.publishAt?.let { com.google.firebase.Timestamp(java.util.Date(it)) },
            "expiresAt" to item.expiresAt?.let { com.google.firebase.Timestamp(java.util.Date(it)) }
        ), onResult)
    }

    fun deleteArchiveProgramRemote(id: String, onResult: (Boolean, String?) -> Unit) = deleteDocument("archive", id, onResult)

    fun addEpgProgramRemote(item: EpgItem, onResult: (Boolean, String?) -> Unit) {
        writeCollectionDocument("epg", item.id, mapOf(
            "startTime" to item.startTime, "title" to item.title, "category" to item.category,
            "duration" to item.duration, "description" to item.description
        ), onResult)
    }

    fun deleteEpgProgramRemote(id: String, onResult: (Boolean, String?) -> Unit) = deleteDocument("epg", id, onResult)

    private fun writeCollectionDocument(collection: String, id: String, data: Map<String, Any?>, onResult: (Boolean, String?) -> Unit) {
        val dbInstance = runCatching { db }.getOrNull()
        if (dbInstance == null) { onResult(false, "Firebase غير متوفر"); return }
        dbInstance.collection(collection).document(id).set(data + ("updatedAt" to com.google.firebase.Timestamp.now()))
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }

    private fun deleteDocument(collection: String, id: String, onResult: (Boolean, String?) -> Unit) {
        val dbInstance = runCatching { db }.getOrNull()
        if (dbInstance == null) { onResult(false, "Firebase غير متوفر"); return }
        dbInstance.collection(collection).document(id).delete()
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }

    fun addAdBannerRemote(banner: AdBanner, onResult: (Boolean, String?) -> Unit) {
        val dbInstance = runCatching { db }.getOrNull()
        if (dbInstance == null) { onResult(false, "Firebase غير متوفر"); return }
        dbInstance.collection("ad_banners").add(
            mapOf(
                "title" to banner.title.take(120),
                "imageUrl" to banner.imageUrl.take(1000),
                "targetUrl" to banner.targetUrl.take(1000),
                "isEnabled" to banner.isEnabled,
                "order" to 0,
                "createdAt" to com.google.firebase.Timestamp.now()
            )
        ).addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }

    fun deleteAdBannerRemote(id: String, onResult: (Boolean, String?) -> Unit) {
        val dbInstance = runCatching { db }.getOrNull()
        if (dbInstance == null) { onResult(false, "Firebase غير متوفر"); return }
        dbInstance.collection("ad_banners").document(id).delete()
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }

    fun toggleAdBannerRemote(id: String, enabled: Boolean, onResult: (Boolean, String?) -> Unit) {
        val dbInstance = runCatching { db }.getOrNull()
        if (dbInstance == null) { onResult(false, "Firebase غير متوفر"); return }
        dbInstance.collection("ad_banners").document(id).update("isEnabled", enabled)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }

    fun updatePopupAlertRemote(alert: PopupAlert, onResult: (Boolean, String?) -> Unit) {
        val dbInstance = runCatching { db }.getOrNull()
        if (dbInstance == null) { onResult(false, "Firebase غير متوفر"); return }
        dbInstance.document("config/popup").set(
            mapOf(
                "active" to alert.active,
                "title" to alert.title,
                "message" to alert.message,
                "buttonText" to alert.buttonText,
                "alertType" to alert.alertType
            )
        ).addOnSuccessListener {
            _popupAlert.value = alert
            onResult(true, null)
        }.addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }
}

/**
 * يستنتج منصة التواصل الاجتماعي من رابط الصفحة عندما لا يوفّر
 * المسؤول حقل platform في لوحة التحكم (مثلاً: facebook.com → Facebook).
 */
private fun inferSocialPlatform(url: String): String {
    val host = url.lowercase()
    return when {
        host.contains("facebook.com") || host.contains("fb.com") -> "Facebook"
        host.contains("youtube.com") || host.contains("youtu.be") -> "YouTube"
        host.contains("instagram.com") -> "Instagram"
        host.contains("x.com") || host.contains("twitter.com") -> "X"
        host.contains("tiktok.com") -> "TikTok"
        host.contains("t.me") || host.contains("telegram.org") -> "Telegram"
        host.contains("whatsapp.com") || host.contains("wa.me") -> "WhatsApp"
        host.contains("snapchat.com") -> "Snapchat"
        host.contains("discord.com") || host.contains("discord.gg") -> "Discord"
        host.contains("twitch.tv") -> "Twitch"
        host.contains("linkedin.com") -> "LinkedIn"
        else -> "أخرى"
    }
}

/** إشعار داخل التطبيق (يُدار من لوحة التحكم → تبويب الإشعارات). */
data class InAppNotification(
    val id: String,
    val title: String,
    val body: String,
    val youtubeUrl: String = "",
    val isActive: Boolean = true,
    val expiresAt: Long? = null,
    val createdAt: Long = 0L
)
