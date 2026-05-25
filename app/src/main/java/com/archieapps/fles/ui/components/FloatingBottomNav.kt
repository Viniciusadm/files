package com.archieapps.fles.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.fles.ui.theme.FlesTheme
import com.archieapps.fles.ui.theme.withAlpha

enum class MainTab {
    Messages,
    Gallery,
    Files
}

private data class TabSpec(
    val tab: MainTab,
    val label: String,
    val icon: ImageVector
)

private val TabSpecs = listOf(
    TabSpec(MainTab.Messages, "Conversas", Icons.Filled.ChatBubbleOutline),
    TabSpec(MainTab.Gallery, "Galeria", Icons.Filled.PhotoLibrary),
    TabSpec(MainTab.Files, "Arquivos", Icons.Filled.FolderOpen)
)

@Composable
fun FloatingBottomNav(
    current: MainTab,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FlesTheme.colors
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp)
                .shadow(elevation = 14.dp, shape = RoundedCornerShape(FlesTheme.radius.full), clip = false),
            shape = RoundedCornerShape(FlesTheme.radius.full),
            color = colors.card
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabSpecs.forEach { spec ->
                    BottomNavItem(
                        spec = spec,
                        active = spec.tab == current,
                        onClick = { onSelect(spec.tab) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    spec: TabSpec,
    active: Boolean,
    onClick: () -> Unit
) {
    val colors = FlesTheme.colors
    val springColor = spring<Color>(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
    val springDp = spring<androidx.compose.ui.unit.Dp>(dampingRatio = 0.78f, stiffness = 320f)
    val background by animateColorAsState(
        targetValue = if (active) colors.primary.withAlpha(0.14f) else Color.Transparent,
        animationSpec = springColor,
        label = "navItemBg"
    )
    val foreground by animateColorAsState(
        targetValue = if (active) colors.primary else colors.mutedForeground,
        animationSpec = springColor,
        label = "navItemFg"
    )
    val horizontalPad by animateDpAsState(
        targetValue = if (active) 16.dp else 12.dp,
        animationSpec = springDp,
        label = "navItemPad"
    )
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(FlesTheme.radius.full))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPad),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(spec.icon, null, tint = foreground, modifier = Modifier.width(20.dp).height(20.dp))
        AnimatedVisibility(
            visible = active,
            enter = slideInHorizontally(
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 320f),
                initialOffsetX = { -it / 3 }
            ) + fadeIn(tween(260)),
            exit = slideOutHorizontally(
                animationSpec = tween(180),
                targetOffsetX = { -it / 3 }
            ) + fadeOut(tween(140))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(8.dp))
                Text(
                    spec.label,
                    color = foreground,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}
