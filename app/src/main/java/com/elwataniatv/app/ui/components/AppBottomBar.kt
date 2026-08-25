package com.elwataniatv.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elwataniatv.app.ui.navigation.Screen
import com.elwataniatv.app.ui.theme.BrandAccent
import com.elwataniatv.app.ui.theme.BrandPanel

/**
 * Responsive bottom navigation for phones, tablets and Android TV.
 *
 * The component deliberately keeps navigation order in the logical Compose
 * layout direction. In Arabic/RTL the first item is placed on the right; in
 * English/LTR the same list starts on the left. Callers only provide [items]
 * in their desired product order.
 */
@Composable
fun AppBottomBar(
    items: List<Screen>,
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    BoxWithConstraints(
            modifier = modifier
            .fillMaxWidth()
            .testTag("app_bottom_bar")
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        val isExpanded = maxWidth >= 600.dp
        val barHeight = if (isExpanded) 72.dp else 62.dp
        val outerPadding = if (isExpanded) 12.dp else 5.dp
        val iconSize = if (isExpanded) 24.dp else 21.dp
        val labelSize = if (isExpanded) 11.sp else 10.sp
        val itemCorner = if (isExpanded) 15.dp else 12.dp

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(if (isExpanded) 20.dp else 16.dp))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(if (isExpanded) 20.dp else 16.dp)
                ),
            color = BrandPanel.copy(alpha = 0.97f),
            tonalElevation = if (isExpanded) 3.dp else 1.dp,
            shadowElevation = if (isExpanded) 6.dp else 2.dp,
            shape = RoundedCornerShape(if (isExpanded) 20.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = outerPadding, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 6.dp else 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { screen ->
                    ResponsiveBottomBarItem(
                        screen = screen,
                        isSelected = screen.route == currentRoute,
                        isExpanded = isExpanded,
                        iconSize = iconSize,
                        labelSize = labelSize,
                        cornerRadius = itemCorner,
                        onClick = { onNavigate(screen) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResponsiveBottomBarItem(
    screen: Screen,
    isSelected: Boolean,
    isExpanded: Boolean,
    iconSize: androidx.compose.ui.unit.Dp,
    labelSize: androidx.compose.ui.unit.TextUnit,
    cornerRadius: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember(screen.route) { mutableStateOf(false) }
    val itemLabel = stringResource(screen.titleRes)
    val description = itemLabel
    val targetScale = if (focused) 1.04f else 1f
    val scale by animateFloatAsState(targetValue = targetScale, label = "bottom_item_scale")
    val backgroundColor by animateColorAsState(
        targetValue = when {
            focused -> BrandAccent.copy(alpha = 0.22f)
            isSelected -> BrandAccent.copy(alpha = 0.14f)
            else -> Color.Transparent
        },
        label = "bottom_item_background"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected || focused) BrandAccent else Color.White.copy(alpha = 0.62f),
        label = "bottom_item_icon_tint"
    )
    val labelTint by animateColorAsState(
        targetValue = if (isSelected || focused) BrandAccent else Color.White.copy(alpha = 0.62f),
        label = "bottom_item_label_tint"
    )
    val focusBorderWidth by animateDpAsState(
        targetValue = if (focused) 2.dp else 0.dp,
        label = "bottom_item_focus_border"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .testTag("bottom_bar_${screen.route}")
            .scale(scale)
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .border(
                width = focusBorderWidth,
                color = if (focused) BrandAccent else Color.Transparent,
                shape = RoundedCornerShape(cornerRadius)
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .focusable()
            .semantics {
                contentDescription = description
                role = Role.Tab
                selected = isSelected
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = if (isExpanded) 6.dp else 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (isExpanded) 3.dp else 2.dp)
        ) {
            Icon(
                imageVector = screen.icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
            Text(
                text = itemLabel,
                color = labelTint,
                fontSize = labelSize,
                fontWeight = if (isSelected || focused) FontWeight.Bold else FontWeight.Normal,
                maxLines = 2,
                softWrap = true,
                lineHeight = if (isExpanded) 13.sp else 11.sp,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
