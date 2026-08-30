plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
}

// The Google Services plugin (FCM / google_app_id resources) is applied
// ONLY when google-services.json is present. This keeps CI green without
// the secret (see .github/workflows/ci.yml) while still wiring Firebase
// fully when the file exists (local builds / releases).
val googleServicesFile = rootProject.file("google-services.json")
val appGoogleServicesFile = file("google-services.json")
val hasGoogleServices = googleServicesFile.exists() || appGoogleServicesFile.exists()
if (hasGoogleServices) {
    if (googleServicesFile.exists() && !appGoogleServicesFile.exists()) {
        // The CI workflow writes the secret to the repo root, but the Google
        // Services plugin only searches the app module — mirror it locally.
        copy {
            from(googleServicesFile)
            into(projectDir)
        }
    }
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
} else if (gradle.startParameter.taskNames.any { it.contains("bundleRelease", ignoreCase = true) }) {
    // Play Store delivery (AAB) must point at the real production Firebase
    // project — refusing to build prevents an accidental silent offline app.
    throw GradleException(
        "bundleRelease requires app/google-services.json for the production Firebase project."
    )
} else if (gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }) {
    // A signed APK can still be assembled for internal testing when the
    // Firebase config is absent: the app degrades gracefully to local data.
    logger.warn(
        "No google-services.json found — release APK will run in offline " +
            "fallback mode (no Firestore/FCM). Add app/google-services.json " +
            "to enable the production Firebase project."
    )
}

android {
    namespace = "com.elwataniatv.app"
    compileSdk = 36

    // In-app language switching (Arabic/English) must work inside Play App
    // Bundles: keep all locale resources in the base module instead of
    // splitting them, otherwise a device running "en" cannot switch to
    // Arabic without Play Core downloading the split on demand.
    bundle {
        language {
            enableSplit = false
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }

    defaultConfig {
        applicationId = "com.elwataniatv.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 36
        versionName = "8.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Release signing is opt-in. Values may come from Gradle properties or
    // environment variables; no secret or keystore is stored in this repo.
    fun signingValue(name: String): String? =
        providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }
            ?: providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }

    val releaseStoreFile = signingValue("RELEASE_STORE_FILE")
    val releaseStorePassword = signingValue("RELEASE_STORE_PASSWORD")
    val releaseKeyAlias = signingValue("RELEASE_KEY_ALIAS")
    val releaseKeyPassword = signingValue("RELEASE_KEY_PASSWORD")
    val hasReleaseSigning = listOf(
        releaseStoreFile,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword
    ).all { it != null }

    signingConfigs {
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (hasReleaseSigning) {
            create("releaseConfig") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword!!
                keyAlias = releaseKeyAlias!!
                keyPassword = releaseKeyPassword!!
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("releaseConfig")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Use the committed debug.keystore when present (stable debug signature
            // across machines / CI); otherwise fall back to the auto-generated
            // ~/.android/debug.keystore so a fresh clone always builds.
            signingConfig = if (file("${rootDir}/debug.keystore").exists()) {
                signingConfigs.getByName("debugConfig")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }



    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INDEX/AL2.0"
            excludes += "/META-INDEX/LGPL2.1"
        }
    }

    // lintDebug/lint run in CI and locally; skip the duplicate lintVital
    // pass on release assembles to keep constrained builders (3.5-4 GB
    // cgroup) from being OOM-killed after R8 minification.
    lint {
        checkReleaseBuilds = false
        abortOnError = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // Android TV optimized Material components and focus-aware foundations.
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)

    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // Firebase (versions managed by BOM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.appcheck)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.play.review)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
