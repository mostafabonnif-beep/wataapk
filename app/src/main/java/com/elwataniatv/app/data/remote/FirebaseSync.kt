package com.elwataniatv.app.data.remote

import android.util.Log
import com.elwataniatv.app.data.local.CommentEntity
import com.elwataniatv.app.data.model.AdBanner
import com.elwataniatv.app.data.model.ArchiveProgram
import com.elwataniatv.app.data.model.BreakingNews
import com.elwataniatv.app.data.model.EpgItem
import com.elwataniatv.app.data.model.PopupAlert
import com.elwataniatv.app.data.model.RemoteAppConfig
import com.elwataniatv.app.data.model.RemoteStream
import com.elwataniatv.app.data.model.SocialPage
import com.elwataniatv.app.data.model.SatelliteFrequency
import com.elwataniatv.app.data.model.WebsiteItem
import kotlinx.coroutines.flow.StateFlow

/**
 * FirebaseSync.kt
 * ─────────────────────────────────────────────────────────────────
 * Public facade for the Firestore sync layer. The original 997-line file
 * has been split by concern and this class keeps the exact same public
 * API (so repository / service callers need no changes):
 *
 *   FirebaseAuthSync      — anonymous sign-in, admin sign-in/out
 *   FirestoreContentSync  — snapshot listeners, StateFlows, Firestore writes
 *   FcmTokenSync          — FCM push-token registration
 *
 * The sync lifecycle (start / stop / reconnect) and the cross-cutting
 * "is Firebase available?" gate live here, matching the old behaviour:
 * listeners start only when both Auth and Firestore resolve.
 * ─────────────────────────────────────────────────────────────────
 */
class FirebaseSync {

    private val authSync = FirebaseAuthSync()
    private val contentSync = FirestoreContentSync(authSync)
    private val fcmSync = FcmTokenSync(authSync)

    private var started = false

    companion object {
        private const val TAG = "FirebaseSync"

        // Same order as the live reactions bar in the app
        val REACTION_EMOJIS = listOf("👍", "❤️", "😮", "⚽", "🇩🇿")

        /**
         * Re-registers the FCM token whenever Firebase rotates it
         * (onNewToken). Standalone — does not need an active sync session.
         */
        fun registerFcmTokenNow() = FcmTokenSync.registerFcmTokenNow()
    }

    // ─── StateFlows (تحدَّث من Firestore) ──────────────────────────
    val streams: StateFlow<List<RemoteStream>?> by contentSync::streams
    val breaking: StateFlow<BreakingNews?> by contentSync::breaking
    val appConfig: StateFlow<RemoteAppConfig?> by contentSync::appConfig
    val epg: StateFlow<List<EpgItem>?> by contentSync::epg
    val satelliteFrequencies: StateFlow<List<SatelliteFrequency>?> by contentSync::satelliteFrequencies
    val websites: StateFlow<List<WebsiteItem>?> by contentSync::websites
    val social: StateFlow<List<SocialPage>?> by contentSync::social
    val archive: StateFlow<List<ArchiveProgram>?> by contentSync::archive
    val comments: StateFlow<List<CommentEntity>?> by contentSync::comments
    val adBanners: StateFlow<List<AdBanner>?> by contentSync::adBanners
    val popupAlert: StateFlow<PopupAlert?> by contentSync::popupAlert
    val liveReactions: StateFlow<Map<String, Int>?> by contentSync::liveReactions
    val myReaction: StateFlow<String?> by contentSync::myReaction
    val inAppNotifications: StateFlow<List<InAppNotification>?> by contentSync::inAppNotifications
    val activeDevices: StateFlow<Int> by contentSync::activeDevices
    val syncError: StateFlow<String?> by contentSync::syncError
    val syncStatus: StateFlow<SyncStatus> by contentSync::syncStatus

    /** يبدأ الاستماع لكل المجموعات (يُستدعى مرة واحدة من ViewModel). */
    fun start(): Boolean {
        if (started) return true

        val authInstance = runCatching { authSync.auth }.getOrNull()
        val dbInstance = runCatching { contentSync.db }.getOrNull()

        if (authInstance == null || dbInstance == null) {
            Log.w(TAG, "Firebase غير متاح أو لم يتم تهيئته. التطبيق يعمل بالبيانات المحلية.")
            return false
        }

        started = true
        Log.i(TAG, "بدء الاستماع لتغييرات Firestore...")

        authSync.ensureAnonymousAuth(authInstance) {
            if (!started) return@ensureAnonymousAuth
            contentSync.listenMyReaction(dbInstance)
            contentSync.listenUserPreferences(dbInstance)
            fcmSync.registerFcmToken(dbInstance)
        }

        contentSync.listenStreams(dbInstance)
        contentSync.listenBreaking(dbInstance)
        contentSync.listenAppConfig(dbInstance)
        contentSync.listenEpg(dbInstance)
        contentSync.listenSatelliteFrequencies(dbInstance)
        contentSync.listenWebsites(dbInstance)
        contentSync.listenSocial(dbInstance)
        contentSync.listenArchive(dbInstance)
        contentSync.listenComments(dbInstance)
        contentSync.listenAdBanners(dbInstance)
        contentSync.listenPopupAlert(dbInstance)
        contentSync.listenLiveReactions(dbInstance)
        contentSync.listenNotifications(dbInstance)
        return true
    }

