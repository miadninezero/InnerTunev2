package com.zionhuang.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val badgeCount: Int = 0
)

/**
 * Glassmorphic bottom navigation with smooth transitions.
 * Provides primary navigation with minimal screen real estate impact.
 */
@Composable
fun GlassmorphicBottomNav(
    items: List<BottomNavItem> = defaultBottomNavItems(),
    selectedIndex: Int = 0,
    onItemSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    GlassmorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        cornerRadius = 24,
        enableBlur = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                BottomNavItemComponent(
                    item = item,
                    isSelected = index == selectedIndex,
                    onClick = { onItemSelected(index) }
                )
            }
        }
    }
}

@Composable
private fun BottomNavItemComponent(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconColor = animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        animationSpec = tween(300),
        label = "nav_icon_color"
    )

    val textAlpha = animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.onSurface
        else
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        animationSpec = tween(300),
        label = "nav_text_alpha"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                modifier = Modifier.size(24.dp),
                tint = iconColor.value
            )

            // Badge for item count (e.g., unread notifications)
            if (item.badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF5252)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.badgeCount.toString(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        if (isSelected) {
            Text(
                text = item.label,
                fontSize = 10.sp,
                fontWeight = FontWeight.W600,
                color = textAlpha.value,
                maxLines = 1
            )
        }
    }
}

/**
 * Default bottom navigation items for InnerTune.
 */
fun defaultBottomNavItems(): List<BottomNavItem> = listOf(
    BottomNavItem(
        label = "Home",
        icon = Icons.Filled.Home,
        route = "home"
    ),
    BottomNavItem(
        label = "Explore",
        icon = Icons.Filled.Explore,
        route = "explore"
    ),
    BottomNavItem(
        label = "Library",
        icon = Icons.Filled.LibraryMusic,
        route = "library"
    ),
    BottomNavItem(
        label = "Profile",
        icon = Icons.Filled.Person,
        route = "profile"
    )
)

/**
 * Compact bottom nav for space-constrained layouts.
 * Shows only icons, labels appear on hover/selection.
 */
@Composable
fun CompactBottomNav(
    items: List<BottomNavItem> = defaultBottomNavItems(),
    selectedIndex: Int = 0,
    onItemSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    GlassmorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        cornerRadius = 20,
        enableBlur = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val iconColor = animateColorAsState(
                    targetValue = if (index == selectedIndex)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    animationSpec = tween(300),
                    label = "compact_nav_icon_color_$index"
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = { onItemSelected(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp),
                        tint = iconColor.value
                    )
                }
            }
        }
    }
}
