package com.kiniot.uflex.features.device.data.ble

import android.content.Context
import com.kiniot.uflex.core.result.AppError
import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.device.data.mapper.toMotionTelemetry
import com.kiniot.uflex.features.device.domain.model.BleConnectionState
import com.kiniot.uflex.features.device.domain.model.MotionTelemetry
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeoutOrNull
import no.nordicsemi.android.kotlin.ble.client.main.callback.ClientBleGatt
import no.nordicsemi.android.kotlin.ble.client.main.service.ClientBleGattCharacteristic
import no.nordicsemi.android.kotlin.ble.core.ServerDevice
import no.nordicsemi.android.kotlin.ble.core.data.GattConnectionState
import no.nordicsemi.android.kotlin.ble.core.scanner.BleScanResult
import no.nordicsemi.android.kotlin.ble.scanner.BleScanner

/**
 * Nordic Kotlin-BLE-backed implementation of [DeviceBleDataSource]. This is the only class in
 * the feature coupled to the concrete BLE library; everything above it works in app/domain
 * types. It is a singleton so the GATT link survives screen recreation.
 *
 * Failure detail is surfaced through [connectionState] ([BleConnectionState.Failed.Reason]);
 * [connect] returns a coarse [AppResult] for the caller.
 */
@Singleton
class DeviceBleDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceBleDataSource {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Idle)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private var client: ClientBleGatt? = null
    private var telemetryCharacteristic: ClientBleGattCharacteristic? = null
    private var connectionMonitorJob: Job? = null
    private var intentionalDisconnect = false

    override suspend fun connect(advertisedName: String, expectedSerial: String): AppResult<Unit> {
        return try {
            disconnect()
            intentionalDisconnect = false

            _connectionState.value = BleConnectionState.Scanning
            val device = scanForDevice(advertisedName)
                ?: return fail(BleConnectionState.Failed.Reason.DeviceNotFound)

            _connectionState.value = BleConnectionState.Connecting
            val gatt = ClientBleGatt.connect(context, device, scope)
            client = gatt

            // A telemetry frame is 53 bytes, but the default ATT MTU (23) only carries 20 bytes
            // per notification, so frames would arrive truncated and be dropped by the parser.
            // Request a larger MTU before subscribing. A failed negotiation is non-fatal (some
            // stacks still raise it), so we don't abort the connection over it.
            runCatching { gatt.requestMtu(REQUESTED_MTU) }

            val services = gatt.discoverServices()
            val service = services.findService(UflexGattProfile.SERVICE_UUID)
                ?: return fail(BleConnectionState.Failed.Reason.MissingService)

            _connectionState.value = BleConnectionState.ConfirmingIdentity
            val serialCharacteristic =
                service.findCharacteristic(UflexGattProfile.SERIAL_CHARACTERISTIC_UUID)
                    ?: return fail(BleConnectionState.Failed.Reason.MissingService)
            val reportedSerial = serialCharacteristic.read().value.decodeToString().trim()
            if (reportedSerial != expectedSerial) {
                return fail(BleConnectionState.Failed.Reason.IdentityMismatch)
            }

            telemetryCharacteristic =
                service.findCharacteristic(UflexGattProfile.TELEMETRY_CHARACTERISTIC_UUID)
                    ?: return fail(BleConnectionState.Failed.Reason.MissingService)

            monitorConnection(gatt)
            _connectionState.value = BleConnectionState.Connected
            AppResult.Success(Unit)
        } catch (security: SecurityException) {
            failResult(BleConnectionState.Failed.Reason.PermissionDenied, security)
        } catch (exception: Exception) {
            failResult(BleConnectionState.Failed.Reason.Unknown, exception)
        }
    }

    override fun observeTelemetry(): Flow<MotionTelemetry> {
        return flow {
            val characteristic = telemetryCharacteristic ?: return@flow
            emitAll(
                characteristic.getNotifications()
                    .mapNotNull { it.value.toMotionTelemetry() }
            )
        }
    }

    override suspend fun disconnect() {
        intentionalDisconnect = true
        connectionMonitorJob?.cancel()
        connectionMonitorJob = null
        telemetryCharacteristic = null
        client?.disconnect()
        client = null
        _connectionState.value = BleConnectionState.Disconnected
    }

    /**
     * Watches the live GATT connection so an unexpected drop (out of range, device powered off)
     * surfaces as [BleConnectionState.Failed] with [BleConnectionState.Failed.Reason.ConnectionLost],
     * rather than the UI being stuck on "Connected". Intentional disconnects cancel this monitor
     * first, so they are reported as [BleConnectionState.Disconnected] instead.
     */
    private fun monitorConnection(gatt: ClientBleGatt) {
        connectionMonitorJob?.cancel()
        connectionMonitorJob = gatt.connectionState
            .onEach { state ->
                if (state == GattConnectionState.STATE_DISCONNECTED && !intentionalDisconnect) {
                    telemetryCharacteristic = null
                    client = null
                    _connectionState.value =
                        BleConnectionState.Failed(BleConnectionState.Failed.Reason.ConnectionLost)
                }
            }
            .launchIn(scope)
    }

    private suspend fun scanForDevice(advertisedName: String): ServerDevice? {
        return withTimeoutOrNull(SCAN_TIMEOUT_MILLIS) {
            BleScanner(context).scan()
                .first { it.matchesUflexKit(advertisedName) }
                .device
        }
    }

    /**
     * Matches a uFlex kit primarily by the advertised service UUID, which is the reliable
     * discovery filter: the device name often does not fit alongside the 128-bit service UUID in
     * the 31-byte advertisement, so name matching alone is fragile. The name is kept as a
     * fallback, and the kit serial is still verified after connecting (see device-identity-contract).
     */
    private fun BleScanResult.matchesUflexKit(advertisedName: String): Boolean {
        val record = data?.scanRecord
        val advertisesService =
            record?.serviceUuids?.any { it.uuid == UflexGattProfile.SERVICE_UUID } == true
        val nameMatches = device.name == advertisedName || record?.deviceName == advertisedName
        return advertisesService || nameMatches
    }

    private fun fail(reason: BleConnectionState.Failed.Reason): AppResult<Unit> {
        _connectionState.value = BleConnectionState.Failed(reason)
        return AppResult.Error(AppError.Unknown())
    }

    private fun failResult(
        reason: BleConnectionState.Failed.Reason,
        cause: Throwable
    ): AppResult<Unit> {
        _connectionState.value = BleConnectionState.Failed(reason)
        return AppResult.Error(AppError.Unknown(cause))
    }

    private companion object {
        const val SCAN_TIMEOUT_MILLIS = 15_000L
        // Max BLE ATT MTU; the peripheral caps it (firmware prefers 256), comfortably above the
        // 53-byte telemetry frame.
        const val REQUESTED_MTU = 517
    }
}
