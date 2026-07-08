package com.kiniot.uflex.features.plan.navigation

import kotlinx.serialization.Serializable

/** Detail screen for a single exercise (video + description), mounted in the MainShell overlay. */
@Serializable
data class ExerciseDetailRoute(val exerciseId: String)
