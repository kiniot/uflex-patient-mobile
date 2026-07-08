package com.kiniot.uflex.features.device.presentation

import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kiniot.uflex.R
import com.kiniot.uflex.core.designsystem.theme.ExtendedTheme
import com.kiniot.uflex.core.ui.UiText
import com.kiniot.uflex.core.ui.asString
import com.kiniot.uflex.features.device.domain.model.BleConnectionState
import com.kiniot.uflex.features.device.domain.model.DeviceCalibrationStatus
import com.kiniot.uflex.features.device.domain.model.LedColor

/** An icon centered inside a soft circular container. Used for the wizard/hero visuals. */
@Composable
fun HaloIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    size: Dp = 96.dp,
    iconSize: Dp = 44.dp,
    haloAlpha: Float = 1f
) {
    Box(
        modifier = modifier
            .size(size)
            .background(containerColor.copy(alpha = haloAlpha), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(iconSize))
    }
}

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

@Composable
fun CalibrationChip(status: DeviceCalibrationStatus) {
    val (container, content) = when (status) {
        DeviceCalibrationStatus.Valid ->
            ExtendedTheme.colors.success.colorContainer to ExtendedTheme.colors.success.onColorContainer
        DeviceCalibrationStatus.NeedsCalibration ->
            ExtendedTheme.colors.warning.colorContainer to ExtendedTheme.colors.warning.onColorContainer
        DeviceCalibrationStatus.Unknown ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    ColoredPill(text = calibrationText(status).asString(LocalContext.current), container = container, content = content)
}

@Composable
fun ConnectionChip(state: BleConnectionState) {
    val extended = ExtendedTheme.colors
    val scheme = MaterialTheme.colorScheme
    val (container, content, labelRes) = when (state) {
        is BleConnectionState.Connected ->
            Triple(extended.success.colorContainer, extended.success.onColorContainer, R.string.device_conn_connected)
        is BleConnectionState.Scanning,
        is BleConnectionState.Connecting,
        is BleConnectionState.ConfirmingIdentity ->
            Triple(extended.info.colorContainer, extended.info.onColorContainer, R.string.device_conn_connecting)
        else ->
            Triple(scheme.surfaceVariant, scheme.onSurfaceVariant, R.string.device_conn_disconnected)
    }
    ColoredPill(text = stringResource(labelRes), container = container, content = content)
}

/**
 * The kit-LED accent (the status "dot") as a single color, mapped with the same semantics as
 * [KitStatusChip]. Reused by the in-session avatar to tint the active-joint halo so the live
 * feedback stays consistent with the chip.
 */
@Composable
fun ledAccentColor(ledColor: LedColor?): Color {
    val extended = ExtendedTheme.colors
    val scheme = MaterialTheme.colorScheme
    return when (ledColor) {
        LedColor.Green -> extended.success.color
        LedColor.Blue, LedColor.Cyan -> extended.info.color
        LedColor.Yellow -> extended.warning.color
        LedColor.Red, LedColor.Magenta -> scheme.error
        LedColor.Off, LedColor.Unknown, null -> scheme.outline
    }
}

@Composable
fun KitStatusChip(ledColor: LedColor?) {
    val extended = ExtendedTheme.colors
    val scheme = MaterialTheme.colorScheme
    val dot = ledAccentColor(ledColor)
    val (container, content, labelRes) = when (ledColor) {
        LedColor.Green ->
            Triple(extended.success.colorContainer, extended.success.onColorContainer,
                R.string.device_kit_status_ready)
        LedColor.Blue ->
            Triple(extended.info.colorContainer, extended.info.onColorContainer,
                R.string.device_kit_status_syncing)
        LedColor.Cyan ->
            Triple(extended.info.colorContainer, extended.info.onColorContainer,
                R.string.device_kit_status_calibrating)
        LedColor.Yellow ->
            Triple(extended.warning.colorContainer, extended.warning.onColorContainer,
                R.string.device_kit_status_waiting_context)
        LedColor.Red ->
            Triple(scheme.errorContainer, scheme.onErrorContainer,
                R.string.device_kit_status_safety_alert)
        LedColor.Magenta ->
            Triple(scheme.errorContainer, scheme.onErrorContainer,
                R.string.device_kit_status_error)
        LedColor.Off, LedColor.Unknown, null ->
            Triple(scheme.surfaceVariant, scheme.onSurfaceVariant,
                R.string.device_kit_status_unknown)
    }
    Surface(shape = RoundedCornerShape(50), color = container) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.size(8.dp).background(dot, CircleShape))
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = content
            )
        }
    }
}

@Composable
private fun ColoredPill(text: String, container: Color, content: Color) {
    Surface(shape = RoundedCornerShape(50), color = container) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = content,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

/** Battery indicator. The value is the backend's last-known reading, not live BLE. */
@Composable
fun BatteryRow(
    level: Int,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(batteryIcon(level), contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(
            text = stringResource(R.string.device_battery_value, level),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = tint
        )
        Text(
            text = stringResource(R.string.device_battery_last_known),
            style = MaterialTheme.typography.bodySmall,
            color = tint.copy(alpha = 0.8f)
        )
    }
}

private fun batteryIcon(level: Int): ImageVector = when {
    level >= 95 -> Icons.Filled.BatteryFull
    level <= 15 -> Icons.Filled.BatteryAlert
    else -> Icons.Filled.BatteryStd
}

/** "Paso X de Y" + title + subtitle, centered — shared by the two wizard steps. */
@Composable
fun WizardStepHeader(
    @StringRes stepRes: Int,
    @StringRes titleRes: Int,
    @StringRes subtitleRes: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
            Text(
                text = stringResource(stepRes),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(subtitleRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

fun connectionReasonText(reason: BleConnectionState.Failed.Reason): UiText = UiText.Resource(
    when (reason) {
        BleConnectionState.Failed.Reason.DeviceNotFound -> R.string.device_error_device_not_found
        BleConnectionState.Failed.Reason.ConnectionLost -> R.string.device_error_connection_lost
        BleConnectionState.Failed.Reason.IdentityMismatch -> R.string.device_error_identity_mismatch
        BleConnectionState.Failed.Reason.MissingService -> R.string.device_error_missing_service
        BleConnectionState.Failed.Reason.BluetoothUnavailable -> R.string.device_error_bluetooth_unavailable
        BleConnectionState.Failed.Reason.PermissionDenied -> R.string.device_error_permission_denied
        BleConnectionState.Failed.Reason.Unknown -> R.string.device_error_unknown
    }
)

fun calibrationText(status: DeviceCalibrationStatus): UiText = UiText.Resource(
    when (status) {
        DeviceCalibrationStatus.Valid -> R.string.device_calibration_valid
        DeviceCalibrationStatus.NeedsCalibration -> R.string.device_calibration_needs
        DeviceCalibrationStatus.Unknown -> R.string.device_calibration_unknown
    }
)
