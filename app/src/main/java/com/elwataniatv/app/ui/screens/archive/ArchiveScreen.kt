package com.elwataniatv.app.ui.screens.archive

import android.annotation.SuppressLint
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.elwataniatv.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.elwataniatv.app.data.local.FavoriteProgram
import com.elwataniatv.app.data.model.ArchiveProgram
import com.elwataniatv.app.ui.components.VideoPlayerView
import com.elwataniatv.app.ui.screens.archive.ALL_CATEGORY
import com.elwataniatv.app.ui.theme.BrandAccent
import com.elwataniatv.app.ui.theme.BrandBorder
import com.elwataniatv.app.ui.theme.BrandPanel
import com.elwataniatv.app.ui.theme.BrandPrimary
import com.elwataniatv.app.ui.theme.BrandPillBg
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * أرشيف برامج الوطنية TV — display-only screen. All URL/category/sort
 * derivation lives in [ArchiveLogic.kt]; query & category filtering of the
 * incoming [programs] list is done upstream in MainViewModel.
 */
@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun ArchiveScreen(
    programs: List<ArchiveProgram>,
    modifier: Modifier = Modifier,
    favorites: List<FavoriteProgram>,
    selectedCategory: String,
    searchQuery: String,
    onSelectCategory: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleFavorite: (ArchiveProgram, Boolean) -> Unit,
    onSaveWatchProgress: (ArchiveProgram, Long, Long) -> Unit = { _, _, _ -> },
    onRetrySync: (() -> Unit)? = null,
    isLoading: Boolean = false,
    hasError: Boolean = false
) {
    var playingProgram by remember { mutableStateOf<ArchiveProgram?>(null) }
    var selectedDetailsProgram by remember { mutableStateOf<ArchiveProgram?>(null) }
    val context = LocalContext.current

    // Dynamically extract categories from real data to avoid showing static empty category filters
    val availableCategories = remember(programs) { extractAvailableCategories(programs) }

    // Sort programs by date or created order from newest to oldest
    val sortedPrograms = remember(programs) { sortArchivePrograms(programs) }
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val refreshScope = rememberCoroutineScope()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (!isRefreshing) {
                isRefreshing = true
                onRetrySync?.invoke()
                refreshScope.launch {
                    delay(800L)
                    isRefreshing = false
                }
            }
        },
        state = pullToRefreshState,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
        // Hero Header & Search Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BrandPillBg,
                            BrandPanel
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BrandPrimary.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = stringResource(R.string.archive_title),
                            tint = BrandAccent,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = stringResource(R.string.archive_title),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = stringResource(R.string.archive_subtitle),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                    }
                }

                if (programs.isNotEmpty()) {
                    Surface(
                        color = BrandAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, BrandAccent.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = pluralStringResource(R.plurals.archive_program_count, programs.size, programs.size),
                            color = BrandAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Quick Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        stringResource(R.string.archive_search_hint),
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 12.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search),
                        tint = BrandAccent
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.clear),
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("archive_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandAccent,
                    unfocusedBorderColor = BrandBorder,
                    focusedContainerColor = Color.Black.copy(alpha = 0.25f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.15f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // Dynamic Category Filter Pills (Only categories that actually exist in the fetched data)
            if (availableCategories.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("archive_category_row")
                ) {
                    items(availableCategories, key = { it }) { cat ->
                        val isSelected = cat == selectedCategory
                        val chipBg = if (isSelected) BrandPrimary else Color.White.copy(alpha = 0.08f)
                        val chipBorder = if (isSelected) BrandAccent else Color.Transparent
                        val categoryIcon = if (cat == ALL_CATEGORY) Icons.Default.VideoLibrary else getCategoryIcon(cat)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(chipBg)
                                .border(1.dp, chipBorder, RoundedCornerShape(20.dp))
                                .clickable { onSelectCategory(cat) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = categoryIcon,
                                    contentDescription = cat,
                                    tint = if (isSelected) BrandAccent else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (cat == ALL_CATEGORY) stringResource(R.string.all_categories) else cat,
                                    style = MaterialTheme.typography.labelMedium.copy(textDirection = TextDirection.ContentOrRtl),
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Playing Video Player Banner
        AnimatedVisibility(
            visible = playingProgram != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            playingProgram?.let { prog ->
                // Tracked position + duration so both updates are persisted
                // together (REPLACE would otherwise overwrite one another).
                var savedPos by remember(prog.id) { mutableLongStateOf(0L) }
                var savedDur by remember(prog.id) { mutableLongStateOf(0L) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(bottom = 8.dp)
                ) {
                    VideoPlayerView(
                        url = prog.youtubeUrl,
                        type = "youtube",
                        title = prog.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        onPositionUpdate = { pos ->
                            savedPos = pos
                            onSaveWatchProgress(prog, savedPos, savedDur)
                        },
                        onDurationUpdate = { dur ->
                            savedDur = dur
                            onSaveWatchProgress(prog, savedPos, savedDur)
                        }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = BrandPrimary.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(prog.category),
                                        contentDescription = prog.category,
                                        tint = BrandAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = prog.category,
                                        style = MaterialTheme.typography.labelSmall.copy(textDirection = TextDirection.ContentOrRtl),
                                        color = BrandAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = prog.title,
                                style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.ContentOrRtl),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = { playingProgram = null },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBorder),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(stringResource(R.string.close_player), color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Content States: Skeleton Loading, Error State, Empty State or Programs List
        when {
            isLoading -> {
                LazyColumn(
                    modifier = Modifier.testTag("archive_loading_state"),
                    contentPadding = PaddingValues(start = 14.dp, top = 10.dp, end = 14.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    items(5) {
                        ArchiveSkeletonCard()
                    }
                }
            }

            hasError -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("archive_error_state")
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.Red.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = stringResource(R.string.archive_load_error),
                                tint = Color.Red,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.archive_load_error_title),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.archive_load_error_message),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        if (onRetrySync != null) {
                            Button(
                                onClick = onRetrySync,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.retry), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.retry), color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            sortedPrograms.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("archive_empty_state")
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(BrandPanel),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = stringResource(R.string.archive_title),
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedCategory != ALL_CATEGORY) stringResource(R.string.archive_no_search_results) else stringResource(R.string.archive_no_programs),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedCategory != ALL_CATEGORY) stringResource(R.string.archive_search_tip) else stringResource(R.string.archive_admin_tip),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        if (onRetrySync != null && searchQuery.isBlank() && selectedCategory == ALL_CATEGORY) {
                            Button(
                                onClick = onRetrySync,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPanel),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh_data), modifier = Modifier.size(16.dp), tint = BrandAccent)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.refresh_data), color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 360.dp),
                    contentPadding = PaddingValues(start = 14.dp, top = 10.dp, end = 14.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("archive_program_list")
                ) {
                    items(sortedPrograms, key = { it.id }) { prog ->
                        val isFav = favorites.any { it.id == prog.id }

                        ArchiveProgramCard(
                            program = prog,
                            isFavorite = isFav,
                            onPlay = {
                                if (isValidVideoUrl(prog.youtubeUrl)) {
                                    onSaveWatchProgress(prog, 0L, 0L)
                                    playingProgram = prog
                                } else {
                                    Toast.makeText(context, context.getString(R.string.archive_video_unavailable), Toast.LENGTH_SHORT).show()
                                }
                            },
                            onCardClick = {
                                selectedDetailsProgram = prog
                            },
                            onToggleFavorite = {
                                onToggleFavorite(prog, isFav)
                                val msg = if (isFav) context.getString(R.string.favorite_removed) else context.getString(R.string.favorite_added)
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            onShare = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        context.getString(R.string.share_program_text, prog.title, prog.youtubeUrl)
                                    )
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.share_program)))
                            }
                        )
                    }
                }
            }
        }
        }
    }

    // Program Details Dialog / Modal Sheet
    selectedDetailsProgram?.let { prog ->
        val isFav = favorites.any { it.id == prog.id }
        val resolvedThumbnail = deriveThumbnailUrl(prog.thumbnailUrl, prog.youtubeUrl)
        var isImageError by remember(prog.id, resolvedThumbnail) { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { selectedDetailsProgram = null },
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = BrandPrimary.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(prog.category),
                                    contentDescription = prog.category,
                                    tint = BrandAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = prog.category,
                                    style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl),
                                    color = BrandAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    IconButton(onClick = { selectedDetailsProgram = null }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Preview Image Thumbnail with Play Overlay Badge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(BrandPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (resolvedThumbnail.isNotBlank() && !isImageError) {
                            SubcomposeAsyncImage(
                                model = resolvedThumbnail,
                                contentDescription = prog.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                loading = {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = BrandAccent, modifier = Modifier.size(28.dp))
                                    }
                                },
                                onError = {
                                    isImageError = true
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f))
                            )
                        } else {
                            // Elegant Badge Fallback with Material Icon
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(BrandPillBg, BrandPanel)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.OndemandVideo,
                                        contentDescription = prog.title,
                                        tint = BrandAccent.copy(alpha = 0.8f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(stringResource(R.string.app_name), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                }
                            }
                        }

                        // Play Button Overlay
                        IconButton(
                            onClick = {
                                if (isValidVideoUrl(prog.youtubeUrl)) {
                                    onSaveWatchProgress(prog, 0L, 0L)
                                    selectedDetailsProgram = null
                                    playingProgram = prog
                                } else {
                                    Toast.makeText(context, context.getString(R.string.archive_video_unavailable), Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(BrandPrimary.copy(alpha = 0.9f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = stringResource(R.string.play_video),
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    // Title
                    Text(
                        text = prog.title,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl),
                        textAlign = TextAlign.End
                    )

                    // Meta Row (Date & Duration)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (prog.date.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = stringResource(R.string.published_date),
                                    tint = BrandAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = prog.date,
                                    style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl),
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (prog.duration.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = stringResource(R.string.duration),
                                    tint = BrandAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = prog.duration,
                                    style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl),
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Description
                    if (prog.description.isNotBlank()) {
                        Text(
                            text = prog.description,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.ContentOrRtl),
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.End
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Watch Now Primary Action
                    Button(
                        onClick = {
                            if (isValidVideoUrl(prog.youtubeUrl)) {
                                onSaveWatchProgress(prog, 0L, 0L)
                                selectedDetailsProgram = null
                                playingProgram = prog
                            } else {
                                Toast.makeText(context, context.getString(R.string.archive_video_unavailable), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = stringResource(R.string.play), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.watch_video_in_app), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    // Favorite & Share Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onToggleFavorite(prog, isFav)
                                val msg = if (isFav) context.getString(R.string.favorite_removed) else context.getString(R.string.favorite_added)
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, BrandBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = stringResource(R.string.favorites),
                                tint = if (isFav) BrandAccent else Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFav) stringResource(R.string.in_favorites) else stringResource(R.string.add_to_favorites),
                                style = MaterialTheme.typography.labelMedium.copy(textDirection = TextDirection.ContentOrRtl),
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        context.getString(R.string.share_program_text, prog.title, prog.youtubeUrl)
                                    )
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.share_program)))
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, BrandBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.share),
                                style = MaterialTheme.typography.labelMedium.copy(textDirection = TextDirection.ContentOrRtl),
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = BrandPanel
        )
    }
}
