package com.elwataniatv.app

import android.app.Application
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.elwataniatv.app.util.AppLanguage
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ElWataniaApp : Application() {

    companion object {
        @Volatile
        var installationId: String = ""
            private set
    }

    override fun onCreate() {
        super.onCreate()
        AppLanguage.initialize(this)
        ensureFirebaseInitialized()
        initObservability()
        initDeviceId()
    }

    private fun initObservability() {
        runCatching {
            if (FirebaseApp.getApps(this).isEmpty()) return
            FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)
            FirebaseCrashlytics.getInstance().apply {
                setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
                setCustomKey("app_version", BuildConfig.VERSION_NAME)
                setCustomKey("app_build", BuildConfig.VERSION_CODE)
                log("Application observability initialized")
            }
        }.onFailure { error ->
            Log.w("ElWataniaApp", "تعذر تهيئة المراقبة الإنتاجية: ${error.message}")
        }
    }

    private fun initDeviceId() {
        val prefs = getSharedPreferences("app_preferences", MODE_PRIVATE)
        installationId = prefs.getString("device_id", null)
            ?: java.util.UUID.randomUUID().toString()
                .also { prefs.edit().putString("device_id", it).apply() }
    }

    private fun ensureFirebaseInitialized() {
        runCatching {
            if (FirebaseApp.getApps(this).isEmpty()) {
                Log.i(
                    "ElWataniaApp",
                    "لم يتم العثور على google-services.json؛ يعمل التطبيق بالبيانات المحلية."
                )
            } else {
                Log.i("ElWataniaApp", "تمت تهيئة Firebase بنجاح")
            }
        }.onFailure { error ->
            Log.e("ElWataniaApp", "تنبيه تهيئة Firebase: ${error.message}")
        }
    }

    override fun getAttributionTag(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "default" else super.getAttributionTag()
    }

    override fun attachBaseContext(base: android.content.Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            super.attachBaseContext(base.createAttributionContext("default"))
        } else {
            super.attachBaseContext(base)
        }
    }
}
