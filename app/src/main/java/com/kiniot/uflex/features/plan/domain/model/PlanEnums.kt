package com.kiniot.uflex.features.plan.domain.model

/** Lifecycle status of a treatment plan. Mirrors the backend TreatmentPlanStatus enum. */
enum class PlanStatus {
    Scheduled,
    Active,
    Completed,
    Canceled,
    Unknown
}

/** Body part targeted by an exercise. Mirrors the backend BodyPart enum. */
enum class BodyPart {
    Elbow,
    Wrist,
    Unknown
}

/** Movement an exercise trains. Mirrors the backend MovementType enum. */
enum class MovementType {
    Pronation,
    Supination,
    Flexion,
    Extension,
    Unknown
}
