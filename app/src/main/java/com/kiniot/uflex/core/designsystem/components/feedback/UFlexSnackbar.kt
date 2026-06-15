package com.kiniot.uflex.core.designsystem.components.feedback

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kiniot.uflex.core.designsystem.theme.ExtendedTheme

@Composable
fun UFlexSnackbarHost(
    snackbarState: UFlexSnackbarState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = snackbarState.hostState,
        modifier = modifier,
        snackbar = { data ->
            UFlexSnackbar(
                snackbarData = data,
                type = snackbarState.currentType.value
            )
        }
    )
}

@Composable
private fun UFlexSnackbar(
    snackbarData: SnackbarData,
    type: SnackbarType,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor) = snackbarColors(type)
    val icon = snackbarIcon(type)

    Snackbar(
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        actionContentColor = contentColor,
        action = snackbarData.visuals.actionLabel?.let { actionLabel ->
            {
                TextButton(onClick = { snackbarData.performAction() }) {
                    Text(text = actionLabel, color = contentColor)
                }
            }
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = snackbarData.visuals.message,
                color = contentColor
            )
        }
    }
}

@Composable
private fun snackbarColors(type: SnackbarType): Pair<Color, Color> {
    val extendedColors = ExtendedTheme.colors
    val colors = MaterialTheme.colorScheme

    return when (type) {
        SnackbarType.Success -> extendedColors.success.colorContainer to extendedColors.success.onColorContainer
        SnackbarType.Error -> colors.errorContainer to colors.onErrorContainer
        SnackbarType.Warning -> extendedColors.warning.colorContainer to extendedColors.warning.onColorContainer
        SnackbarType.Info -> extendedColors.info.colorContainer to extendedColors.info.onColorContainer
    }
}

private fun snackbarIcon(type: SnackbarType): ImageVector {
    return when (type) {
        SnackbarType.Success -> Icons.Filled.CheckCircle
        SnackbarType.Error -> Icons.Filled.Error
        SnackbarType.Warning -> Icons.Filled.Warning
        SnackbarType.Info -> Icons.Filled.Info
    }
}
