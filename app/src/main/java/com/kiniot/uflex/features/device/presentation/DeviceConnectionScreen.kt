package com.kiniot.uflex.features.device.presentation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiniot.uflex.features.device.domain.model.BleConnectionState
import com.kiniot.uflex.features.device.domain.model.Device
import com.kiniot.uflex.features.device.domain.model.MotionTelemetry
import com.kiniot.uflex.features.device.domain.model.Quaternion
import java.util.Locale

/**
 * Quick proof-of-concept screen: checks for an assigned device, lets the patient scan/connect
 * over BLE (requesting the runtime permissions), and prints the live telemetry frames once paired.
 * Strings are inline for speed; a real screen would move them to strings_device.xml + UiText.
 */
@Composable
fun DeviceConnectionScreen(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: DeviceConnectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var permissionDenied by remember { mutableStateOf(false) }

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
            viewModel.onConnect()
        } else {
            permissionDenied = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Dispositivo uFlex", style = MaterialTheme.typography.titleLarge)

        when (val check = uiState.deviceCheck) {
            DeviceCheck.Loading -> CenteredProgress()

            DeviceCheck.None -> Text(
                text = "No tienes un dispositivo asignado.",
                style = MaterialTheme.typography.bodyLarge
            )

            DeviceCheck.Failed -> {
                Text(
                    text = "No se pudo verificar tu dispositivo asignado.",
                    style = MaterialTheme.typography.bodyLarge
                )
                OutlinedButton(onClick = { viewModel.loadAssignedDevice() }) {
                    Text("Reintentar")
                }
            }

            is DeviceCheck.Assigned -> AssignedDeviceContent(
                device = check.device,
                connectionState = uiState.connectionState,
                telemetry = uiState.latestTelemetry,
                framesReceived = uiState.framesReceived,
                permissionDenied = permissionDenied,
                onScanAndConnect = {
                    permissionDenied = false
                    permissionLauncher.launch(blePermissions)
                },
                onDisconnect = viewModel::onDisconnect
            )
        }
    }
}

@Composable
private fun AssignedDeviceContent(
    device: Device,
    connectionState: BleConnectionState,
    telemetry: MotionTelemetry?,
    framesReceived: Int,
    permissionDenied: Boolean,
    onScanAndConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Asignado", style = MaterialTheme.typography.labelMedium)
            Text(device.serialNumber, style = MaterialTheme.typography.titleMedium)
            Text("Modelo: ${device.model}", style = MaterialTheme.typography.bodyMedium)
            Text("Anunciado: ${device.advertisedName}", style = MaterialTheme.typography.bodyMedium)
            Text("Batería: ${device.batteryLevel}%", style = MaterialTheme.typography.bodyMedium)
        }
    }

    Text(
        text = "Estado: ${connectionState.toLabel()}",
        style = MaterialTheme.typography.titleSmall
    )

    val isBusy = connectionState is BleConnectionState.Scanning ||
        connectionState is BleConnectionState.Connecting ||
        connectionState is BleConnectionState.ConfirmingIdentity
    val isConnected = connectionState is BleConnectionState.Connected

    if (isBusy) {
        CenteredProgress()
    }

    if (isConnected) {
        Button(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) {
            Text("Desconectar")
        }
    } else {
        Button(
            onClick = onScanAndConnect,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Escanear y conectar")
        }
    }

    if (permissionDenied) {
        Text(
            text = "Se necesitan permisos de Bluetooth para escanear.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }

    if (isConnected) {
        TelemetryContent(telemetry = telemetry, framesReceived = framesReceived)
    }
}

@Composable
private fun TelemetryContent(
    telemetry: MotionTelemetry?,
    framesReceived: Int
) {
    Spacer(modifier = Modifier.height(4.dp))
    Text("Telemetría en vivo", style = MaterialTheme.typography.titleSmall)

    if (telemetry == null) {
        Text("Esperando primeras tramas…", style = MaterialTheme.typography.bodyMedium)
        return
    }

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Frames: $framesReceived   Seq: ${telemetry.sequenceNumber}",
                style = MaterialTheme.typography.bodyMedium
            )
            QuaternionRow("upper-middle", telemetry.upperMiddleRotation)
            QuaternionRow("middle-lower", telemetry.middleLowerRotation)
            QuaternionRow("upper-lower", telemetry.upperLowerRotation)
            Text(
                text = "LED: ${telemetry.ledColor}   Buzzer: ${telemetry.buzzerActive}   " +
                    "Vibración: ${telemetry.vibrationActive}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun QuaternionRow(label: String, q: Quaternion) {
    Text(
        text = "$label  w=${q.w.f()} x=${q.x.f()} y=${q.y.f()} z=${q.z.f()}",
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace
    )
}

@Composable
private fun CenteredProgress() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
    }
}

private fun Float.f(): String = String.format(Locale.US, "%+.3f", this)

private fun BleConnectionState.toLabel(): String = when (this) {
    BleConnectionState.Idle -> "Listo"
    BleConnectionState.Scanning -> "Buscando dispositivo…"
    BleConnectionState.Connecting -> "Conectando…"
    BleConnectionState.ConfirmingIdentity -> "Confirmando identidad…"
    BleConnectionState.Connected -> "Conectado"
    BleConnectionState.Disconnected -> "Desconectado"
    is BleConnectionState.Failed -> "Error: ${reason.toLabel()}"
}

private fun BleConnectionState.Failed.Reason.toLabel(): String = when (this) {
    BleConnectionState.Failed.Reason.DeviceNotFound -> "no se encontró el dispositivo"
    BleConnectionState.Failed.Reason.ConnectionLost -> "se perdió la conexión"
    BleConnectionState.Failed.Reason.IdentityMismatch -> "el serial no coincide"
    BleConnectionState.Failed.Reason.MissingService -> "servicio BLE no encontrado"
    BleConnectionState.Failed.Reason.BluetoothUnavailable -> "Bluetooth no disponible"
    BleConnectionState.Failed.Reason.PermissionDenied -> "permiso denegado"
    BleConnectionState.Failed.Reason.Unknown -> "error desconocido"
}
