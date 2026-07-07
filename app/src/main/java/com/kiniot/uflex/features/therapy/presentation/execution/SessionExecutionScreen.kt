package com.kiniot.uflex.features.therapy.presentation.execution

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiniot.uflex.R
import com.kiniot.uflex.core.designsystem.components.HaloIcon
import com.kiniot.uflex.core.designsystem.components.Pill
import com.kiniot.uflex.core.designsystem.components.RadialGauge
import com.kiniot.uflex.core.designsystem.theme.ExtendedTheme
import com.kiniot.uflex.core.ui.asString
import com.kiniot.uflex.features.plan.domain.model.Exercise
import com.kiniot.uflex.features.plan.presentation.detail.ExerciseVideoPlayer
import com.kiniot.uflex.features.plan.presentation.exercises.toUiText
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
        if (running == null && next != null) {
            UpcomingExerciseCard(
                exercise = uiState.upcomingExercise,
                isLoading = uiState.isUpcomingExerciseLoading
            )
        }
        when {
            uiState.allSeriesCompleted -> Button(
                onClick = onFinalize,
                enabled = !uiState.isFinalizing,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.therapy_exec_finalize))
            }

            running == null && next != null -> Button(
                onClick = onStartSerie,
                enabled = !uiState.isStartingSerie && !uiState.isCalibrating,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.therapy_exec_start_serie))
            }
        }

        OutlinedButton(
            onClick = onReportPain,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Default.MonitorHeart, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.therapy_exec_report_pain))
        }

        if (uiState.phase == Phase.Active) {
            TextButton(
                onClick = onTerminate,
                enabled = !uiState.isTerminating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    stringResource(R.string.therapy_cancel_session),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
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
        val degreesTemplate = stringResource(R.string.therapy_exec_degrees)
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            RadialGauge(
                degrees = flexionDegrees,
                valueLabel = { String.format(degreesTemplate, it) }
            )
        }
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
    val dotColor = if (active) ExtendedTheme.colors.success.color else MaterialTheme.colorScheme.outlineVariant
    Surface(
        shape = RoundedCornerShape(50),
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
            // Same guard as the original fraction computation: an invalid target renders a single
            // empty segment instead of dividing by (or indexing against) a non-positive count.
            val segmentCount = if (running.targetRepetitions > 0) running.targetRepetitions else 1
            val filledSegments = if (running.targetRepetitions > 0) {
                running.currentRepetitions.coerceIn(0, running.targetRepetitions)
            } else {
                0
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(segmentCount) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .background(
                                color = if (index < filledSegments) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
            }
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Default.BluetoothDisabled,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    stringResource(R.string.therapy_exec_disconnected_banner),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            OutlinedButton(onClick = onReconnect, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Text(stringResource(R.string.therapy_reconnect))
            }
        }
    }
}

@Composable
private fun UpcomingExerciseCard(exercise: Exercise?, isLoading: Boolean) {
    Card {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HaloIcon(
                icon = Icons.Outlined.PlayCircle,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                size = 48.dp,
                iconSize = 24.dp
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.therapy_exec_upcoming_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = exercise?.name ?: stringResource(R.string.therapy_exec_upcoming_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        when {
            isLoading -> UpcomingExerciseLoading()
            exercise?.videoUrl != null -> ExerciseVideoPlayer(
                videoUrl = exercise.videoUrl,
                playWhenReady = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(18.dp))
            )
            exercise != null -> ExerciseVideoUnavailable()
        }

        exercise?.let {
            val context = LocalContext.current
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(it.bodyPart.toUiText().asString(context))
                Pill(it.movementType.toUiText().asString(context))
            }
        }

        Text(
            text = stringResource(R.string.therapy_exec_upcoming_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UpcomingExerciseLoading() {
    Surface(
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun ExerciseVideoUnavailable() {
    Surface(
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {
            HaloIcon(
                icon = Icons.Outlined.Videocam,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 52.dp,
                iconSize = 24.dp
            )
            Text(
                text = stringResource(R.string.therapy_exec_video_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PainDialog(onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var level by remember { mutableFloatStateOf(0f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.MonitorHeart, contentDescription = null) },
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
        icon = { Icon(Icons.Default.Warning, contentDescription = null) },
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
        icon = { Icon(Icons.Default.Sensors, contentDescription = null) },
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
        HaloIcon(
            icon = Icons.Default.ErrorOutline,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
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
