package com.elwataniatv.app.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class RemoteAppConfig(
    val appName: String = "",
    val appSlogan: String = "",
    val logoUrl: String = "",
    val onboardingBannerUrl: String = "",
    val accentColor: String = "",
    val privacyUrl: String = "",
    val contactEmail: String = "",
    val officialWebsite: String = "",
    val appStoreUrl: String = "",
    val iosStoreUrl: String = "",
    val facebookUrl: String = "",
    val youtubeUrl: String = "",
    val telegramUrl: String = "",
    val tiktokUrl: String = "",
    val instagramUrl: String = "",
    val twitterUrl: String = "",
    val whatsappUrl: String = "",
    val defaultStreamUrl: String = "",
    val minVersion: String = "",
    val latestVersion: String = "",
    val updateUrl: String = "",
    val updateMessage: String = "",
    val primaryColor: String = "",
    val secondaryColor: String = "",
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "",
    val enableOnboarding: Boolean = true,
    val enableEpg: Boolean = true,
    val showArchivePreview: Boolean = true,
    val showPromotionalBanners: Boolean = true,
    val enableArchive: Boolean = true,
    val enableSocial: Boolean = true,
    val enableWebsites: Boolean = true,
    val enableComments: Boolean = true,
    val enablePush: Boolean = true,
    val enableDarkMode: Boolean = true,
    val enableDynamicColor: Boolean = false
)
