package com.kiniot.uflex.features.plan.presentation.exercises

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kiniot.uflex.R
import com.kiniot.uflex.core.designsystem.theme.ExtendedTheme
import com.kiniot.uflex.core.ui.UiText
import com.kiniot.uflex.features.plan.domain.model.BodyPart
import com.kiniot.uflex.features.plan.domain.model.MovementType
import com.kiniot.uflex.features.plan.domain.model.PlanStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

fun PlanStatus.toUiText(): UiText = when (this) {
    PlanStatus.Scheduled -> UiText.Resource(R.string.plan_status_scheduled)
    PlanStatus.Active -> UiText.Resource(R.string.plan_status_active)
    PlanStatus.Completed -> UiText.Resource(R.string.plan_status_completed)
    PlanStatus.Canceled -> UiText.Resource(R.string.plan_status_canceled)
    PlanStatus.Unknown -> UiText.Resource(R.string.plan_status_unknown)
}

/** Container + content colors for the status chip. */
@Composable
fun PlanStatus.statusColors(): Pair<Color, Color> {
    val extended = ExtendedTheme.colors
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        PlanStatus.Active -> extended.success.colorContainer to extended.success.onColorContainer
        PlanStatus.Scheduled -> extended.info.colorContainer to extended.info.onColorContainer
        PlanStatus.Completed -> scheme.secondaryContainer to scheme.onSecondaryContainer
        PlanStatus.Canceled -> scheme.errorContainer to scheme.onErrorContainer
        PlanStatus.Unknown -> scheme.surfaceVariant to scheme.onSurfaceVariant
    }
}

fun BodyPart.toUiText(): UiText = when (this) {
    BodyPart.Elbow -> UiText.Resource(R.string.bodypart_elbow)
    BodyPart.Wrist -> UiText.Resource(R.string.bodypart_wrist)
    BodyPart.Unknown -> UiText.Resource(R.string.bodypart_unknown)
}

fun MovementType.toUiText(): UiText = when (this) {
    MovementType.Pronation -> UiText.Resource(R.string.movement_pronation)
    MovementType.Supination -> UiText.Resource(R.string.movement_supination)
    MovementType.Flexion -> UiText.Resource(R.string.movement_flexion)
    MovementType.Extension -> UiText.Resource(R.string.movement_extension)
    MovementType.Unknown -> UiText.Resource(R.string.movement_unknown)
}

private val shortDateFormatter: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

fun formatShortDate(date: LocalDate?): String? = date?.format(shortDateFormatter)

/** "Wed · 08:00" style label from a routine schedule, or null when nothing to show. */
fun formatSchedule(dayOfWeek: String?, scheduledTime: String?): String? {
    val day = dayOfWeek?.let {
        runCatching {
            DayOfWeek.valueOf(it.uppercase(Locale.US))
                .getDisplayName(TextStyle.SHORT, Locale.getDefault())
        }.getOrNull()
    }
    return listOfNotNull(day, scheduledTime).takeIf { it.isNotEmpty() }?.joinToString(" · ")
}
