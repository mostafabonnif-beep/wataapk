package com.elwataniatv.app.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elwataniatv.app.R
import com.elwataniatv.app.data.model.SatelliteFrequency
import com.elwataniatv.app.ui.theme.BrandAccent
import com.elwataniatv.app.ui.theme.BrandPanel

@Composable
fun SatelliteFrequenciesCard(
    frequencies: List<SatelliteFrequency>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = BrandPanel),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.satellite_frequencies),
                color = BrandAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            if (frequencies.isEmpty()) {
                Text(
                    text = stringResource(R.string.satellite_frequencies_empty),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            } else {
                frequencies.forEach { frequency ->
                    val copiedMessage = stringResource(R.string.satellite_frequency_copied, frequency.satelliteName)
                    SatelliteFrequencyRow(
                        frequency = frequency,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(
                                ClipData.newPlainText(frequency.satelliteName, frequency.copyText)
                            )
                            Toast.makeText(
                                context,
                                copiedMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SatelliteFrequencyRow(
    frequency: SatelliteFrequency,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = buildString {
                    append(frequency.satelliteName)
                    if (frequency.orbitalPosition.isNotBlank()) append(" (${frequency.orbitalPosition})")
                },
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(
                    R.string.satellite_frequency_details,
                    frequency.displayFrequency,
                    frequency.polarization
                ),
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 11.sp
            )
            Text(
                text = stringResource(
                    R.string.satellite_symbol_rate_details,
                    frequency.displaySymbolRate,
                    frequency.fec.ifBlank { "—" }
                ),
                color = Color.White.copy(alpha = 0.52f),
                fontSize = 10.sp
            )
            if (frequency.notes.isNotBlank()) {
                Text(
                    text = frequency.notes,
                    color = Color.White.copy(alpha = 0.48f),
                    fontSize = 10.sp,
                    maxLines = 2
                )
            }
        }

        IconButton(onClick = onCopy) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = stringResource(R.string.copy),
                tint = BrandAccent
            )
        }
    }
}
