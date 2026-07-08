package com.kiniot.uflex.features.plan.presentation.exercises

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiniot.uflex.R
import com.kiniot.uflex.core.designsystem.components.ColoredPill
import com.kiniot.uflex.core.designsystem.components.HaloIcon
import com.kiniot.uflex.core.designsystem.components.Pill
import com.kiniot.uflex.core.designsystem.components.ProgressRing
import com.kiniot.uflex.core.ui.asString
import com.kiniot.uflex.features.plan.domain.model.PlanExercise
import com.kiniot.uflex.features.plan.domain.model.PlanRoutine
import com.kiniot.uflex.features.plan.domain.model.TreatmentPlan
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@Composable
fun ExercisesScreen(
    paddingValues: PaddingValues,
    onExerciseClick: (String) -> Unit,
    onStartSession: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExercisesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize().padding(paddingValues)) {
        when {
            uiState.isLoading -> ExercisesLoadingSkeleton()
            uiState.errorMessage != null -> ErrorState(onRetry = viewModel::onRetry)
            else -> ExercisesContent(
                activePlan = uiState.activePlan,
                scheduledPlans = uiState.scheduledPlans,
                onExerciseClick = onExerciseClick,
                onStartSession = onStartSession
            )
        }
    }
}

@Composable
private fun ExercisesContent(
    activePlan: TreatmentPlan?,
    scheduledPlans: List<TreatmentPlan>,
    onExerciseClick: (String) -> Unit,
    onStartSession: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (activePlan != null) {
            item(key = "hero") { ActivePlanHero(activePlan, onStartSession = { onStartSession(activePlan.id) }) }
            activePlan.routines.forEach { routine ->
                item(key = "routine-${routine.id}") { RoutineHeader(routine) }
                items(routine.exercises, key = { "ex-${routine.id}-${it.order}-${it.exerciseId}" }) { exercise ->
                    ExerciseRow(exercise = exercise, onClick = { onExerciseClick(exercise.exerciseId) })
                }
            }
        } else {
            item(key = "empty") { EmptyActivePlan() }
        }

        if (scheduledPlans.isNotEmpty()) {
            item(key = "scheduled-title") {
                Text(
                    text = stringResource(R.string.plan_scheduled_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(scheduledPlans, key = { "sched-${it.id}" }) { plan -> ScheduledPlanCard(plan) }
        }
    }
}

@Composable
private fun ActivePlanHero(plan: TreatmentPlan, onStartSession: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.plan_active_label).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                planProgressRing(plan)?.let { (fraction, percent) ->
                    ProgressRing(
                        fraction = fraction,
                        percent = percent,
                        diameter = 56.dp,
                        strokeWidth = 6.dp,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                        progressColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        textColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Pill(stringResource(R.string.plan_routine_count, plan.routines.size))
                Pill(stringResource(R.string.plan_exercise_count, plan.routines.sumOf { it.exercises.size }))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusPill(plan)
                val from = formatShortDate(plan.period?.startsAt)
                val to = formatShortDate(plan.period?.endsAt)
                if (from != null || to != null) {
                    Text(
                        text = listOfNotNull(from, to).joinToString(" – "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
            Button(
                onClick = onStartSession,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.therapy_start_today))
            }
        }
    }
}

/**
 * Elapsed/total-days fraction + percent for the hero's decorative progress ring, or `null` when
 * either date is missing or the period's range isn't positive (in which case the hero falls back
 * to just the status pill + date range, exactly as before this ring existed).
 */
private fun planProgressRing(plan: TreatmentPlan): Pair<Float, Int>? {
    val startsAt = plan.period?.startsAt ?: return null
    val endsAt = plan.period.endsAt ?: return null
    if (!endsAt.isAfter(startsAt)) return null
    val totalDays = ChronoUnit.DAYS.between(startsAt, endsAt)
    if (totalDays <= 0) return null
    val elapsedDays = ChronoUnit.DAYS.between(startsAt, LocalDate.now())
    val fraction = (elapsedDays.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)
    return fraction to (fraction * 100).roundToInt()
}

@Composable
private fun StatusPill(plan: TreatmentPlan) {
    val (container, content) = plan.status.statusColors()
    ColoredPill(text = plan.status.toUiText().asString(LocalContext.current), container = container, content = content)
}

@Composable
private fun RoutineHeader(routine: PlanRoutine) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = routine.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val schedule = formatSchedule(routine.schedule?.dayOfWeek, routine.schedule?.scheduledTime)
            if (schedule != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = schedule,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.plan_exercise_count, routine.exercises.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ExerciseRow(exercise: PlanExercise, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HaloIcon(icon = Icons.Default.FitnessCenter, size = 44.dp, iconSize = 22.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val context = LocalContext.current
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(exercise.bodyPart.toUiText().asString(context))
                    Pill(exercise.movementType.toUiText().asString(context))
                }
                val dosage = exercise.dosageText()
                if (dosage != null) {
                    Text(
                        text = dosage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlanExercise.dosageText(): String? {
    val parts = buildList {
        repetitions?.let { add(stringResource(R.string.plan_dosage_reps, it)) }
        rangeOfMotionDegrees?.let { add(stringResource(R.string.plan_dosage_rom, it)) }
        durationSeconds?.let { add(stringResource(R.string.plan_dosage_seconds, it)) }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString("  ·  ")
}

@Composable
private fun ScheduledPlanCard(plan: TreatmentPlan) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HaloIcon(
                icon = Icons.Default.CalendarMonth,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                size = 44.dp,
                iconSize = 22.dp
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = plan.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val start = formatShortDate(plan.period?.startsAt)
                val detail = buildList {
                    if (start != null) add(stringResource(R.string.plan_scheduled_starts, start))
                    add(stringResource(R.string.plan_routine_count, plan.routines.size))
                }.joinToString("  ·  ")
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyActivePlan() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HaloIcon(icon = Icons.Default.FitnessCenter, size = 64.dp, iconSize = 30.dp)
            Text(
                text = stringResource(R.string.plan_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.plan_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExercisesLoadingSkeleton() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 850), RepeatMode.Reverse),
        label = "shimmerAlpha"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SkeletonBlock(Modifier.fillMaxWidth().height(150.dp), alpha, RoundedCornerShape(28.dp))
        SkeletonBlock(Modifier.fillMaxWidth(0.4f).height(20.dp), alpha)
        repeat(3) {
            SkeletonBlock(Modifier.fillMaxWidth().height(72.dp), alpha, RoundedCornerShape(20.dp))
        }
    }
}

@Composable
private fun SkeletonBlock(
    modifier: Modifier,
    alpha: Float,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha), shape))
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        HaloIcon(
            icon = Icons.Default.ErrorOutline,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
        Text(
            text = stringResource(R.string.plan_error_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Button(onClick = onRetry, shape = RoundedCornerShape(18.dp)) {
            Text(stringResource(R.string.plan_retry))
        }
    }
}
