package com.zionhuang.music.ui.component

// Note: RenderEffect-based blur removed for broad compatibility. Keep API for future use.
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.zionhuang.music.constants.PremiumBlurKey
import com.zionhuang.music.constants.BlurType
import com.zionhuang.music.constants.BlurTypeKey
import com.zionhuang.music.utils.rememberPreference
import com.zionhuang.music.utils.rememberEnumPreference
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


/**
 * Device-aware glass modifier: gradient + optional blur (API 31+).
 */
@Composable
fun Modifier.glass(
    color: Color,
    alpha: Float = 0.2f,
    cornerRadius: Dp = 24.dp,
    blurRadius: Float = 20f,
    enableBlur: Boolean = true,
): Modifier {
    var m = this
        .clip(RoundedCornerShape(cornerRadius))
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    color.copy(alpha = alpha),
                    color.copy(alpha = alpha * 0.9f)
                )
            )
        )
        .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            shape = RoundedCornerShape(cornerRadius)
        )

    // Blur via RenderEffect is intentionally omitted for now to avoid platform compatibility issues.

    return m
}


/**
 * Glassmorphic card with blur effect and semi-transparent background.
 * Adapts to device capabilities (full glass on high-end, solid on low-end).
 */
@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    enableBlur: Boolean = true,
    cornerRadius: Int = 24,
    content: @Composable () -> Unit,
) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    val (premiumBlur, _) = rememberPreference(PremiumBlurKey, defaultValue = false)
    val actualEnableBlur = enableBlur && premiumBlur
    val (blurType, _) = rememberEnumPreference(BlurTypeKey, defaultValue = BlurType.FROSTED)

    val alpha = when {
        actualEnableBlur && blurType == BlurType.FROSTED -> 0.2f
        actualEnableBlur && blurType == BlurType.LIQUID_IOS -> 0.12f
        else -> 0.4f
    }

    Box(
        modifier = modifier.glass(
            color = backgroundColor,
            alpha = alpha,
            cornerRadius = cornerRadius.dp,
            blurRadius = 20f,
            enableBlur = actualEnableBlur
        )
    ) {
        content()
    }
}

/**
 * Glassmorphic button with ripple effect and premium styling.
 */
@Composable
fun GlassmorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enableBlur: Boolean = true,
    cornerRadius: Int = 16,
    content: @Composable () -> Unit,
) {
    GlassmorphicCard(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            ),
        enableBlur = enableBlur,
        cornerRadius = cornerRadius,
        content = content
    )
}

/**
 * Glassmorphic surface - similar to card but for larger containers
 */
@Composable
fun GlassmorphicSurface(
    modifier: Modifier = Modifier,
    enableBlur: Boolean = true,
    cornerRadius: Int = 32,
    content: @Composable () -> Unit,
) {
    GlassmorphicCard(
        modifier = modifier,
        enableBlur = enableBlur,
        cornerRadius = cornerRadius,
        content = content
    )
}
