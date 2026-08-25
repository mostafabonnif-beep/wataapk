package com.elwataniatv.app.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.elwataniatv.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elwataniatv.app.ui.theme.BrandAccent
import com.elwataniatv.app.ui.theme.BrandBg
import com.elwataniatv.app.ui.theme.BrandPanel
import com.elwataniatv.app.ui.theme.BrandPrimary
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Utility class to observe network availability in real time.
 */
class NetworkMonitor(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Send initial state
        trySend(checkCurrentConnection())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    fun checkCurrentConnection(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

/**
 * Network availability shell with an offline-mode screen.
 *
 * This composable does not intercept uncaught exceptions; application-level
 * crash handling remains owned by the platform process handler.
 */
@Composable
fun GlobalErrorBoundary(
    modifier: Modifier = Modifier,
    onRetrySync: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val networkMonitor = remember(context) { NetworkMonitor(context) }
    val isOnline by networkMonitor.isOnline.collectAsState(initial = networkMonitor.checkCurrentConnection())

    var allowOfflineMode by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            // Offline / No Internet Connection Screen
            !isOnline && !allowOfflineMode -> {
                NoInternetScreen(
                    onRetry = {
                        val reconnected = networkMonitor.checkCurrentConnection()
                        if (reconnected) {
                            onRetrySync()
                        }
                    },
                    onContinueOffline = {
                        allowOfflineMode = true
                    }
                )
            }

            // Normal Application Flow
            else -> {
                content()

                // Persistent Offline Mode Banner (when operating offline in fallback mode)
                AnimatedVisibility(
                    visible = !isOnline && allowOfflineMode,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 70.dp, start = 16.dp, end = 16.dp)
                ) {
                    Surface(
                        color = Color(0xFFD97706),
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.offline_banner),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modern M3 Custom "No Internet" State Screen
 */
@Composable
fun NoInternetScreen(
    onRetry: () -> Unit,
    onContinueOffline: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BrandBg,
                        BrandPanel
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Decorative Glowing Disconnected Header Icon
            Surface(
                shape = CircleShape,
                color = BrandPrimary.copy(alpha = 0.2f),
                border = BorderStroke(2.dp, BrandAccent.copy(alpha = 0.5f)),
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = stringResource(R.string.no_connection),
                        tint = BrandAccent,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.no_internet_title),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.no_internet_message),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Action: Retry Connection
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.retry_connection),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Secondary Action: Continue with Offline Database Cache
            OutlinedButton(
                onClick = onContinueOffline,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BrandAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cached,
                    contentDescription = null,
                    tint = BrandAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.continue_offline),
                    color = BrandAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
