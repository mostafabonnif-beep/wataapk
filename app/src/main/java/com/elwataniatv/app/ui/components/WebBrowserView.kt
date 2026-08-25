package com.elwataniatv.app.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.elwataniatv.app.R
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.elwataniatv.app.data.model.WebsiteItem
import com.elwataniatv.app.ui.theme.BrandAccent
import com.elwataniatv.app.ui.theme.BrandBorder
import com.elwataniatv.app.ui.theme.BrandPanel
import com.elwataniatv.app.ui.theme.BrandPrimary
import com.elwataniatv.app.util.safeHttpUri

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun WebBrowserView(
    website: WebsiteItem,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var hasError by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf(website.url) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Intercept back gesture if WebView has web history
    BackHandler(enabled = canGoBack) {
        webViewRef?.let {
            if (it.canGoBack()) {
                it.goBack()
            }
        }
    }

    val displayHost = remember(currentUrl) {
        try {
            java.net.URI(currentUrl).host?.replace("www.", "") ?: currentUrl
        } catch (e: Exception) {
            currentUrl
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Browser Header Toolbar
        Surface(
            color = BrandPanel,
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Navigation Actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = Color.White
                            )
                        }

                        // Back Navigation
                        IconButton(
                            onClick = { webViewRef?.goBack() },
                            enabled = canGoBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.browser_back),
                                tint = if (canGoBack) Color.White else Color.White.copy(alpha = 0.3f)
                            )
                        }

                        // Forward Navigation
                        IconButton(
                            onClick = { webViewRef?.goForward() },
                            enabled = canGoForward,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = stringResource(R.string.browser_forward),
                                tint = if (canGoForward) Color.White else Color.White.copy(alpha = 0.3f)
                            )
                        }

                        // Site Title & Address
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp)
                        ) {
                            Text(
                                text = website.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = displayHost,
                                color = BrandAccent,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Refresh & External Open Button
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = {
                                hasError = false
                                webViewRef?.reload()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.browser_reload),
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                try {
                                    val targetUrl = if (currentUrl.isNotBlank()) currentUrl else website.url
                                    val safeUri = safeHttpUri(targetUrl) ?: throw SecurityException("Unsafe URL")
                                    val intent = Intent(Intent.ACTION_VIEW, safeUri)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, context.getString(R.string.browser_external_error), Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = stringResource(R.string.open_external_browser),
                                tint = BrandAccent
                            )
                        }
                    }
                }

                // Loading Indicator Progress Bar
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { loadProgress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = BrandAccent,
                        trackColor = Color.Transparent
                    )
                }
            }
        }

        // WebView Content Area
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("embedded_web_view"),
                factory = { ctx ->
                    try {
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = false
                                allowContentAccess = false
                                javaScriptCanOpenWindowsAutomatically = false
                                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    safeBrowsingEnabled = true
                                }
                                setSupportMultipleWindows(false)
                                cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                builtInZoomControls = true
                                displayZoomControls = false
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    loadProgress = newProgress
                                    isLoading = newProgress < 100
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                    hasError = false
                                    url?.let { currentUrl = it }
                                    canGoBack = view?.canGoBack() == true
                                    canGoForward = view?.canGoForward() == true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    canGoBack = view?.canGoBack() == true
                                    canGoForward = view?.canGoForward() == true
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        isLoading = false
                                        hasError = true
                                    }
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val uri = request?.url ?: return false
                                    if (uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true)) {
                                        return false
                                    }
                                    // Never hand custom schemes to an external intent.
                                    return true
                                }
                            }
                            loadUrl(website.url)
                            webViewRef = this
                        }
                    } catch (e: Exception) {
                        hasError = true
                        View(ctx)
                    }
                },
                update = { view ->
                    webViewRef = view as? WebView
                }
            )

            DisposableEffect(Unit) {
                onDispose {
                    webViewRef?.apply {
                        stopLoading()
                        webChromeClient = null
                        destroy()
                    }
                    webViewRef = null
                }
            }

            // Web Error Overlay
            if (hasError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.Red.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.browser_load_error_title),
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.browser_load_error_message),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                        Button(
                            onClick = {
                                hasError = false
                                isLoading = true
                                if (webViewRef != null) {
                                    webViewRef?.reload()
                                } else {
                                    webViewRef?.loadUrl(website.url)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.retry), color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
