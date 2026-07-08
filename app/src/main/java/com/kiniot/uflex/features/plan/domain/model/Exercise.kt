package com.kiniot.uflex.features.plan.domain.model

/** Catalog detail of an exercise, shown on the exercise detail screen (with its video). */
data class Exercise(
    val id: String,
    val name: String,
    val description: String?,
    val bodyPart: BodyPart,
    val movementType: MovementType,
    val videoUrl: String?
)
