package com.elwataniatv.app.data.model

/**
 * Returns the next configured, active stream after [currentStreamId].
 *
 * Only URLs already supplied by Firestore are considered. Blank URLs and the
 * currently failing stream are never returned, and the list is not wrapped so
 * a failed stream cannot start an endless fallback loop.
 */
fun nextFallbackStream(
    currentStreamId: String?,
    streams: List<RemoteStream>,
): RemoteStream? {
    val candidates = streams.filter { it.isActive && it.url.isNotBlank() }
    val currentIndex = candidates.indexOfFirst { it.id == currentStreamId }
    val startIndex = if (currentIndex >= 0) currentIndex + 1 else 0
    return candidates.drop(startIndex).firstOrNull { it.id != currentStreamId }
}
