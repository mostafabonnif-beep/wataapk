package com.elwataniatv.app.ui.screens.archive

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.elwataniatv.app.R
import com.elwataniatv.app.data.model.ArchiveProgram
import com.elwataniatv.app.data.local.WatchHistoryItem
import com.elwataniatv.app.util.extractYouTubeVideoId
import com.elwataniatv.app.util.isYouTubeVideoUrl

const val ALL_CATEGORY = "__all__"

/**
 * ArchiveLogic.kt
 * ─────────────────────────────────────────────────────────────────
 * Pure, side-effect-free archive logic extracted from the old
 * ArchiveScreen.kt so the UI file only composes display. Nothing here
 * touches Compose runtime state or the network — all functions are
 * deterministic and trivially unit-testable.
 *
 * Note: query/category *filtering* of the archive list itself lives in
 * MainViewModel (filteredArchive); these helpers cover URL validation,
 * thumbnail derivation, category icons and the small derivations the
 * screen used to inline in `remember` blocks.
 * ─────────────────────────────────────────────────────────────────
 */

private val directVideoExtensions = listOf("mp4", "m3u8", "webm", "mkv", "mov", "m4v", "3gp", "ts")

/**
 * Whether the archive item contains a playable video URL: either a
 * YouTube link or a direct progressive/HLS media link the ExoPlayer
 * engine can handle natively.
 */
fun isValidVideoUrl(url: String): Boolean {
    val trimmed = url.trim()
    if (isYouTubeVideoUrl(trimmed)) return true
    if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return false
    val path = trimmed.substringBefore('?').substringBefore('#')
    val extension = path.substringAfterLast('.', "").lowercase()
    return extension in directVideoExtensions
}

/**
 * The engine type to pass to VideoPlayerView: "youtube" renders in the
 * WebView path, while direct links fall through to ExoPlayer — the
 * player detects HLS from the URL automatically.
 */
fun archiveVideoType(url: String): String =
    if (isYouTubeVideoUrl(url.trim())) "youtube" else "mp4"

/**
 * Resolves the thumbnail to show: the stored thumbnail when it is an
 * http(s) URL, otherwise the YouTube `hqdefault` thumbnail derived from
 * the video id, otherwise an empty string (UI falls back to a badge).
 */
fun deriveThumbnailUrl(thumbnailUrl: String, youtubeUrl: String): String {
    val trimmedThumb = thumbnailUrl.trim()
    if (trimmedThumb.isNotBlank() && (trimmedThumb.startsWith("http://") || trimmedThumb.startsWith("https://"))) {
        return trimmedThumb
    }
    val videoId = extractYouTubeVideoId(youtubeUrl)
    if (!videoId.isNullOrBlank()) {
        return "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
    }
    return ""
}

/** Material icon used for a program category (falls back to a generic video icon). */
fun getCategoryIcon(category: String): ImageVector {
    val lower = category.trim().lowercase()
    return when {
        lower.contains("أخبار") || lower.contains("اخبار") || lower.contains("نشرة") || lower.contains("نشرات") || lower.contains("news") || lower.contains("actualité") -> Icons.Default.Newspaper
        lower.contains("رياضة") || lower.contains("رياضي") || lower.contains("sport") -> Icons.Default.Sports
        lower.contains("وثائقي") || lower.contains("وثائقية") || lower.contains("document") -> Icons.Default.Movie
        lower.contains("ثقافة") || lower.contains("ثقافي") || lower.contains("cultur") -> Icons.AutoMirrored.Filled.MenuBook
        lower.contains("ترفيه") || lower.contains("ترفيهي") || lower.contains("entertain") || lower.contains("divert") -> Icons.Default.Theaters
        else -> Icons.Default.OndemandVideo
    }
}

/** Localized display label for standard categories. */
@Composable
fun getLocalizedCategoryName(category: String): String {
    if (category == ALL_CATEGORY) return stringResource(R.string.all_categories)
    val lower = category.trim().lowercase()
    return when {
        lower == "أخبار" || lower == "اخبار" || lower == "news" || lower == "actualités" -> stringResource(R.string.category_news)
        lower == "رياضة" || lower == "sports" || lower == "sport" -> stringResource(R.string.category_sports)
        lower == "ثقافة" || lower == "culture" -> stringResource(R.string.category_culture)
        lower == "اقتصاد" || lower == "economy" || lower == "économie" -> stringResource(R.string.category_economy)
        lower == "وثائقي" || lower == "وثائقية" || lower == "documentary" || lower == "documentaires" -> stringResource(R.string.category_documentary)
        lower == "ترفيه" || lower == "entertainment" || lower == "divertissement" -> stringResource(R.string.category_entertainment)
        lower == "برامج" || lower == "programs" || lower == "programmes" -> stringResource(R.string.category_programs)
        lower == "أخرى" || lower == "other" || lower == "autre" -> stringResource(R.string.category_other)
        else -> category
    }
}

/**
 * Categories present in the real data, with a stable all-category key
 * prepended — avoids coupling filtering logic to a translated label and
 * avoids showing static category filters that match nothing.
 */
fun extractAvailableCategories(programs: List<ArchiveProgram>): List<String> {
    val extracted = programs
        .map { it.category }
        .filter { it.isNotBlank() && it != ALL_CATEGORY }
        .distinct()
    return listOf(ALL_CATEGORY) + extracted
}

/** Programs sorted from newest to oldest by date. */
fun sortArchivePrograms(programs: List<ArchiveProgram>): List<ArchiveProgram> =
    programs.sortedByDescending { it.date }

/** Returns a useful unfinished position, or zero when the item should start over. */
fun resumePositionMs(history: List<WatchHistoryItem>, programId: String): Long {
    val item = history.firstOrNull { it.id == programId } ?: return 0L
    val position = item.positionMs.coerceAtLeast(0L)
    val duration = item.durationMs.coerceAtLeast(0L)
    return position.takeIf {
        it > 10_000L && (duration <= 0L || it < duration - 5_000L)
    } ?: 0L
}