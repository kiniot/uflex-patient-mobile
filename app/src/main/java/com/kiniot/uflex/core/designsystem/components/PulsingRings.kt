package com.kiniot.uflex.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Concentric rings that expand and fade outward on a loop, with [content] (typically a [HaloIcon])
 * pinned at the center — the "linking…" pulse. Ring count is fixed to keep the composable-call
 * count stable across recompositions.
 */
@Composable
fun PulsingRings(
    modifier: Modifier = Modifier,
    ringColor: Color = MaterialTheme.colorScheme.primary,
    ringCount: Int = 3,
    diameter: Dp = 220.dp,
    content: @Composable () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val durationMs = 1800
    val progresses = (0 until ringCount).map { i ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = durationMs, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(offsetMillis = i * (durationMs / ringCount))
            ),
            label = "ring$i"
        )
    }
    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val maxRadius = size.minDimension / 2f
            val stroke = 3.dp.toPx()
            progresses.forEach { state ->
                val t = state.value
                drawCircle(
                    color = ringColor.copy(alpha = (1f - t) * 0.5f),
                    radius = maxRadius * (0.35f + 0.65f * t),
                    style = Stroke(width = stroke)
                )
            }
        }
        content()
    }
}
