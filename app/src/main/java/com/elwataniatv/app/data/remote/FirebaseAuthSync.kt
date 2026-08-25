package com.elwataniatv.app.data.remote

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

/**
 * FirebaseAuthSync.kt
 * ─────────────────────────────────────────────────────────────────
 * Authentication concerns extracted from the old FirebaseSync.kt:
 *
 *   - lazy, guarded access to the [FirebaseAuth] instance (the app must
 *     keep working when Firebase is not initialised)
 *   - anonymous sign-in used by the whole sync layer (anonymous auth is
 *     the baseline identity for Firestore rules)
 *   - admin sign-in via email/password + `admin: true` custom claim check
 *
 * `FirebaseSync` composes this class with `FirestoreContentSync` and
 * `FcmTokenSync` and keeps the exact same public behaviour as before.
 * ─────────────────────────────────────────────────────────────────
 */
class FirebaseAuthSync {

    private companion object {
        private const val TAG = "FirebaseAuthSync"
    }

    /** Firebase Auth instance, or null when Firebase is unavailable/uninitialised. */
    val auth: FirebaseAuth? by lazy {
        runCatching { Firebase.auth }
            .onFailure { Log.w(TAG, "Firebase Auth غير متاح: ${it.message}") }
            .getOrNull()
    }

    /**
     * Ensures an anonymous session exists (signs in silently when needed)
     * then invokes [onReady]. When anonymous auth is disabled in the
     * Firebase console, [onReady] simply never runs — listeners that need
     * a uid stay dormant, exactly like the original implementation.
     */
    fun ensureAnonymousAuth(auth: FirebaseAuth, onReady: () -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null && !currentUser.isAnonymous) {
            onReady()
            return
        }
        if (currentUser != null) {
            onReady()
            return
        }
        auth.signInAnonymously()
            .addOnSuccessListener {
                Log.i(TAG, "دخول مجهول جاهز")
                onReady()
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "الدخول المجهول غير مفعّل في Firebase: ${error.message}")
            }
    }

    /** Runs [action] only when Firebase Auth is available and signed in. */
    fun withSignedIn(onFailure: (String) -> Unit, action: () -> Unit) {
        val authInstance = runCatching { auth }.getOrNull()
        if (authInstance == null) {
            onFailure("مصادقة Firebase غير متوفرة")
            return
        }
        ensureAnonymousAuth(authInstance) { action() }
    }

    /**
     * Signs an admin in with email/password and verifies the `admin: true`
     * custom claim on the ID token. Non-admin accounts are signed back out.
     */
    fun signInAdmin(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val authInstance = runCatching { auth }.getOrNull()
        if (authInstance == null) {
            onResult(false, "Firebase Auth غير متوفر")
            return
        }
        authInstance.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                result.user?.getIdToken(true)
                    ?.addOnSuccessListener { tokenResult ->
                        if (tokenResult.claims["admin"] == true) {
                            onResult(true, null)
                        } else {
                            authInstance.signOut()
                            onResult(false, "هذا الحساب ليس مسؤولاً مخوّلاً")
                        }
                    }
                    ?.addOnFailureListener { error ->
                        authInstance.signOut()
                        onResult(false, error.localizedMessage)
                    }
                    ?: run {
                        authInstance.signOut()
                        onResult(false, "تعذر التحقق من صلاحية المسؤول")
                    }
            }
            .addOnFailureListener { error ->
                onResult(false, error.localizedMessage)
            }
    }

    /** Signs the current user out (no-op when Firebase is unavailable). */
    fun signOutAdmin() {
        runCatching { auth?.signOut() }
    }
}
