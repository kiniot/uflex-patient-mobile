package com.kiniot.uflex.features.device.presentation

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiniot.uflex.R
import com.kiniot.uflex.core.designsystem.theme.ExtendedTheme
import com.kiniot.uflex.core.ui.asString
import com.kiniot.uflex.features.device.domain.model.BleConnectionState
import com.kiniot.uflex.features.device.domain.model.Device

@Composable
fun DeviceConnectionScreen(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: DeviceConnectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }

    val blePermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val enableBtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.onConnect()
    }

    fun connectWhenBluetoothReady() {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        when {
            // No adapter present: let the BLE layer surface BluetoothUnavailable.
            adapter == null -> viewModel.onConnect()
            !adapter.isEnabled -> enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            else -> viewModel.onConnect()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            permissionDenied = false
            connectWhenBluetoothReady()
        } else {
            permissionDenied = true
        }
    }

    // Permissions first (silently proceed if already granted), then ensure Bluetooth is on, then connect.
    fun ensureReadyThenConnect() {
        val hasAll = blePermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (hasAll) {
            permissionDenied = false
            connectWhenBluetoothReady()
        } else {
            permissionLauncher.launch(blePermissions)
        }
    }

    Box(modifier = modifier.fillMaxSize().padding(paddingValues)) {
        when (val check = uiState.deviceCheck) {
            DeviceCheck.Loading -> CenteredLoading()
            DeviceCheck.Failed -> DeviceCheckFailed(onRetry = viewModel::loadAssignedDevice)
            DeviceCheck.None -> NoDeviceView()
            is DeviceCheck.Assigned -> {
                val conn = uiState.connectionState
                when {
                    conn is BleConnectionState.Connected ->
                        ConnectedView(
                            device = check.device,
                            receivingData = uiState.receivingData,
                            onDisconnect = viewModel::onDisconnect
                        )

                    conn is BleConnectionState.Scanning ||
                        conn is BleConnectionState.Connecting ||
                        conn is BleConnectionState.ConfirmingIdentity ->
                        PairingConnecting(onCancel = viewModel::cancelPairing)

                    uiState.inPairing && conn is BleConnectionState.Failed ->
                        PairingError(
                            reason = conn.reason,
                            onRetry = ::ensureReadyThenConnect,
                            onEnableBluetooth = { enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) },
                            onGrantPermission = { permissionLauncher.launch(blePermissions) },
                            onCancel = viewModel::cancelPairing
                        )

                    uiState.inPairing ->
                        PairingTurnOn(
                            permissionDenied = permissionDenied,
                            onNext = ::ensureReadyThenConnect,
                            onCancel = viewModel::cancelPairing
                        )

                    conn is BleConnectionState.Failed &&
                        conn.reason == BleConnectionState.Failed.Reason.ConnectionLost ->
                        AssignedLanding(
                            device = check.device,
                            connectionState = conn,
                            isReconnect = true,
                            onPair = ::ensureReadyThenConnect
                        )

                    else ->
                        AssignedLanding(
                            device = check.device,
                            connectionState = conn,
                            isReconnect = false,
                            onPair = viewModel::startPairing
                        )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Views
// -------------------------------------------------------------------------------------------------

@Composable
private fun NoDeviceView() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        HaloIcon(Icons.Filled.Sensors, size = 96.dp, iconSize = 44.dp)
        Text(
            text = stringResource(R.string.device_none_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.device_none_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AssignedLanding(
    device: Device,
    connectionState: BleConnectionState,
    isReconnect: Boolean,
    onPair: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HaloIcon(Icons.Filled.Sensors, size = 104.dp, iconSize = 50.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.device_landing_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = device.advertisedName.ifBlank { device.model },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        BatteryRow(device.batteryLevel)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalibrationChip(device.calibrationStatus)
            ConnectionChip(connectionState)
        }
        if (isReconnect) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.device_reconnect_banner),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(28.dp))
        Button(onClick = onPair, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Filled.Bluetooth, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(if (isReconnect) R.string.device_cta_reconnect else R.string.device_cta_pair))
        }
    }
}

@Composable
private fun PairingTurnOn(
    permissionDenied: Boolean,
    onNext: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        WizardStepHeader(R.string.device_step1_of, R.string.device_step1_title, R.string.device_step1_subtitle)
        Spacer(Modifier.weight(1f))
        HaloIcon(
            Icons.Filled.Sensors,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            size = 200.dp,
            iconSize = 84.dp,
            haloAlpha = 0.55f
        )
        Spacer(Modifier.weight(1f))
        if (permissionDenied) {
            Text(
                text = stringResource(R.string.device_perm_denied),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
        }
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Text(stringResource(R.string.device_step_next))
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.device_step_cancel))
        }
    }
}

@Composable
private fun PairingConnecting(onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        WizardStepHeader(R.string.device_step2_of, R.string.device_step2_title, R.string.device_step2_subtitle)
        Spacer(Modifier.weight(1f))
        PulsingRings(ringColor = MaterialTheme.colorScheme.primary, diameter = 220.dp) {
            HaloIcon(
                Icons.AutoMirrored.Filled.BluetoothSearching,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                size = 96.dp,
                iconSize = 42.dp
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.device_connecting_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.device_step_cancel))
        }
    }
}

@Composable
private fun PairingError(
    reason: BleConnectionState.Failed.Reason,
    onRetry: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onGrantPermission: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HaloIcon(
            Icons.Filled.ErrorOutline,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            size = 96.dp,
            iconSize = 44.dp
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = connectionReasonText(reason).asString(LocalContext.current),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Text(stringResource(R.string.device_retry))
        }
        if (reason == BleConnectionState.Failed.Reason.BluetoothUnavailable) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onEnableBluetooth, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Text(stringResource(R.string.device_bt_enable))
            }
        }
        if (reason == BleConnectionState.Failed.Reason.PermissionDenied) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onGrantPermission, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Text(stringResource(R.string.device_perm_grant))
            }
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.device_step_cancel))
        }
    }
}

@Composable
private fun ConnectedView(
    device: Device,
    receivingData: Boolean,
    onDisconnect: () -> Unit
) {
    val success = ExtendedTheme.colors.success
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HaloIcon(
            Icons.Filled.CheckCircle,
            containerColor = success.colorContainer,
            contentColor = success.onColorContainer,
            size = 104.dp,
            iconSize = 52.dp
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.device_connected_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.device_connected_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        BatteryRow(device.batteryLevel)
        if (receivingData) {
            Spacer(Modifier.height(10.dp))
            LivenessDot()
        }
        Spacer(Modifier.height(28.dp))
        OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Text(stringResource(R.string.device_disconnect))
        }
    }
}

@Composable
private fun LivenessDot() {
    val success = ExtendedTheme.colors.success
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(8.dp).background(success.color, CircleShape))
        Text(
            text = stringResource(R.string.device_liveness_receiving),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DeviceCheckFailed(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = stringResource(R.string.device_check_failed_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Button(onClick = onRetry, shape = RoundedCornerShape(18.dp)) {
            Text(stringResource(R.string.device_retry))
        }
    }
}

@Composable
private fun CenteredLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
