package com.kiniot.uflex.features.home.presentation.home

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import com.kiniot.uflex.R
import com.kiniot.uflex.core.designsystem.theme.ExtendedTheme

// ── Mock data (prototype only) ────────────────────────────────────────────────

private data class MockExercise(
    val nameRes: Int,
    val series: Int,
    val reps: Int,
    val icon: ImageVector,
    val isPrimary: Boolean,
)

private val mockExercises = listOf(
    MockExercise(R.string.home_exercise_elbow_flexion, 3, 15, Icons.Default.FitnessCenter, true),
    MockExercise(R.string.home_exercise_shoulder_rotation, 2, 12, Icons.Default.AccessibilityNew, false),
    MockExercise(R.string.home_exercise_wrist_extension, 3, 10, Icons.Default.PanTool, false),
)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding() + 24.dp,
            bottom = paddingValues.calculateBottomPadding() + 24.dp,
            start = 16.dp,
            end = 16.dp,
        ),
    ) {
        item { GreetingSection() }
        item { Spacer(Modifier.height(20.dp)) }
        item { WeeklyProgressCard() }
        item { Spacer(Modifier.height(28.dp)) }
        item { ExercisesSectionHeader(pendingCount = mockExercises.size) }
        item { Spacer(Modifier.height(12.dp)) }
        items(mockExercises) { exercise ->
            ExerciseItem(exercise)
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ── Greeting ──────────────────────────────────────────────────────────────────

@Composable
private fun GreetingSection() {
    Column {
        Text(
            text = stringResource(R.string.home_greeting_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.home_greeting_title, "Salim"),
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

// ── Weekly progress card ──────────────────────────────────────────────────────

@Composable
private fun WeeklyProgressCard() {
    val successColors = ExtendedTheme.colors.success

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.home_weekly_progress_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_weekly_progress_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = successColors.colorContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = successColors.onColorContainer,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(R.string.home_weekly_progress_badge),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = successColors.onColorContainer,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressRing(progress = 0.75f)
            }
        }
    }
}

@Composable
private fun CircularProgressRing(progress: Float) {
    val progressColor = MaterialTheme.colorScheme.secondary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(140.dp),
    ) {
        Canvas(modifier = Modifier.size(140.dp)) {
            val strokeWidth = 14.dp.toPx()
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Text(
            text = stringResource(R.string.home_progress_percentage, (progress * 100).toInt()),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Exercises section ─────────────────────────────────────────────────────────

@Composable
private fun ExercisesSectionHeader(pendingCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_exercises_today_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.home_exercises_pending, pendingCount),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ExerciseItem(exercise: MockExercise) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = exercise.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(exercise.nameRes),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_exercise_detail, exercise.series, exercise.reps),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (exercise.isPrimary) {
                Button(onClick = {}) {
                    Text(stringResource(R.string.home_exercise_start))
                }
            } else {
                OutlinedButton(onClick = {}) {
                    Text(stringResource(R.string.home_exercise_start))
                }
            }
        }
    }
}
