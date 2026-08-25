package com.elwataniatv.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.elwataniatv.app.R
import com.elwataniatv.app.data.model.WebsiteItem
import com.elwataniatv.app.ui.components.WebBrowserView
import com.elwataniatv.app.ui.theme.BrandAccent
import com.elwataniatv.app.util.safeHttpUri
import com.elwataniatv.app.ui.theme.BrandBorder
import com.elwataniatv.app.ui.theme.BrandPanel
import com.elwataniatv.app.ui.theme.BrandPrimary
import com.elwataniatv.app.ui.theme.BrandPillBg

private fun extractDomainName(url: String): String {
    return try {
        val trimmed = url.trim()
        val uri = java.net.URI(trimmed)
        val host = uri.host ?: trimmed
        host.removePrefix("www.")
    } catch (e: Exception) {
        url.trim().removePrefix("https://").removePrefix("http://").removePrefix("www.").split("/").firstOrNull() ?: url
    }
}

private fun isValidWebsiteUrl(url: String): Boolean = safeHttpUri(url) != null

private fun deriveLogoUrl(site: WebsiteItem, domainName: String): String {
    if (safeHttpUri(site.logoUrl) != null) {
        return site.logoUrl
    }
    if (domainName.isNotBlank() && domainName.contains(".")) {
        return "https://www.google.com/s2/favicons?domain=$domainName&sz=128"
    }
    return ""
}

@Composable
fun WebsitesScreen(
    websites: List<WebsiteItem>,
    selectedWebsite: WebsiteItem?,
    onSelectWebsite: (WebsiteItem) -> Unit,
    onCloseWebsite: () -> Unit,
    onRetrySync: (() -> Unit)? = null,
    isLoading: Boolean = false,
    hasError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Safely filter and sort valid active websites by order from Firestore
    val validWebsites = remember(websites) {
        websites
            .filter { it.isActive && isValidWebsiteUrl(it.url) }
            .sortedBy { it.order }
    }

    if (selectedWebsite != null) {
        WebBrowserView(
            website = selectedWebsite,
            onClose = onCloseWebsite,
            modifier = modifier
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Hero Screen Header
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
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
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
                                imageVector = Icons.Default.Newspaper,
                                contentDescription = stringResource(R.string.websites_title),
                                tint = BrandAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = stringResource(R.string.websites_title),
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 15.sp,
                                lineHeight = 19.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                            Text(
                                text = stringResource(R.string.websites_subtitle),
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                lineHeight = 12.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                    }

                    if (validWebsites.isNotEmpty()) {
                        Surface(
                            color = BrandAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BrandAccent.copy(alpha = 0.3f)),
                            modifier = Modifier.widthIn(min = 76.dp)
                        ) {
                            Text(
                                text = pluralStringResource(R.plurals.websites_count, validWebsites.size, validWebsites.size),
                                color = BrandAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Screen Content States (Loading, Error, Empty, List)
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BrandAccent)
                    }
                }

                hasError -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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
                                    contentDescription = stringResource(R.string.websites_load_error),
                                    tint = Color.Red,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.websites_load_error),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.websites_check_connection),
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
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

                validWebsites.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(BrandPanel),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = stringResource(R.string.tab_websites),
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.websites_empty),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.websites_empty_hint),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                            if (onRetrySync != null) {
                                Button(
                                    onClick = onRetrySync,
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPanel),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.websites_refresh_list), modifier = Modifier.size(16.dp), tint = BrandAccent)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.websites_refresh_list), color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 14.dp, top = 8.dp, end = 14.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("websites_list")
                    ) {
                        items(validWebsites, key = { it.id }) { site ->
                            val domainName = extractDomainName(site.url)
                            val invalidUrlMessage = stringResource(R.string.websites_invalid_url)

                            WebsiteCardItem(
                                site = site,
                                domainName = domainName,
                                onOpen = {
                                    if (isValidWebsiteUrl(site.url)) {
                                        onSelectWebsite(site)
                                    } else {
                                        Toast.makeText(context, invalidUrlMessage, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WebsiteCardItem(
    site: WebsiteItem,
    domainName: String,
    onOpen: () -> Unit
) {
    val logoToLoad = remember(site.logoUrl, site.url, domainName) {
        deriveLogoUrl(site, domainName)
    }

    var isImageError by remember(site.id, logoToLoad) { mutableStateOf(false) }
    val openDescription = stringResource(R.string.websites_open, site.name)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandPanel)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clickable { onOpen() }
            .semantics { contentDescription = openDescription }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Official Website Logo with Loading & Clean Material Fallback
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandPrimary.copy(alpha = 0.2f))
                        .border(1.dp, BrandBorder, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (logoToLoad.isNotBlank() && !isImageError) {
                        SubcomposeAsyncImage(
                            model = logoToLoad,
                            contentDescription = site.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentScale = ContentScale.Fit,
                            loading = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = BrandAccent,
                                        strokeWidth = 2.dp
                                    )
                                }
                            },
                            onError = {
                                isImageError = true
                            }
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = site.name,
                            tint = BrandAccent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Name & Domain Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = site.name,
                        modifier = Modifier.fillMaxWidth(),
                        style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.ContentOrRtl),
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )

                    if (domainName.isNotBlank()) {
                        Surface(
                            color = BrandPrimary.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = domainName,
                                color = BrandAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Description text
            if (site.description.isNotBlank()) {
                Text(
                    text = site.description,
                    style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.ContentOrRtl),
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
            }

            // Open Website Button
            OutlinedButton(
                onClick = onOpen,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 34.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandAccent.copy(alpha = 0.65f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandAccent),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = stringResource(R.string.open_website_in_browser),
                    tint = BrandAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.open_website),
                    color = BrandAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
