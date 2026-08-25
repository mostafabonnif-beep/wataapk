package com.elwataniatv.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.elwataniatv.app.R
import com.elwataniatv.app.data.local.WatchHistoryItem
import com.elwataniatv.app.ui.components.VideoPlayerView
import com.elwataniatv.app.ui.theme.BrandAccent
import com.elwataniatv.app.ui.theme.BrandPanel
import com.elwataniatv.app.ui.theme.BrandPrimary
import com.elwataniatv.app.ui.theme.BrandRed

@Composable
fun HistoryScreen(
    history: List<WatchHistoryItem>,
    modifier: Modifier = Modifier,
    onClear: () -> Unit,
    onSavePosition: (WatchHistoryItem, Long) -> Unit = { _, _ -> }
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var playingItem by remember { mutableStateOf<WatchHistoryItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(BrandPanel, Color(0xFF122A4C))))
                .border(1.dp, BrandAccent.copy(alpha = 0.20f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.History, contentDescription = null, tint = BrandAccent, modifier = Modifier.size(30.dp))
                Column {
                    Text(stringResource(R.string.history_title), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.history_subtitle), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
            if (history.isNotEmpty()) {
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.history_clear), tint = BrandRed)
                }
            }
        }

        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(52.dp))
                    Text(stringResource(R.string.history_empty), color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.history_empty_hint), color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(history, key = { it.id }) { item ->
                    val playDescription = stringResource(R.string.history_play_program, item.title)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(BrandPanel)
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .clickable { playingItem = item }
                            .semantics {
                                contentDescription = playDescription
                            }
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BrandAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = playDescription,
                                    tint = BrandAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.ContentOrRtl),
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.category,
                                    style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.ContentOrRtl),
                                    color = BrandAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // In-app playback dialog — the video opens inside the app (YouTube
    // embedded player), not in an external browser. Resumes from the last
    // saved position when available.
    playingItem?.let { item ->
        Dialog(
            onDismissRequest = { playingItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                VideoPlayerView(
                    url = item.youtubeUrl,
                    type = "youtube",
                    title = item.title,
                    initialPositionMs = item.positionMs,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    onPositionUpdate = { pos ->
                        if (pos > 0) onSavePosition(item, pos)
                    }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { playingItem = null },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.history_close_player), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.history_clear_title)) },
            text = { Text(stringResource(R.string.history_clear_message)) },
            confirmButton = {
                Button(
                    onClick = { onClear(); showClearDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) { Text(stringResource(R.string.history_clear_all)) }
            },
            dismissButton = { OutlinedButton(onClick = { showClearDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
