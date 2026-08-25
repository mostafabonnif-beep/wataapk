package com.elwataniatv.app.ui.components

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.elwataniatv.app.R
import androidx.compose.ui.unit.sp
import com.elwataniatv.app.data.local.ProgramReminder
import com.elwataniatv.app.data.model.EpgItem
import com.elwataniatv.app.ui.theme.BrandAccent
import com.elwataniatv.app.ui.theme.BrandPanel
import com.elwataniatv.app.ui.theme.BrandPrimary
import com.elwataniatv.app.ui.theme.BrandRed

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun EpgStrip(
    epgList: List<EpgItem>,
    reminders: List<ProgramReminder>,
    onToggleReminder: (EpgItem, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Current time (minutes of day), refreshed every minute so the
    // "يعرض الآن" highlight stays correct.
    var nowMinutes by remember { mutableIntStateOf(algeriaMinutesOfDay()) }
    var selectedProgram by remember { mutableStateOf<EpgItem?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            nowMinutes = algeriaMinutesOfDay()
        }
    }

    // The current program is the latest item whose start time has passed;
    // the next program is the soonest item still in the future.
    val currentItem = remember(epgList, nowMinutes) {
        currentEpgItem(epgList, nowMinutes)
    }
    val nextItem = remember(epgList, nowMinutes) {
        nextEpgItem(epgList, nowMinutes)
    }
    val minutesUntilNext = remember(nextItem, nowMinutes) {
        minutesUntilEpg(nextItem, nowMinutes)
    }
    val currentProgress = remember(currentItem, nowMinutes) {
        epgProgress(currentItem, nowMinutes)
    }
    val currentRemaining = remember(currentItem, nowMinutes) {
        minutesRemainingEpg(currentItem, nowMinutes)
    }
    val highlightedItem = currentItem ?: nextItem

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.epg_today),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.epg_algeria_time),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("epg_next_program_card")
                .heightIn(min = 78.dp),
            color = BrandPanel,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BrandAccent.copy(alpha = 0.22f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = BrandAccent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play), tint = BrandAccent, modifier = Modifier.size(20.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(if (currentItem != null) R.string.epg_now_playing else R.string.epg_next_program),
                        style = LocalTextStyle.current.copy(textDirection = TextDirection.ContentOrRtl),
                        color = BrandAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = highlightedItem?.title ?: stringResource(R.string.epg_no_upcoming),
                        style = LocalTextStyle.current.copy(textDirection = TextDirection.ContentOrRtl),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (currentItem != null && currentProgress != null) {
                        LinearProgressIndicator(
                            progress = { currentProgress },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = BrandAccent,
                            trackColor = Color.White.copy(alpha = 0.12f)
                        )
                    }
                }
                Text(
                    text = if (currentItem != null) {
                        currentRemaining?.let { pluralStringResource(R.plurals.epg_remaining_minutes, it, it) }
                            ?: currentItem.startTime
                    } else {
                        minutesUntilNext?.let { pluralStringResource(R.plurals.epg_starts_in_minutes, it, it) }
                            ?: nextItem?.startTime.orEmpty()
                    },
                    style = LocalTextStyle.current.copy(textDirection = TextDirection.ContentOrRtl),
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val epgCardWidth = when {
                maxWidth >= 1200.dp -> 260.dp
                maxWidth >= 600.dp -> 220.dp
                else -> 180.dp
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.testTag("epg_strip_row")
            ) {
            items(epgList, key = { it.id }) { item ->
                val isReminderSet = reminders.any { it.id == item.id }
                val isNow = item.id == currentItem?.id
                val displayTitle = item.title
                    .takeIf { title -> title.isNotBlank() && title.any { character -> !character.isDigit() } }
                    ?: context.getString(R.string.epg_unknown_program)

                Box(
                    modifier = Modifier
                        .width(epgCardWidth)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { selectedProgram = item }
                        .testTag("epg_program_card_${item.id}")
                        .background(if (isNow) BrandRed.copy(alpha = 0.16f) else BrandPanel)
                        .border(
                            width = 2.dp,
                            color = when {
                                isNow -> BrandRed
                                isReminderSet -> BrandAccent
                                else -> Color.White.copy(alpha = 0.1f)
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (isNow) BrandRed else BrandPrimary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = item.startTime,
                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.ContentOrRtl),
                                    color = if (isNow) Color.White else BrandAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    onToggleReminder(item, isReminderSet)
                                    val msg = if (isReminderSet) {
                                        context.getString(R.string.epg_reminder_cancelled, displayTitle)
                                    } else {
                                        context.getString(R.string.epg_reminder_enabled, displayTitle, item.startTime)
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (isReminderSet) Icons.Default.Notifications else Icons.Outlined.NotificationsNone,
                                    contentDescription = stringResource(R.string.epg_reminder_content_description, displayTitle),
                                    tint = if (isReminderSet) BrandAccent else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        if (isNow) {
                            Surface(
                                color = BrandRed,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.epg_now_playing),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = displayTitle,
                            style = LocalTextStyle.current.copy(textDirection = TextDirection.ContentOrRtl),
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = item.description.ifEmpty { item.duration },
                            style = LocalTextStyle.current.copy(textDirection = TextDirection.ContentOrRtl),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                }
            }
        }

        selectedProgram?.let { program ->
            val isReminderSet = reminders.any { it.id == program.id }
            AlertDialog(
                onDismissRequest = { selectedProgram = null },
                title = {
                    Text(
                        text = program.title,
                        style = LocalTextStyle.current.copy(textDirection = TextDirection.ContentOrRtl),
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${program.startTime}${if (program.duration.isNotBlank()) " · ${program.duration}" else ""}",
                            style = LocalTextStyle.current.copy(textDirection = TextDirection.ContentOrRtl),
                            color = BrandAccent,
                            fontWeight = FontWeight.Bold
                        )
                            if (program.category.isNotBlank()) {
                            Text(
                                text = program.category,
                                style = LocalTextStyle.current.copy(textDirection = TextDirection.ContentOrRtl),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = program.description.ifBlank { stringResource(R.string.epg_no_upcoming) },
                            style = LocalTextStyle.current.copy(textDirection = TextDirection.ContentOrRtl),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onToggleReminder(program, isReminderSet)
                            selectedProgram = null
                        }
                    ) {
                        Text(
                            text = stringResource(
                                if (isReminderSet) R.string.epg_cancel_reminder else R.string.epg_set_reminder
                            )
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedProgram = null }) {
                        Text(stringResource(R.string.close))
                    }
                }
            )
        }
    }
    }
}
