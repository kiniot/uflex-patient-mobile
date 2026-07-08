package com.kiniot.uflex.features.therapy.domain.model

/**
 * Rendezvous info for reaching the patient's edge on the LAN and authenticating its live SSE
 * progress stream. [localEdgeUrl] is null when the edge has not reported its LAN URL yet.
 */
data class EdgeConnection(
    val localEdgeUrl: String?,
    val pairingToken: String,
    val expiresAt: String? = null
)
