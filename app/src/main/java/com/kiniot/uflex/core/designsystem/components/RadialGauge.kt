package com.kiniot.uflex.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// Fixed, decorative-only highlight range near the top of the sweep — no clinical meaning,
// purely a visual accent band. Never derived from a prop.
private const val ZONE_FRACTION_START = 0.72f
private const val ZONE_FRACTION_END = 0.88f

/**
 * Semicircular gauge reading [degrees] against [minDegrees]..[maxDegrees]. Purely presentational:
 * when [degrees] is null it renders just the track and an em dash — callers own all gating logic
 * (calibration/connection guards); this component never re-derives them.
 */
@Composable
fun RadialGauge(
    degrees: Float?,
    modifier: Modifier = Modifier,
    minDegrees: Float = 0f,
    maxDegrees: Float = 180f,
    diameter: Dp = 200.dp,
    strokeWidth: Dp = 18.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    arcColor: Color = MaterialTheme.colorScheme.primary,
    zoneColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
    valueLabel: (Int) -> String
) {
    val fraction = degrees?.let { ((it - minDegrees) / (maxDegrees - minDegrees)).coerceIn(0f, 1f) } ?: 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 500),
        label = "radialGauge"
    )
    Box(
        modifier = modifier.size(width = diameter, height = diameter / 2 + strokeWidth).clipToBounds(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = strokeWidth.toPx()
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            val arcSize = Size(size.width - stroke, size.width - stroke)

            // Decorative zone band, drawn first so the track/value arcs sit on top of it.
            drawArc(
                color = zoneColor,
                startAngle = 180f + 180f * ZONE_FRACTION_START,
                sweepAngle = 180f * (ZONE_FRACTION_END - ZONE_FRACTION_START),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            drawArc(
                color = trackColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = arcColor,
                startAngle = 180f,
                sweepAngle = 180f * animatedFraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            text = if (degrees != null) valueLabel(degrees.roundToInt()) else "—",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (degrees != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