    /** يوقف كل المستمعين (يُستدعى عند إغلاق التطبيق). */
    fun stop() {
        contentSync.stop()
        started = false
    }

    /** إعادة تفعيل مستمعي البيانات عند عودة الاتصال بالمشبكة */
    fun reconnect() {
        Log.i(TAG, "إعادة الاتصال واسترجاع مستمعي Firestore...")
        stop()
        start()
    }

    // ─── التليمتري والتفاعلات ─────────────────────────────────────

    /** Writes this installation.s heartbeat to devices/{deviceId}. */
    fun sendHeartbeat() = contentSync.sendHeartbeat()

    /** Counts devices seen within the last [withinMinutes] (real viewers). */
    fun refreshActiveDevices(withinMinutes: Int = 15) = contentSync.refreshActiveDevices(withinMinutes)

    fun updateUserPreferences(
        darkModeEnabled: Boolean,
        pushEnabled: Boolean,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) = contentSync.updateUserPreferences(darkModeEnabled, pushEnabled, onResult)

    /** إرسال تعليق إلى Firestore مع التحقق من الهوية والأذونات. */
    fun postComment(
        programId: String = "live",
        authorName: String,
        text: String,
        onResult: (Boolean, String?) -> Unit
    ) = contentSync.postComment(programId, authorName, text, onResult)

    /** إرسال الاقتراحات والملاحظات إلى مجموعة feedback. */
    fun submitFeedback(
        type: String = "other",
        message: String,
        email: String = "",
        onResult: (Boolean, String?) -> Unit
    ) = contentSync.submitFeedback(type, message, email, onResult)

    // ─── العمليات الإدارية المباشرة مع Firestore ──────────────────

    fun updateBreakingNewsRemote(
        enabled: Boolean,
        text: String,
        youtubeUrl: String = "",
        onResult: (Boolean, String?) -> Unit
    ) = contentSync.updateBreakingNewsRemote(enabled, text, youtubeUrl, onResult)

    fun updateAppConfigRemote(config: RemoteAppConfig, onResult: (Boolean, String?) -> Unit) =
        contentSync.updateAppConfigRemote(config, onResult)

    fun addStreamChannelRemote(stream: RemoteStream, onResult: (Boolean, String?) -> Unit) =
        contentSync.addStreamChannelRemote(stream, onResult)

    fun deleteStreamChannelRemote(id: String, onResult: (Boolean, String?) -> Unit) =
        contentSync.deleteStreamChannelRemote(id, onResult)

    fun updateStreamChannelRemote(stream: RemoteStream, onResult: (Boolean, String?) -> Unit) =
        contentSync.updateStreamChannelRemote(stream, onResult)

    fun addSocialPageRemote(page: SocialPage, onResult: (Boolean, String?) -> Unit) =
        contentSync.addSocialPageRemote(page, onResult)

    fun deleteSocialPageRemote(id: String, onResult: (Boolean, String?) -> Unit) =
        contentSync.deleteSocialPageRemote(id, onResult)

    fun addWebsiteRemote(item: WebsiteItem, onResult: (Boolean, String?) -> Unit) =
        contentSync.addWebsiteRemote(item, onResult)

    fun deleteWebsiteRemote(id: String, onResult: (Boolean, String?) -> Unit) =
        contentSync.deleteWebsiteRemote(id, onResult)

    fun addArchiveProgramRemote(item: ArchiveProgram, onResult: (Boolean, String?) -> Unit) =
        contentSync.addArchiveProgramRemote(item, onResult)

    fun deleteArchiveProgramRemote(id: String, onResult: (Boolean, String?) -> Unit) =
        contentSync.deleteArchiveProgramRemote(id, onResult)

    fun addEpgProgramRemote(item: EpgItem, onResult: (Boolean, String?) -> Unit) =
        contentSync.addEpgProgramRemote(item, onResult)

    fun deleteEpgProgramRemote(id: String, onResult: (Boolean, String?) -> Unit) =
        contentSync.deleteEpgProgramRemote(id, onResult)

    fun addAdBannerRemote(banner: AdBanner, onResult: (Boolean, String?) -> Unit) =
        contentSync.addAdBannerRemote(banner, onResult)

    fun deleteAdBannerRemote(id: String, onResult: (Boolean, String?) -> Unit) =
        contentSync.deleteAdBannerRemote(id, onResult)

    fun toggleAdBannerRemote(id: String, enabled: Boolean, onResult: (Boolean, String?) -> Unit) =
        contentSync.toggleAdBannerRemote(id, enabled, onResult)

    fun updatePopupAlertRemote(alert: PopupAlert, onResult: (Boolean, String?) -> Unit) =
        contentSync.updatePopupAlertRemote(alert, onResult)

    // ─── المصادقة ─────────────────────────────────────────────────

    /** Signs an admin in with email/password and verifies the admin claim. */
    fun signInAdmin(email: String, password: String, onResult: (Boolean, String?) -> Unit) =
        authSync.signInAdmin(email, password, onResult)

    /** Signs the current user out (admin logout). */
    fun signOutAdmin() = authSync.signOutAdmin()

    // ─── FCM ──────────────────────────────────────────────────────

    /** Registers the FCM push token (used when no active sync session). */
    fun registerFcmTokenIfPossible() = fcmSync.registerFcmTokenIfPossible()
}
