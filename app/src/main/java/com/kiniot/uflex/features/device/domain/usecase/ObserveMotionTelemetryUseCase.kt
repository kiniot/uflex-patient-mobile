package com.kiniot.uflex.features.device.domain.usecase

import com.kiniot.uflex.features.device.domain.model.MotionTelemetry
import com.kiniot.uflex.features.device.domain.repository.DeviceConnectionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams decoded telemetry frames from the connected kit. */
class ObserveMotionTelemetryUseCase @Inject constructor(
    private val connectionRepository: DeviceConnectionRepository
) {
    operator fun invoke(): Flow<MotionTelemetry> {
        return connectionRepository.observeTelemetry()
    }
}
