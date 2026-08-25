package com.elwataniatv.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.elwataniatv.app.R
import com.elwataniatv.app.data.model.SocialPage
import com.elwataniatv.app.ui.theme.BrandAccent
import com.elwataniatv.app.util.safeHttpUri
import com.elwataniatv.app.ui.theme.BrandBorder
import com.elwataniatv.app.ui.theme.BrandPanel
import com.elwataniatv.app.ui.theme.BrandPrimary
import com.elwataniatv.app.ui.theme.BrandPillBg

private fun normalizePlatformKey(platform: String): String {
    val lower = platform.trim().lowercase()
    return when {
        lower.contains("facebook") || lower.contains("فيسبوك") -> "facebook"
        lower.contains("youtube") || lower.contains("يوتيوب") -> "youtube"
        lower.contains("telegram") || lower.contains("تيليجرام") || lower.contains("تليجرام") -> "telegram"
        lower.contains("tiktok") || lower.contains("تيك توك") || lower.contains("تيكتوك") -> "tiktok"
        lower.contains("instagram") || lower.contains("إنستغرام") || lower.contains("انستغرام") -> "instagram"
        lower.contains("twitter") || lower.contains("تويتر") || lower == "x" || lower.contains("إكس") || lower.contains("اكس") -> "x"
        lower.contains("whatsapp") || lower.contains("واتساب") || lower.contains("واتس") -> "whatsapp"
        else -> "other"
    }
}

private fun parsePlatformColor(platform: String, hexColor: String): Color {
    if (hexColor.isNotBlank() && hexColor.startsWith("#")) {
        try {
            return Color(android.graphics.Color.parseColor(hexColor))
        } catch (e: Exception) {
            // fallback
        }
    }
    return when (normalizePlatformKey(platform)) {
        "facebook" -> Color(0xFF1877F2)
        "youtube" -> Color(0xFFFF0000)
        "telegram" -> Color(0xFF229ED9)
        "tiktok" -> Color(0xFF00F2FE)
        "instagram" -> Color(0xFFE4405F)
        "x" -> Color(0xFF1DA1F2)
        "whatsapp" -> Color(0xFF25D366)
        else -> BrandAccent
    }
}

private fun isValidSocialUrl(url: String): Boolean = safeHttpUri(url) != null

private fun deriveSocialLogoUrl(page: SocialPage): String {
    if (safeHttpUri(page.logoUrl) != null) {
        return page.logoUrl
    }
    val domain = when (normalizePlatformKey(page.platform)) {
        "facebook" -> "facebook.com"
        "youtube" -> "youtube.com"
        "telegram" -> "telegram.org"
        "tiktok" -> "tiktok.com"
        "instagram" -> "instagram.com"
        "x" -> "x.com"
        "whatsapp" -> "whatsapp.com"
        else -> if (page.url.contains(".")) {
            try {
                java.net.URI(page.url).host?.removePrefix("www.") ?: ""
            } catch (e: Exception) { "" }
        } else ""
    }
    return if (domain.isNotBlank()) "https://www.google.com/s2/favicons?domain=$domain&sz=128" else ""
}

