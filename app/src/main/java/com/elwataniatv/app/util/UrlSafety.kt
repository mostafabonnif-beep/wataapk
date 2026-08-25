package com.elwataniatv.app.util

import android.net.Uri

/**
 * URL validation shared by embedded browsers and external intents.
 * Only network URLs with a non-empty host are accepted; custom schemes
 * (intent:, javascript:, file:, content:, and tel:) are rejected.
 */
fun safeHttpUri(rawUrl: String): Uri? {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) return null
    val uri = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase() ?: return null
    if (scheme != "http" && scheme != "https") return null
    if (uri.host.isNullOrBlank()) return null
    return uri
}

fun isSafeHttpUrl(rawUrl: String): Boolean = safeHttpUri(rawUrl) != null

fun isAllowedHost(uri: Uri, allowedHosts: Set<String>): Boolean {
    val host = uri.host?.lowercase() ?: return false
    return allowedHosts.any { allowed ->
        val normalized = allowed.lowercase().removePrefix("www.")
        host == normalized || host.endsWith(".$normalized")
    }
}
