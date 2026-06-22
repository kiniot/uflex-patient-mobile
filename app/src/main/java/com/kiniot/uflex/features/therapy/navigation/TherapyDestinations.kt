package com.kiniot.uflex.features.therapy.navigation

import kotlinx.serialization.Serializable

/** Session-preparation flow for a treatment plan, mounted in the MainShell overlay. */
@Serializable
data class SessionPreparationRoute(val treatmentPlanId: String)

/** Session-execution screen (Phase 3) for an in-progress session, mounted in the MainShell overlay. */
@Serializable
data class SessionExecutionRoute(val sessionId: String)