@Composable
fun NativePlatformVectorIcon(
    platformKey: String,
    tint: Color,
    modifier: Modifier = Modifier.size(28.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (platformKey) {
            "facebook" -> {
                val path = Path().apply {
                    moveTo(w * 0.68f, h * 0.95f)
                    lineTo(w * 0.68f, h * 0.55f)
                    lineTo(w * 0.82f, h * 0.55f)
                    lineTo(w * 0.85f, h * 0.38f)
                    lineTo(w * 0.68f, h * 0.38f)
                    lineTo(w * 0.68f, h * 0.27f)
                    cubicTo(w * 0.68f, h * 0.22f, w * 0.70f, h * 0.18f, w * 0.78f, h * 0.18f)
                    lineTo(w * 0.86f, h * 0.18f)
                    lineTo(w * 0.86f, h * 0.03f)
                    cubicTo(w * 0.82f, h * 0.02f, w * 0.75f, h * 0.01f, w * 0.67f, h * 0.01f)
                    cubicTo(w * 0.48f, h * 0.01f, w * 0.35f, h * 0.12f, w * 0.35f, h * 0.34f)
                    lineTo(w * 0.35f, h * 0.38f)
                    lineTo(w * 0.18f, h * 0.38f)
                    lineTo(w * 0.18f, h * 0.55f)
                    lineTo(w * 0.35f, h * 0.55f)
                    lineTo(w * 0.35f, h * 0.95f)
                    close()
                }
                drawPath(path = path, color = tint)
            }
            "youtube" -> {
                val rectPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(0f, h * 0.20f, w, h * 0.80f),
                            cornerRadius = CornerRadius(w * 0.22f, h * 0.22f)
                        )
                    )
                }
                drawPath(path = rectPath, color = tint)
                val playTriangle = Path().apply {
                    moveTo(w * 0.40f, h * 0.36f)
                    lineTo(w * 0.68f, h * 0.50f)
                    lineTo(w * 0.40f, h * 0.64f)
                    close()
                }
                drawPath(path = playTriangle, color = BrandPanel)
            }
            "telegram" -> {
                val planePath = Path().apply {
                    moveTo(w * 0.15f, h * 0.48f)
                    lineTo(w * 0.85f, h * 0.18f)
                    lineTo(w * 0.72f, h * 0.82f)
                    lineTo(w * 0.52f, h * 0.62f)
                    lineTo(w * 0.42f, h * 0.72f)
                    lineTo(w * 0.40f, h * 0.58f)
                    lineTo(w * 0.70f, h * 0.32f)
                    lineTo(w * 0.32f, h * 0.54f)
                    close()
                }
                drawPath(path = planePath, color = tint)
            }
            "tiktok" -> {
                val notePath = Path().apply {
                    moveTo(w * 0.55f, h * 0.15f)
                    cubicTo(w * 0.58f, h * 0.28f, w * 0.68f, h * 0.38f, w * 0.82f, h * 0.40f)
                    lineTo(w * 0.82f, h * 0.55f)
                    cubicTo(w * 0.72f, h * 0.54f, w * 0.62f, h * 0.48f, w * 0.55f, h * 0.42f)
                    lineTo(w * 0.55f, h * 0.70f)
                    cubicTo(w * 0.55f, h * 0.83f, w * 0.43f, h * 0.92f, w * 0.30f, h * 0.90f)
                    cubicTo(w * 0.18f, h * 0.88f, w * 0.10f, h * 0.76f, w * 0.12f, h * 0.63f)
                    cubicTo(w * 0.15f, h * 0.52f, w * 0.28f, h * 0.45f, w * 0.40f, h * 0.48f)
                    lineTo(w * 0.40f, h * 0.62f)
                    cubicTo(w * 0.34f, h * 0.60f, w * 0.26f, h * 0.65f, w * 0.25f, h * 0.71f)
                    cubicTo(w * 0.24f, h * 0.77f, w * 0.30f, h * 0.82f, w * 0.36f, h * 0.80f)
                    cubicTo(w * 0.42f, h * 0.78f, w * 0.45f, h * 0.72f, w * 0.45f, h * 0.65f)
                    lineTo(w * 0.45f, h * 0.15f)
                    close()
                }
                drawPath(path = notePath, color = tint)
            }
            "instagram" -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.12f, h * 0.12f),
                    size = Size(w * 0.76f, h * 0.76f),
                    cornerRadius = CornerRadius(w * 0.22f, h * 0.22f),
                    style = Stroke(width = w * 0.08f)
                )
                drawCircle(
                    color = tint,
                    radius = w * 0.20f,
                    center = Offset(w * 0.50f, h * 0.50f),
                    style = Stroke(width = w * 0.08f)
                )
                drawCircle(
                    color = tint,
                    radius = w * 0.05f,
                    center = Offset(w * 0.72f, h * 0.28f)
                )
            }
            "x" -> {
                val xPath = Path().apply {
                    moveTo(w * 0.15f, h * 0.15f)
                    lineTo(w * 0.45f, h * 0.52f)
                    lineTo(w * 0.15f, h * 0.85f)
                    lineTo(w * 0.28f, h * 0.85f)
                    lineTo(w * 0.51f, h * 0.59f)
                    lineTo(w * 0.72f, h * 0.85f)
                    lineTo(w * 0.88f, h * 0.85f)
                    lineTo(w * 0.56f, h * 0.46f)
                    lineTo(w * 0.85f, h * 0.15f)
                    lineTo(w * 0.72f, h * 0.15f)
                    lineTo(w * 0.50f, h * 0.39f)
                    lineTo(w * 0.31f, h * 0.15f)
                    close()
                }
                drawPath(path = xPath, color = tint)
            }
            "whatsapp" -> {
                val bubble = Path().apply {
                    addOval(Rect(w * 0.10f, h * 0.10f, w * 0.90f, h * 0.82f))
                    moveTo(w * 0.22f, h * 0.75f)
                    lineTo(w * 0.12f, h * 0.90f)
                    lineTo(w * 0.32f, h * 0.82f)
                }
                drawPath(path = bubble, color = tint, style = Stroke(width = w * 0.08f))
                val phone = Path().apply {
                    moveTo(w * 0.35f, h * 0.35f)
                    cubicTo(w * 0.38f, h * 0.35f, w * 0.42f, h * 0.40f, w * 0.45f, h * 0.45f)
                    lineTo(w * 0.42f, h * 0.50f)
                    cubicTo(w * 0.48f, h * 0.58f, w * 0.54f, h * 0.62f, w * 0.60f, h * 0.62f)
                    lineTo(w * 0.65f, h * 0.58f)
                    cubicTo(w * 0.70f, h * 0.62f, w * 0.72f, h * 0.68f, w * 0.70f, h * 0.72f)
                    cubicTo(w * 0.60f, h * 0.80f, w * 0.40f, h * 0.70f, w * 0.30f, h * 0.50f)
                    cubicTo(w * 0.25f, h * 0.42f, w * 0.30f, h * 0.36f, w * 0.35f, h * 0.35f)
                    close()
                }
                drawPath(path = phone, color = tint)
            }
        }
    }
}

