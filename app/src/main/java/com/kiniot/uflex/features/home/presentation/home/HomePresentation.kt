package com.kiniot.uflex.features.home.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kiniot.uflex.R
import com.kiniot.uflex.core.ui.asString
import com.kiniot.uflex.features.plan.domain.model.PlanExercise
import com.kiniot.uflex.features.plan.presentation.exercises.toUiText

/** Time-of-day greeting, e.g. "Buenos días, Salim". Falls back to a name-less variant. */
@Composable
fun greetingText(greeting: Greeting, firstName: String?): String {
    if (firstName == null) return stringResource(R.string.home_greeting_generic)
    val resId = when (greeting) {
        Greeting.Morning -> R.string.home_greeting_morning
        Greeting.Afternoon -> R.string.home_greeting_afternoon
        Greeting.Evening -> R.string.home_greeting_evening
    }
    return stringResource(resId, firstName)
}

/**
 * One of today's exercises as an informational row (tap opens its detail). Visually mirrors the
 * plan feature's exercise row; re-implemented locally to avoid widening that feature's API.
 */
@Composable
fun TodayExerciseRow(exercise: PlanExercise, onClick: () -> Unit) {
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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
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
fun Pill(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
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
