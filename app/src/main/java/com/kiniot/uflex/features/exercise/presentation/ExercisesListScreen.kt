package com.kiniot.uflex.features.exercise.presentation

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiniot.uflex.R

private data class MockExerciseItem(
    val nameRes: Int,
    val series: Int,
    val reps: Int,
    val icon: ImageVector,
)

private val mockExercises = listOf(
    MockExerciseItem(R.string.exercise_name_elbow_flexion, 3, 20, Icons.Default.FitnessCenter),
    MockExerciseItem(R.string.exercise_name_shoulder_rotation, 2, 15, Icons.Default.AccessibilityNew),
    MockExerciseItem(R.string.exercise_name_wrist_extension, 3, 12, Icons.Default.PanTool),
)

@Composable
fun ExercisesListScreen(
    paddingValues: PaddingValues,
    onStartExercise: () -> Unit,
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
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.exercise_list_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.exercise_list_count, mockExercises.size),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.exercise_list_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
        }
        itemsIndexed(mockExercises) { index, exercise ->
            ExerciseListItem(
                exercise = exercise,
                isPrimary = index == 0,
                onStart = onStartExercise,
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ExerciseListItem(
    exercise: MockExerciseItem,
    isPrimary: Boolean,
    onStart: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    text = stringResource(R.string.exercise_detail, exercise.series, exercise.reps),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isPrimary) {
                Button(onClick = onStart) { Text(stringResource(R.string.exercise_start)) }
            } else {
                OutlinedButton(onClick = onStart) { Text(stringResource(R.string.exercise_start)) }
            }
        }
    }
}
