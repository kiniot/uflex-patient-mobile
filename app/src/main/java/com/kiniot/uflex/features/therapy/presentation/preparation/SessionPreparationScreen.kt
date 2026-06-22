package com.kiniot.uflex.features.therapy.presentation.preparation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.kiniot.uflex.features.device.domain.model.BleConnectionState
import com.kiniot.uflex.features.therapy.presentation.preparation.SessionPreparationUiState.Phase

@Composable
fun SessionPreparationScreen(
    paddingValues: PaddingValues,
    onBack: () -> Unit,
    onNavigateToExecution: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionPreparationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var permissionDenied by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.dismiss.collect { onBack() }
    }

    val blePermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            permissionDenied = false
            pendingAction?.invoke()
        } else {
            permissionDenied = true
        }
        pendingAction = null
    }
    val withPermissions: (() -> Unit) -> Unit = { action ->
        permissionDenied = false
        pendingAction = action
        permissionLauncher.launch(blePermissions)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val step = uiState.phase.stepIndex()
        if (step >= 0) StepIndicator(currentStep = step)

        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            when (uiState.phase) {
                Phase.Loading -> CircularProgressIndicator()

                Phase.NoRoutine -> InfoState(
                    title = stringResource(R.string.therapy_no_routine_title),
                    subtitle = stringResource(R.string.therapy_no_routine_subtitle),
                    onBack = onBack
                )

                Phase.NoDevice -> InfoState(
                    title = stringResource(R.string.therapy_no_device_title),
                    subtitle = stringResource(R.string.therapy_no_device_subtitle),
                    onBack = onBack
                )

                Phase.Summary -> SummaryContent(
                    totalSeries = uiState.totalSeries,
                    minutes = uiState.estimatedDurationMinutes,
                    deviceSerial = uiState.deviceSerial,
                    permissionDenied = permissionDenied,
                    onBegin = { withPermissions { viewModel.onBegin() } }
                )

                Phase.Resume -> CenteredCard {
                    Text(
                        stringResource(R.string.therapy_resume_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.therapy_resume_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PermissionHint(permissionDenied)
                    Button(
                        onClick = { withPermissions { viewModel.onResumeConnect() } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) { Text(stringResource(R.string.therapy_continue)) }
                }

                Phase.Initiating -> Progress(stringResource(R.string.therapy_initiating))

                Phase.Connecting -> Progress(uiState.connectionState.toUiText().asString(LocalContext.current))

                Phase.AwaitingSensors -> SensorsContent(onConfirm = viewModel::onConfirmSensors)

                Phase.Starting -> Progress(stringResource(R.string.therapy_starting))

                Phase.Started -> StartedContent(
                    connected = uiState.connectionState is BleConnectionState.Connected,
                    permissionDenied = permissionDenied,
                    onReconnect = { withPermissions { viewModel.onReconnect() } },
                    onContinue = { uiState.sessionId?.let(onNavigateToExecution) },
                    onDone = onBack
                )

                Phase.Failed -> FailedContent(
                    message = uiState.errorMessage?.asString(LocalContext.current)
                        ?: uiState.connectionState.let {
                            if (it is BleConnectionState.Failed) {
                                it.toUiText().asString(LocalContext.current)
                            } else {
                                stringResource(R.string.therapy_failed_title)
                            }
                        },
                    onRetry = viewModel::onRetry
                )
            }
        }

        if (uiState.sessionId != null) {
            TextButton(onClick = viewModel::onCancel, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.therapy_cancel_session),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(4) { index ->
            val color = if (index <= currentStep) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(3.dp),
                    color = color
                ) {}
            }
        }
    }
}

@Composable
private fun SummaryContent(
    totalSeries: Int,
    minutes: Int,
    deviceSerial: String?,
    permissionDenied: Boolean,
    onBegin: () -> Unit
) {
    CenteredCard {
        Text(
            stringResource(R.string.therapy_summary_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            stringResource(R.string.therapy_summary_detail, totalSeries, minutes),
            style = MaterialTheme.typography.bodyLarge
        )
        if (deviceSerial != null) {
            Text(
                stringResource(R.string.therapy_summary_device, deviceSerial),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        PermissionHint(permissionDenied)
        Button(
            onClick = onBegin,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) { Text(stringResource(R.string.therapy_begin)) }
    }
}

@Composable
private fun SensorsContent(onConfirm: () -> Unit) {
    var checked by remember { mutableStateOf(false) }
    CenteredCard {
        Text(
            stringResource(R.string.therapy_sensors_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = { checked = it })
            Text(
                stringResource(R.string.therapy_sensors_checkbox),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Button(
            onClick = onConfirm,
            enabled = checked,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) { Text(stringResource(R.string.therapy_start)) }
    }
}

@Composable
private fun StartedContent(
    connected: Boolean,
    permissionDenied: Boolean,
    onReconnect: () -> Unit,
    onContinue: () -> Unit,
    onDone: () -> Unit
) {
    CenteredCard(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            if (connected) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(56.dp)
        )
        Text(
            stringResource(R.string.therapy_started_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            stringResource(
                if (connected) R.string.therapy_started_connected
                else R.string.therapy_started_disconnected
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!connected) {
            PermissionHint(permissionDenied)
            OutlinedButton(
                onClick = onReconnect,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) { Text(stringResource(R.string.therapy_reconnect)) }
        }
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Text(stringResource(R.string.therapy_exec_continue))
        }
        TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.therapy_done))
        }
    }
}

@Composable
private fun FailedContent(message: String, onRetry: () -> Unit) {
    CenteredCard(horizontalAlignment = Alignment.CenterHorizontally) {
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
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Text(stringResource(R.string.therapy_retry))
        }
    }
}

@Composable
private fun InfoState(title: String, subtitle: String, onBack: () -> Unit) {
    CenteredCard(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Text(stringResource(R.string.therapy_prep_back))
        }
    }
}

@Composable
private fun Progress(label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator()
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PermissionHint(visible: Boolean) {
    if (visible) {
        Text(
            stringResource(R.string.therapy_permission_needed),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun CenteredCard(
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }
}
