package com.kiniot.uflex.features.therapy.navigation

import kotlinx.serialization.Serializable

/** Session-preparation flow for a treatment plan, mounted in the MainShell overlay. */
@Serializable
data class SessionPreparationRoute(val treatmentPlanId: String)
