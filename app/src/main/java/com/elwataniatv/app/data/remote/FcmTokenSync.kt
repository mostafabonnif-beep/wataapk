package com.elwataniatv.app.data.remote

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging

/**
 * FcmTokenSync.kt
 * ─────────────────────────────────────────────────────────────────
 * FCM registration concerns extracted from the old FirebaseSync.kt:
 *
 *   - writes this installation's push token to `push_tokens/{uid}` so the
 *     admin panel can target it
 *   - [registerFcmTokenNow] is a standalone entry point used by
 *     `FcmMessageService.onNewToken` when Firebase rotates the token —
 *     it does not need an active sync session
 *
 * `FirebaseSync` composes this class with `FirebaseAuthSync` and
 * `FirestoreContentSync` and keeps the exact same public behaviour.
 * ─────────────────────────────────────────────────────────────────
 */
class FcmTokenSync(private val authSync: FirebaseAuthSync) {

    companion object {
        private const val TAG = "FcmTokenSync"

        /**
         * Re-registers the FCM token whenever Firebase rotates it
         * (onNewToken). Standalone — does not need an active sync session.
         */
        fun registerFcmTokenNow() {
            FirebaseSync().registerFcmTokenIfPossible()
        }
    }

    private val db: FirebaseFirestore? by lazy {
        runCatching { Firebase.firestore }
            .onFailure { Log.w(TAG, "Firebase Firestore غير متاح: ${it.message}") }
            .getOrNull()
    }

    /**
     * Public wrapper used from FcmMessageService.onNewToken — works without
     * an active FirebaseSync session (auth + db are lazily resolved here).
     */
    fun registerFcmTokenIfPossible() {
        val dbInstance = runCatching { db }.getOrNull() ?: run {
            Log.w(TAG, "Firebase غير متوفر لتسجيل التوكن")
            return
        }
        val authInstance = runCatching { authSync.auth }.getOrNull() ?: run {
            Log.w(TAG, "مصادقة Firebase غير متوفرة لتسجيل التوكن")
            return
        }
        authSync.ensureAnonymousAuth(authInstance) {
            registerFcmToken(dbInstance)
        }
    }

    /** يسجّل توكن FCM في push_tokens حتى تصل الإشعارات من لوحة التحكم. */
    fun registerFcmToken(db: FirebaseFirestore) {
        runCatching {
            val installationId = com.elwataniatv.app.ElWataniaApp.installationId
            val userUid = authSync.auth?.currentUser?.uid ?: return@runCatching
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    db.collection("push_tokens").document(userUid).set(
                        mapOf(
                            "token" to token,
                            "deviceId" to installationId,
                            "userUid" to userUid,
                            "platform" to "android",
                            // No per-topic preference UI exists yet; explicitly opt in
                            // to all categories until the user can customize this list.
                            "categories" to listOf("all"),
                            "savedAt" to com.google.firebase.Timestamp.now(),
                        )
                    ).addOnSuccessListener { Log.i(TAG, "توكن FCM مسجّل ✅") }
                        .addOnFailureListener { e -> Log.w(TAG, "فشل تسجيل التوكن: ${e.message}") }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "فشل الحصول على توكن FCM: ${e.message}")
                }
        }.onFailure { e -> Log.w(TAG, "فشل registerFcmToken: ${e.message}") }
    }
}
