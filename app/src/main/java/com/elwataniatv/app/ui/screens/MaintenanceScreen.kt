package com.elwataniatv.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elwataniatv.app.R
import com.elwataniatv.app.ui.theme.BrandAccent
import com.elwataniatv.app.util.safeHttpUri
import com.elwataniatv.app.ui.theme.BrandBg
import com.elwataniatv.app.ui.theme.BrandPanel
import com.elwataniatv.app.ui.theme.BrandPrimary

@Composable
fun MaintenanceScreen(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BrandBg, BrandPanel, BrandBg)
                )
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.maintenance_title),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = message.ifEmpty { stringResource(R.string.maintenance_default_message) },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.retry), color = Color.White, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        safeHttpUri("https://elwataniatv.dz")?.let { safeUri ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, safeUri))
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.visit_official_website), color = BrandAccent)
                }
            }
        }
    }
}
