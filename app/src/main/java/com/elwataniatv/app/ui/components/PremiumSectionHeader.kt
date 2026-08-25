package com.elwataniatv.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elwataniatv.app.ui.theme.BrandAccent

@Composable
fun PremiumSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 24.dp),
            style = androidx.compose.ui.text.TextStyle(
                textDirection = TextDirection.ContentOrRtl,
                lineHeight = 24.sp
            ),
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        subtitle?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 18.dp),
                style = androidx.compose.ui.text.TextStyle(
                    textDirection = TextDirection.ContentOrRtl,
                    lineHeight = 18.sp
                ),
                color = BrandAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