@Composable
fun PlatformIcon(
    platform: String,
    logoUrl: String,
    contentDescription: String,
    tintColor: Color,
    modifier: Modifier = Modifier.size(28.dp)
) {
    val platformKey = remember(platform) { normalizePlatformKey(platform) }
    var isImageError by remember(platformKey, logoUrl) { mutableStateOf(false) }

    if (logoUrl.isNotBlank() && !isImageError) {
        SubcomposeAsyncImage(
            model = logoUrl,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit,
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = tintColor,
                        strokeWidth = 2.dp
                    )
                }
            },
            onError = {
                isImageError = true
            }
        )
    } else if (platformKey != "other") {
        NativePlatformVectorIcon(
            platformKey = platformKey,
            tint = tintColor,
            modifier = modifier
        )
    } else {
        Icon(
            imageVector = Icons.Default.Public,
            contentDescription = contentDescription,
            tint = tintColor,
            modifier = modifier
        )
    }
}

@Composable
fun SocialScreen(
    socialPages: List<SocialPage>,
    isLoading: Boolean = false,
    hasError: Boolean = false,
    onRetrySync: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Safely filter social pages with valid URLs
    val validSocialPages = remember(socialPages) {
        socialPages
            .filter { it.isActive && isValidSocialUrl(it.url) }
            .sortedWith(compareBy<SocialPage> { it.order }.thenBy { it.name.lowercase() })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Hero Header
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
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.social_title),
                            tint = BrandAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = stringResource(R.string.social_title),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                        Text(
                            text = stringResource(R.string.social_subtitle),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                }

                if (validSocialPages.isNotEmpty()) {
                    Surface(
                        color = BrandAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandAccent.copy(alpha = 0.3f)),
                        modifier = Modifier.widthIn(min = 86.dp)
                    ) {
                        Text(
                            text = pluralStringResource(R.plurals.social_count, validSocialPages.size, validSocialPages.size),
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

        // Screen States: Loading, Error, Empty, List
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
                                contentDescription = stringResource(R.string.social_load_error),
                                tint = Color.Red,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.social_load_error),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.social_check_connection),
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

            validSocialPages.isEmpty() -> {
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
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.tab_social),
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.social_empty),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.social_empty_hint),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                        if (onRetrySync != null) {
                            Button(
                                onClick = onRetrySync,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPanel),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.social_refresh_list), modifier = Modifier.size(16.dp), tint = BrandAccent)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.social_refresh_list), color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(start = 14.dp, top = 8.dp, end = 14.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("social_list")
                ) {
                    items(validSocialPages, key = { it.id }) { page ->
                        val platformColor = parsePlatformColor(page.platform, page.color)
                        val visualPlatformColor = if (normalizePlatformKey(page.platform) == "x") Color.White else platformColor
                        val logoUrlToLoad = remember(page.logoUrl, page.platform, page.url) {
                            deriveSocialLogoUrl(page)
                        }
                        val contentDesc = stringResource(R.string.social_visit_page) + ": " + page.platform
                        val openFailedMessage = stringResource(R.string.social_open_failed)
                        val invalidUrlMessage = stringResource(R.string.social_invalid_url)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(BrandPanel)
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                .clickable {
                                    if (isValidSocialUrl(page.url)) {
                                        try {
                                            val safeUri = safeHttpUri(page.url)
                                            if (safeUri == null) throw IllegalArgumentException("unsafe url")
                                            val intent = Intent(Intent.ACTION_VIEW, safeUri)
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, openFailedMessage, Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, invalidUrlMessage, Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .semantics { contentDescription = contentDesc }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Platform Official Logo
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(visualPlatformColor.copy(alpha = 0.18f))
                                        .border(1.dp, visualPlatformColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    PlatformIcon(
                                        platform = page.platform,
                                        logoUrl = logoUrlToLoad,
                                        contentDescription = contentDesc,
                                        tintColor = visualPlatformColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                // Platform Details
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = visualPlatformColor.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = page.platform.ifBlank { stringResource(R.string.tab_social) },
                                                style = MaterialTheme.typography.labelSmall.copy(textDirection = TextDirection.ContentOrRtl),
                                                color = visualPlatformColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = stringResource(R.string.social_official_page),
                                            tint = visualPlatformColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Text(
                                        text = page.name.ifBlank { page.platform.ifBlank { stringResource(R.string.tab_social) } },
                                        modifier = Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.titleSmall.copy(textDirection = TextDirection.ContentOrRtl),
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                                    )

                                    if (page.description.isNotBlank()) {
                                        Text(
                                            text = page.description,
                                            modifier = Modifier.fillMaxWidth(),
                                            style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.ContentOrRtl),
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 11.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                                        )
                                    }
                                }

                                // Direct Link Action Button
                                Surface(
                                    color = visualPlatformColor.copy(alpha = 0.15f),
                                    shape = CircleShape,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, visualPlatformColor.copy(alpha = 0.4f))
                                ) {
                                    Box(
                                        modifier = Modifier.padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = stringResource(R.string.social_visit_page),
                                            tint = visualPlatformColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
