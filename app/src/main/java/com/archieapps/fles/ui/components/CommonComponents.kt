package com.archieapps.fles.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.fles.ui.theme.FlesTheme
import com.archieapps.fles.ui.theme.withAlpha

enum class AppButtonVariant {
    Default,
    Destructive,
    Outline,
    Secondary,
    Ghost
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Default,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    val colors = FlesTheme.colors
    val bg = when (variant) {
        AppButtonVariant.Default -> colors.primary
        AppButtonVariant.Destructive -> colors.destructive
        AppButtonVariant.Outline -> colors.background
        AppButtonVariant.Secondary -> colors.secondary
        AppButtonVariant.Ghost -> Color.Transparent
    }
    val fg = when (variant) {
        AppButtonVariant.Default -> colors.primaryForeground
        AppButtonVariant.Destructive -> colors.destructiveForeground
        AppButtonVariant.Outline -> colors.foreground
        AppButtonVariant.Secondary -> colors.secondaryForeground
        AppButtonVariant.Ghost -> colors.foreground
    }
    val border = if (variant == AppButtonVariant.Outline) BorderStroke(1.dp, colors.input) else null
    Surface(
        modifier = modifier
            .height(36.dp)
            .alpha(if (enabled && !loading) 1f else 0.5f)
            .clickable(enabled = enabled && !loading, onClick = onClick),
        shape = RoundedCornerShape(FlesTheme.radius.md),
        color = bg,
        border = border,
        shadowElevation = if (variant == AppButtonVariant.Ghost) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = fg)
            } else {
                if (leadingIcon != null) {
                    Icon(leadingIcon, null, modifier = Modifier.size(16.dp), tint = fg)
                    Spacer(Modifier.width(8.dp))
                }
                Text(text, color = fg, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }
    }
}

@Composable
fun IconButtonBox(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = FlesTheme.colors.foreground,
    background: Color = Color.Transparent,
    size: Dp = 36.dp,
    iconSize: Dp = 20.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(FlesTheme.radius.md))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, modifier = Modifier.size(iconSize), tint = tint)
    }
}

@Composable
fun AppInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    singleLine: Boolean = true,
    minHeight: Dp = 36.dp,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val colors = FlesTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .border(1.dp, if (focused) colors.ring else colors.input, RoundedCornerShape(FlesTheme.radius.md))
            .padding(horizontal = 12.dp, vertical = if (minHeight > 40.dp) 8.dp else 0.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, null, modifier = Modifier.size(16.dp), tint = colors.mutedForeground)
            Spacer(Modifier.width(8.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(color = colors.foreground, fontSize = 14.sp, lineHeight = 20.sp),
            singleLine = singleLine,
            cursorBrush = SolidColor(colors.primary),
            interactionSource = interactionSource,
            visualTransformation = visualTransformation,
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(placeholder, color = colors.mutedForeground, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.shadow(1.dp, RoundedCornerShape(FlesTheme.radius.xl)),
        shape = RoundedCornerShape(FlesTheme.radius.xl),
        color = FlesTheme.colors.card,
        border = BorderStroke(1.dp, FlesTheme.colors.border),
        content = content
    )
}

@Composable
fun Badge(
    text: String,
    modifier: Modifier = Modifier,
    active: Boolean = false
) {
    val colors = FlesTheme.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(FlesTheme.radius.full),
        color = if (active) colors.primary else Color.Transparent,
        border = BorderStroke(1.dp, if (active) colors.primary else colors.border)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (active) colors.primaryForeground else colors.mutedForeground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    description: String?,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Inbox
) {
    val colors = FlesTheme.colors
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.muted),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(24.dp), tint = colors.mutedForeground)
        }
        Spacer(Modifier.height(16.dp))
        Text(title, color = colors.foreground, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        if (description != null) {
            Spacer(Modifier.height(4.dp))
            Text(description, color = colors.mutedForeground, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun Skeleton(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    radius: Dp = FlesTheme.radius.md
) {
    val alpha by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "skeletonAlpha"
    )
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(FlesTheme.colors.muted.withAlpha(alpha))
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        color = FlesTheme.colors.foreground,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
