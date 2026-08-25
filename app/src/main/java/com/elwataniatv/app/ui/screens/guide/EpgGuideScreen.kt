package com.elwataniatv.app.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elwataniatv.app.R
import com.elwataniatv.app.data.local.ProgramReminder
import com.elwataniatv.app.data.model.EpgItem
import com.elwataniatv.app.ui.components.algeriaMinutesOfDay
import com.elwataniatv.app.ui.components.currentEpgItem
import com.elwataniatv.app.ui.components.minutesRemainingEpg
import com.elwataniatv.app.ui.components.nextEpgItem
import com.elwataniatv.app.ui.theme.BrandAccent
import com.elwataniatv.app.ui.theme.BrandBg
import com.elwataniatv.app.ui.theme.BrandPanel
import com.elwataniatv.app.ui.theme.BrandPrimary
import com.elwataniatv.app.ui.theme.BrandRed
import com.elwataniatv.app.ui.theme.TextSecondary
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Full program guide: the whole day's schedule in one scrollable list.
 *
 * Unlike the compact home strip (which shows only now/next), this screen
 * lists every active program of the selected day, highlights the current
 * one, marks past ones as finished, and lets the user toggle reminders or
 * open a program details dialog. Day tabs (yesterday / today / tomorrow)
 * use Algeria time.
 */
@Composable
fun EpgGuideScreen(
    epgList: List<EpgItem>,
    reminders: List<ProgramReminder>,
    onToggleReminder: (EpgItem, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nowMinutes by remember { mutableIntStateOf(algeriaMinutesOfDay()) }
    // 0 = today, -1 = yesterday, +1 = tomorrow
    var dayOffset by remember { mutableIntStateOf(0) }
    var selectedProgram by remember { mutableStateOf<EpgItem?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            nowMinutes = algeriaMinutesOfDay()
        }
    }

    val sortedPrograms = remember(epgList) {
        epgList
            .filter { it.isActive }
            .sortedWith(compareBy({ it.startTime }, { it.order }))
    }
    val currentItem = remember(sortedPrograms, nowMinutes, dayOffset) {
        if (dayOffset == 0) currentEpgItem(sortedPrograms, nowMinutes) else null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBg)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.back),
                    tint = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.epg_today),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = stringResource(R.string.epg_algeria_time),
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Day selector
        DaySelector(
            dayOffset = dayOffset,
            onDayChange = { dayOffset = it }
        )

        if (sortedPrograms.isEmpty()) {
            EmptyGuide()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                items(sortedPrograms, key = { it.id }) { program ->
                    GuideRow(
                        program = program,
                        isCurrent = program.id == currentItem?.id,
                        isPast = dayOffset == 0 && program.startTime < minutesToTime(nowMinutes),
                        hasReminder = reminders.any { it.id == program.id },
                        onClick = { selectedProgram = program }
                    )
                }
            }
        }
    }

    // Program details dialog
    selectedProgram?.let { program ->
        ProgramDetailsDialog(
            program = program,
            hasReminder = reminders.any { it.id == program.id },
            onToggleReminder = { isSet ->
                onToggleReminder(program, isSet)
                selectedProgram = null
            },
            onDismiss = { selectedProgram = null }
        )
    }
}

private fun minutesToTime(minutes: Int): String {
    val h = (minutes / 60).coerceIn(0, 23)
    val m = minutes % 60
    return String.format(Locale.US, "%02d:%02d", h, m)
}

@Composable
private fun DaySelector(dayOffset: Int, onDayChange: (Int) -> Unit) {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("Africa/Algiers"))
    cal.add(Calendar.DAY_OF_YEAR, dayOffset)
    val dayLabel = when (dayOffset) {
        -1 -> stringResource(R.string.guide_yesterday)
        1 -> stringResource(R.string.guide_tomorrow)
        else -> stringResource(R.string.guide_today)
    }
    val dateLabel = String.format(
        Locale.US,
        "%02d/%02d",
        cal.get(Calendar.DAY_OF_MONTH),
        cal.get(Calendar.MONTH) + 1
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onDayChange((dayOffset - 1).coerceAtLeast(-1)) },
            enabled = dayOffset > -1
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.guide_yesterday),
                tint = if (dayOffset > -1) Color.White else Color.White.copy(alpha = 0.25f)
            )
        }
        Surface(
            color = BrandPanel,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BrandAccent.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = dayLabel,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateLabel,
                    color = BrandAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        IconButton(
            onClick = { onDayChange((dayOffset + 1).coerceAtMost(1)) },
            enabled = dayOffset < 1
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.guide_tomorrow),
                tint = if (dayOffset < 1) Color.White else Color.White.copy(alpha = 0.25f)
            )
        }
    }
}

@Composable
private fun GuideRow(
    program: EpgItem,
    isCurrent: Boolean,
    isPast: Boolean,
    hasReminder: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = if (isCurrent) BrandPrimary.copy(alpha = 0.16f) else BrandPanel.copy(alpha = 0.7f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isCurrent) 1.5.dp else 1.dp,
            color = if (isCurrent) BrandRed.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.06f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time column
            Column(
                modifier = Modifier.width(64.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = program.startTime,
                    color = if (isCurrent) BrandRed else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
                if (program.duration.isNotBlank()) {
                    Text(
                        text = program.duration,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            // Title + category
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = program.title.ifBlank { stringResource(R.string.epg_unknown_program) },
                    color = if (isPast) Color.White.copy(alpha = 0.45f) else Color.White,
                    fontSize = 14.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl)
                )
                if (program.category.isNotBlank()) {
                    Text(
                        text = program.category,
                        color = BrandAccent.copy(alpha = if (isPast) 0.5f else 0.9f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Status / reminder
            if (isCurrent) {
                Surface(
                    color = BrandRed,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.epg_now_playing),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            } else if (hasReminder) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = stringResource(R.string.epg_reminder_content_description, program.title),
                    tint = BrandAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyGuide() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Category,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.home_no_schedule),
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun ProgramDetailsDialog(
    program: EpgItem,
    hasReminder: Boolean,
    onToggleReminder: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BrandPanel,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = program.title.ifBlank { stringResource(R.string.epg_unknown_program) },
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl)
                )
                if (program.category.isNotBlank()) {
                    Surface(
                        color = BrandAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = program.category,
                            color = BrandAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Time & duration row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = BrandAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = program.startTime,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (program.duration.isNotBlank()) {
                            Text(
                                text = "• ${program.duration}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (program.description.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = BrandAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = program.description,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl)
                        )
                    }
                }

                // Reminder status
                Text(
                    text = if (hasReminder) {
                        stringResource(R.string.epg_reminder_enabled, program.title, program.startTime)
                    } else {
                        stringResource(R.string.epg_set_reminder)
                    },
                    color = if (hasReminder) BrandAccent else TextSecondary,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onToggleReminder(!hasReminder) }) {
                Icon(
                    imageVector = if (hasReminder) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                    contentDescription = null,
                    tint = BrandAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        if (hasReminder) R.string.epg_cancel_reminder else R.string.epg_set_reminder
                    ),
                    color = BrandAccent
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close), color = BrandAccent)
            }
        }
    )
}
