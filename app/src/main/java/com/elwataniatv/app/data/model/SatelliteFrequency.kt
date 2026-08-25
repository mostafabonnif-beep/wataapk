package com.elwataniatv.app.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class SatelliteFrequency(
    val id: String,
    val satelliteName: String,
    val orbitalPosition: String = "",
    val frequencyMhz: Int = 0,
    val polarization: String = "",
    val symbolRate: Int = 0,
    val fec: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
    val order: Int = 0
) {
    val displayFrequency: String
        get() = if (frequencyMhz > 0) "$frequencyMhz MHz" else ""

    val displaySymbolRate: String
        get() = if (symbolRate > 0) symbolRate.toString() else ""

    val copyText: String
        get() = buildString {
            append(satelliteName)
            if (orbitalPosition.isNotBlank()) append(" ($orbitalPosition)")
            if (frequencyMhz > 0) append(" $frequencyMhz")
            if (polarization.isNotBlank()) append(" $polarization")
            if (symbolRate > 0) append(" $symbolRate")
            if (fec.isNotBlank()) append(" FEC $fec")
        }
}
