package com.elwataniatv.app.ui.components

import com.elwataniatv.app.data.model.EpgItem
import java.util.Calendar
import java.util.TimeZone

private val algeriaZone: TimeZone = TimeZone.getTimeZone("Africa/Algiers")
private val durationPattern = Regex("(\\d+)")

fun algeriaMinutesOfDay(): Int {
    val calendar = Calendar.getInstance(algeriaZone)
    return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
}

fun epgTimeMinutes(value: String): Int? {
    val parts = value.trim().split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

fun epgDurationMinutes(value: String): Int? = durationPattern.find(value)?.groupValues?.getOrNull(1)?.toIntOrNull()
    ?.takeIf { it in 1..(24 * 60) }

private fun elapsedMinutesSince(start: Int, now: Int): Int = (now - start + 24 * 60) % (24 * 60)

private fun isWithinProgram(start: Int, duration: Int, now: Int): Boolean =
    elapsedMinutesSince(start, now) < duration

fun currentEpgItem(items: List<EpgItem>, nowMinutes: Int): EpgItem? {
    val valid = items.mapNotNull { item -> epgTimeMinutes(item.startTime)?.let { it to item } }
    val durationAware = valid.filter { (start, item) ->
        epgDurationMinutes(item.duration)?.let { isWithinProgram(start, it, nowMinutes) } == true
    }
    return durationAware.maxByOrNull { (start, _) -> elapsedMinutesSince(start, nowMinutes) }?.second
        ?: valid.filter { (start, _) -> start <= nowMinutes }
            .maxByOrNull { (start, _) -> start }
            ?.second
}

fun nextEpgItem(items: List<EpgItem>, nowMinutes: Int): EpgItem? {
    val valid = items.mapNotNull { item -> epgTimeMinutes(item.startTime)?.let { it to item } }
    return valid
        .filter { (start, _) -> start > nowMinutes }
        .minByOrNull { (start, _) -> start }
        ?.second
        ?: valid.minByOrNull { (start, _) -> start }?.second
}

fun minutesUntilEpg(item: EpgItem?, nowMinutes: Int): Int? {
    val start = item?.let { epgTimeMinutes(it.startTime) } ?: return null
    val delta = start - nowMinutes
    return if (delta > 0) delta else delta + 24 * 60
}

fun minutesRemainingEpg(item: EpgItem?, nowMinutes: Int): Int? {
    val program = item ?: return null
    val start = epgTimeMinutes(program.startTime) ?: return null
    val duration = epgDurationMinutes(program.duration) ?: return null
    if (!isWithinProgram(start, duration, nowMinutes)) return null
    return (duration - elapsedMinutesSince(start, nowMinutes)).coerceAtLeast(0)
}

fun epgProgress(item: EpgItem?, nowMinutes: Int): Float? {
    val program = item ?: return null
    val start = epgTimeMinutes(program.startTime) ?: return null
    val duration = epgDurationMinutes(program.duration) ?: return null
    if (!isWithinProgram(start, duration, nowMinutes)) return null
    return (elapsedMinutesSince(start, nowMinutes).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
}
