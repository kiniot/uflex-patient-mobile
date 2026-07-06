package com.kiniot.uflex.features.therapy.presentation.execution

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiniot.uflex.R
import com.kiniot.uflex.core.ui.asString
import com.kiniot.uflex.features.therapy.presentation.execution.SessionExecutionUiState.Phase
import kotlin.math.roundToInt

@Composable
fun SessionExecutionScreen(
    paddingValues: PaddingValues,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionExecutionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.dismiss.collect { onBack() }
    }

    // Intercept system back while the session is running so leaving confirms termination
    // (the top-bar arrow routes through the same viewModel.onBackPressed()).
    BackHandler(enabled = uiState.phase == Phase.Active) { viewModel.onBackPressed() }

    val blePermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> if (result.values.all { it }) viewModel.onReconnect() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        when (uiState.phase) {
            Phase.Loading -> CircularProgressIndicator()

            Phase.Failed -> FailedContent(
                message = uiState.errorMessage?.asString(LocalContext.current)
                    ?: stringResource(R.string.therapy_failed_title),
                onBack = onBack
            )

            Phase.Active, Phase.Finished -> ActiveContent(
                uiState = uiState,
                onStartSerie = viewModel::onRequestStartSerie,
                onFinalize = viewModel::onFinalize,
                onReportPain = viewModel::onShowPainDialog,
                onReconnect = { permissionLauncher.launch(blePermissions) },
                onTerminate = viewModel::onRequestTerminate
            )
        }
    }

    if (uiState.painDialogVisible) {
        PainDialog(
            onConfirm = viewModel::onReportPain,
            onDismiss = viewModel::onDismissPainDialog
        )
    }

    if (uiState.terminateDialogVisible) {
        TerminateDialog(
            onConfirm = viewModel::onConfirmTerminate,
            onDismiss = viewModel::onDismissTerminate
        )
    }

    if (uiState.calibrationPromptVisible) {
        CalibrationPromptDialog(
            onConfirm = viewModel::onConfirmStartSerie,
            onDismiss = viewModel::onDismissCalibrationPrompt
        )
    }

    if (uiState.isCalibrating) {
        CalibratingDialog()
    }
}

@Composable
private fun ActiveContent(
    uiState: SessionExecutionUiState,
    onStartSerie: () -> Unit,
    onFinalize: () -> Unit,
    onReportPain: () -> Unit,
    onReconnect: () -> Unit,
    onTerminate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!uiState.connected) {
            DisconnectedBanner(onReconnect = onReconnect)
        }

        GaugeCard(uiState = uiState)

        SerieProgressCard(uiState = uiState)

        val running = uiState.runningSerie
        val next = uiState.nextPendingSerie
        when {
            uiState.allSeriesCompleted -> Button(
                onClick = onFinalize,
                enabled = !uiState.isFinalizing,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) { Text(stringResource(R.string.therapy_exec_finalize)) }

            running == null && next != null -> Button(
                onClick = onStartSerie,
                enabled = !uiState.isStartingSerie && !uiState.isCalibrating,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) { Text(stringResource(R.string.therapy_exec_start_serie)) }
        }

        OutlinedButton(
            onClick = onReportPain,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) { Text(stringResource(R.string.therapy_exec_report_pain)) }

        if (uiState.phase == Phase.Active) {
            TextButton(
                onClick = onTerminate,
                enabled = !uiState.isTerminating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.therapy_cancel_session),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun GaugeCard(uiState: SessionExecutionUiState) {
    Card {
        Text(
            stringResource(R.string.therapy_exec_angle),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val telemetry = uiState.latestTelemetry
        // The kit sends the calibrated, gravity-anchored flexion of the active joint; show it only
        // once calibrated (reads 0 at the reference pose) and connected. "—" otherwise.
        val flexionDegrees = if (telemetry != null && telemetry.isCalibrated && uiState.connected) {
            telemetry.jointFlexionDegrees
        } else {
            null
        }
        Text(
            text = if (flexionDegrees != null) {
                stringResource(R.string.therapy_exec_degrees, flexionDegrees.roundToInt())
            } else {
                "—"
            },
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            stringResource(R.string.therapy_exec_actuators),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActuatorChip(label = stringResource(R.string.therapy_exec_led), active = telemetry?.ledColor?.name?.let { it != "Off" && it != "Unknown" } == true)
            ActuatorChip(label = stringResource(R.string.therapy_exec_buzzer), active = telemetry?.buzzerActive == true)
            ActuatorChip(label = stringResource(R.string.therapy_exec_vibration), active = telemetry?.vibrationActive == true)
        }
    }
}

@Composable
private fun ActuatorChip(label: String, active: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SerieProgressCard(uiState: SessionExecutionUiState) {
    val progress = uiState.progress ?: return
    Card {
        if (uiState.allSeriesCompleted) {
            Text(
                stringResource(R.string.therapy_exec_all_done),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            return@Card
        }
        val currentIndex = (progress.completedSeries + 1).coerceAtMost(progress.totalSeries)
        Text(
            stringResource(R.string.therapy_exec_serie_progress, currentIndex, progress.totalSeries),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        val running = uiState.runningSerie
        if (running != null) {
            Text(
                stringResource(R.string.therapy_exec_running),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.therapy_exec_reps, running.currentRepetitions, running.targetRepetitions),
                style = MaterialTheme.typography.bodyLarge
            )
            val fraction = if (running.targetRepetitions > 0) {
                (running.currentRepetitions.toFloat() / running.targetRepetitions).coerceIn(0f, 1f)
            } else {
                0f
            }
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                stringResource(R.string.therapy_exec_waiting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DisconnectedBanner(onReconnect: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.therapy_exec_disconnected_banner),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            OutlinedButton(onClick = onReconnect, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Text(stringResource(R.string.therapy_reconnect))
            }
        }
    }
}

@Composable
private fun PainDialog(onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var level by remember { mutableFloatStateOf(0f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.therapy_exec_pain_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.therapy_exec_pain_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = level.roundToInt().toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                )
                Slider(value = level, onValueChange = { level = it }, valueRange = 0f..10f, steps = 9)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(level.roundToInt()) }) {
                Text(stringResource(R.string.therapy_exec_pain_send))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.therapy_exec_cancel)) }
        }
    )
}

@Composable
private fun TerminateDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.therapy_exec_terminate_title)) },
        text = {
            Text(
                stringResource(R.string.therapy_exec_terminate_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.therapy_exec_terminate_confirm),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.therapy_exec_cancel)) }
        }
    )
}

@Composable
private fun CalibrationPromptDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.therapy_exec_calibrate_title)) },
        text = {
            Text(
                stringResource(R.string.therapy_exec_calibrate_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.therapy_exec_calibrate_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.therapy_exec_cancel)) }
        }
    )
}

@Composable
private fun CalibratingDialog() {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.therapy_exec_calibrate_title)) },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    stringResource(R.string.therapy_exec_calibrating),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun FailedContent(message: String, onBack: () -> Unit) {
    Card {
        Text(
            stringResource(R.string.therapy_failed_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Text(stringResource(R.string.therapy_prep_back))
        }
    }
}

@Composable
private fun Card(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}
