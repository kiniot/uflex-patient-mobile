package com.kiniot.uflex.features.therapy.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Wire shape of `GET /patients/me/edge-connection` (camelCase, matching the backend record).
 * `localEdgeUrl` is null until the edge has reported its LAN URL; the client then falls back to
 * its build-time default.
 */
@Serializable
data class EdgeConnectionResponseDto(
    val localEdgeUrl: String? = null,
    val pairingToken: String,
    val expiresAt: String? = null
)
