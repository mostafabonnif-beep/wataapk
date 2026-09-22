package com.elwataniatv.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Feedback
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
import com.elwataniatv.app.data.model.RemoteAppConfig
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
    appConfig: RemoteAppConfig? = null,
    onSubmitFeedback: ((type: String, message: String, email: String, onResult: (Boolean, String?) -> Unit) -> Unit)? = null,
    isLoading: Boolean = false,
    hasError: Boolean = false,
    onRetrySync: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val socialOpenFailedMessage = stringResource(R.string.social_open_failed)

    // Safely filter social pages with valid URLs
    val validSocialPages = remember(socialPages) {
        socialPages
            .filter { it.isActive && isValidSocialUrl(it.url) }
            .sortedWith(compareBy<SocialPage> { it.order }.thenBy { it.name.lowercase() })
    }

    val hasDirectChannels = remember(appConfig) {
        appConfig != null && (
            appConfig.contactEmail.isNotBlank() ||
            appConfig.whatsappUrl.isNotBlank() ||
            appConfig.officialWebsite.isNotBlank()
        )
    }

    val isCompletelyEmpty = validSocialPages.isEmpty() && !hasDirectChannels && onSubmitFeedback == null

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
                            text = stringResource(R.string.tab_social),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                        )
                        Text(
                            text = stringResource(R.string.contact_form_subtitle),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
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

        // Screen States: Loading, Error, Empty, Content
        when {
            isLoading && isCompletelyEmpty -> {
                LazyColumn(
                    contentPadding = PaddingValues(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("social_loading_state")
                ) {
                    item { ContactSkeletonCard() }
                    items(4) { SocialSkeletonCard() }
                }
            }

            hasError && isCompletelyEmpty -> {
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

            isCompletelyEmpty -> {
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
                                imageVector = Icons.Default.Feedback,
                                contentDescription = stringResource(R.string.contact_form_not_configured),
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.contact_form_not_configured),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.contact_form_not_configured_hint),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                    contentPadding = PaddingValues(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.testTag("social_list")
                ) {
                    // Direct Contact Channels (Email, WhatsApp, Website)
                    if (hasDirectChannels && appConfig != null) {
                        item(key = "direct_contact_card") {
                            DirectContactCard(
                                appConfig = appConfig,
                                onOpenUrl = { url ->
                                    val safeUri = safeHttpUri(url)
                                    if (safeUri != null) {
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, safeUri))
                                        }.onFailure {
                                            Toast.makeText(context, socialOpenFailedMessage, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onSendEmail = { email ->
                                    runCatching {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:$email")
                                            putExtra(Intent.EXTRA_SUBJECT, "تواصل مع الوطنية TV")
                                        }
                                        context.startActivity(intent)
                                    }.onFailure {
                                        Toast.makeText(context, email, Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                        }
                    }

                    // In-App Contact & Feedback Form
                    if (onSubmitFeedback != null) {
                        item(key = "in_app_contact_form") {
                            ContactFormCard(onSubmit = onSubmitFeedback)
                        }
                    }

                    // Section Title for Social Channels
                    if (validSocialPages.isNotEmpty()) {
                        item(key = "social_section_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.social_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = stringResource(R.string.social_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }

                        items(validSocialPages, key = { it.id }) { page ->
                            val platformColor = parsePlatformColor(page.platform, page.color)
                            val visualPlatformColor = if (normalizePlatformKey(page.platform) == "x") Color.White else platformColor
                            val logoUrlToLoad = remember(page.logoUrl, page.platform, page.url) { deriveSocialLogoUrl(page) }
                            val contentDesc = stringResource(R.string.social_visit_page) + ": " + page.platform
                            val openFailedMessage = stringResource(R.string.social_open_failed)
                            val invalidUrlMessage = stringResource(R.string.social_invalid_url)
                            val openPage: () -> Unit = {
                                val safeUri = safeHttpUri(page.url)
                                if (safeUri == null) {
                                    Toast.makeText(context, invalidUrlMessage, Toast.LENGTH_SHORT).show()
                                } else {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, safeUri))
                                    }.onFailure {
                                        Toast.makeText(context, socialOpenFailedMessage, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(BrandPanel)
                                    .border(1.dp, visualPlatformColor.copy(alpha = 0.24f), RoundedCornerShape(18.dp))
                                    .semantics { contentDescription = contentDesc }
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(visualPlatformColor.copy(alpha = 0.16f))
                                            .border(1.dp, visualPlatformColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        PlatformIcon(
                                            platform = page.platform,
                                            logoUrl = logoUrlToLoad,
                                            contentDescription = contentDesc,
                                            tintColor = visualPlatformColor,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                    Surface(
                                        color = visualPlatformColor.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(Icons.Default.Verified, contentDescription = stringResource(R.string.social_official_page), tint = visualPlatformColor, modifier = Modifier.size(15.dp))
                                            Text(
                                                text = page.platform.ifBlank { stringResource(R.string.tab_social) },
                                                style = MaterialTheme.typography.labelSmall.copy(textDirection = TextDirection.ContentOrRtl),
                                                color = visualPlatformColor,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = page.name.ifBlank { page.platform.ifBlank { stringResource(R.string.tab_social) } },
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                                )
                                Text(
                                    text = page.description.ifBlank { stringResource(R.string.social_visit_page) },
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Content),
                                    color = Color.White.copy(alpha = 0.64f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                                )
                                Button(
                                    onClick = openPage,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = visualPlatformColor),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 9.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = stringResource(R.string.social_visit_page), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.social_visit_page), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DirectContactCard(
    appConfig: RemoteAppConfig,
    onOpenUrl: (String) -> Unit,
    onSendEmail: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BrandPanel)
            .border(1.dp, BrandBorder, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Email, contentDescription = stringResource(R.string.contact_direct_channels), tint = BrandAccent, modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(R.string.contact_direct_channels),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        if (appConfig.contactEmail.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable { onSendEmail(appConfig.contactEmail) }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "البريد الإلكتروني الرسمي", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                    Text(text = appConfig.contactEmail, style = MaterialTheme.typography.bodyMedium, color = BrandAccent, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = appConfig.contactEmail, tint = BrandAccent, modifier = Modifier.size(18.dp))
            }
        }

        if (appConfig.whatsappUrl.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF25D366).copy(alpha = 0.12f))
                    .clickable { onOpenUrl(appConfig.whatsappUrl) }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "خدمة المشاهدين عبر واتساب", style = MaterialTheme.typography.bodySmall, color = Color(0xFF25D366))
                    Text(text = "مراسلة مباشرة مع فريق القناة", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "WhatsApp", tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
            }
        }

        if (appConfig.officialWebsite.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable { onOpenUrl(appConfig.officialWebsite) }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "الموقع الرسمي للقناة", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                    Text(text = appConfig.officialWebsite, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Icon(Icons.Default.Language, contentDescription = "Website", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun ContactFormCard(
    onSubmit: (type: String, message: String, email: String, onResult: (Boolean, String?) -> Unit) -> Unit
) {
    var selectedType by remember { mutableStateOf("general") }
    var emailText by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submittedSuccess by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val contactMessageHint = stringResource(R.string.contact_form_message_hint)
    val contactSentSuccess = stringResource(R.string.contact_form_sent_success)
    val socialLoadError = stringResource(R.string.social_load_error)

    val types = listOf(
        "general" to stringResource(R.string.contact_form_type_general),
        "suggestion" to stringResource(R.string.contact_form_type_suggestion),
        "bug" to stringResource(R.string.contact_form_type_bug),
        "content" to stringResource(R.string.contact_form_type_content)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BrandPanel)
            .border(1.dp, BrandBorder, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Feedback, contentDescription = stringResource(R.string.contact_form_title), tint = BrandAccent, modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(R.string.contact_form_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Text(
            text = stringResource(R.string.contact_form_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )

        AnimatedVisibility(visible = submittedSuccess) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2E7D32).copy(alpha = 0.2f))
                    .border(1.dp, Color(0xFF4CAF50), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                Text(
                    text = stringResource(R.string.contact_form_sent_success),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Type selection chips
        Text(text = stringResource(R.string.contact_form_type_label), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            types.forEach { (typeKey, typeLabel) ->
                val isSelected = selectedType == typeKey
                Surface(
                    color = if (isSelected) BrandPrimary else Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedType = typeKey }
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        OutlinedTextField(
            value = emailText,
            onValueChange = { emailText = it },
            label = { Text(stringResource(R.string.contact_form_email_label), fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandAccent,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        OutlinedTextField(
            value = messageText,
            onValueChange = { messageText = it },
            label = { Text(stringResource(R.string.contact_form_message_label), fontSize = 12.sp) },
            placeholder = { Text(stringResource(R.string.contact_form_message_hint), fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandAccent,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Button(
            onClick = {
                val msg = messageText.trim()
                if (msg.isBlank()) {
                    Toast.makeText(context, contactMessageHint, Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isSubmitting = true
                onSubmit(selectedType, msg, emailText.trim()) { success, err ->
                    isSubmitting = false
                    if (success) {
                        submittedSuccess = true
                        messageText = ""
                        Toast.makeText(context, contactSentSuccess, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, err ?: socialLoadError, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = !isSubmitting && messageText.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.contact_form_sending), fontSize = 13.sp)
            } else {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.contact_form_send_btn), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ContactSkeletonCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "contact_shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandPanel)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = alpha))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = alpha))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = alpha))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = alpha))
            )
        }
    }
}

@Composable
fun SocialSkeletonCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "social_shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandPanel)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = alpha))
                )
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = alpha))
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = alpha))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = alpha))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = alpha))
            )
        }
    }
}
