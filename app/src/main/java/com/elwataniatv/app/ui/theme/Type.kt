package com.elwataniatv.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.elwataniatv.app.R

/**
 * Professional channel typography.
 *
 * Cairo (Google Fonts, OFL license) is a modern geometric typeface with
 * excellent Arabic and Latin coverage — it gives the app a distinct
 * broadcast identity instead of the stock system font, while keeping
 * RTL shaping correct on every supported API level.
 */
val ChannelFontFamily = FontFamily(
    androidx.compose.ui.text.font.Font(R.font.cairo_regular, FontWeight.Normal),
    androidx.compose.ui.text.font.Font(R.font.cairo_medium, FontWeight.Medium),
    androidx.compose.ui.text.font.Font(R.font.cairo_semibold, FontWeight.SemiBold),
    androidx.compose.ui.text.font.Font(R.font.cairo_bold, FontWeight.Bold),
    androidx.compose.ui.text.font.Font(R.font.cairo_black, FontWeight.Black)
)

val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = ChannelFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = ChannelFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = ChannelFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = ChannelFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = ChannelFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = ChannelFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ChannelFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = ChannelFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelMedium = TextStyle(
        fontFamily = ChannelFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = ChannelFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.sp
    )
)
