package com.kiniot.uflex.features.home.presentation.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiniot.uflex.R
import com.kiniot.uflex.core.designsystem.theme.ExtendedTheme
import com.kiniot.uflex.core.ui.asString
import com.kiniot.uflex.features.plan.domain.model.TreatmentPlan
import com.kiniot.uflex.features.plan.presentation.exercises.statusColors
import com.kiniot.uflex.features.plan.presentation.exercises.toUiText
import com.kiniot.uflex.features.therapy.domain.model.ScheduleResolution

@Composable
fun HomeScreen(
    paddingValues: PaddingValues,
    onNavigateToExerciseDetail: (String) -> Unit,
    onNavigateToSessionPreparation: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize().padding(paddingValues)) {
        when {
            uiState.isLoading -> HomeLoading()
            uiState.errorMessage != null -> HomeErrorState(onRetry = viewModel::onRetry)
            else -> HomeContent(
                uiState = uiState,
                onNavigateToExerciseDetail = onNavigateToExerciseDetail,
                onNavigateToSessionPreparation = onNavigateToSessionPreparation
            )
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onNavigateToExerciseDetail: (String) -> Unit,
    onNavigateToSessionPreparation: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "greeting") { GreetingHeader(uiState.greeting, uiState.firstName) }

        item(key = "hero") {
            val plan = uiState.activePlan
            if (plan != null) {
                val showToday = uiState.todayResolution == ScheduleResolution.Found
                PlanProgressHero(
                    plan = plan,
                    progress = uiState.planProgress,
                    totalSeries = uiState.todayTotalSeries.takeIf { showToday },
                    estMinutes = uiState.todayEstMinutes.takeIf { showToday }
                )
            } else {
                NoPlanCard()
            }
        }

        when (uiState.todayResolution) {
            ScheduleResolution.Found -> if (uiState.todaysExercises.isNotEmpty()) {
                item(key = "today-title") { TodaySectionHeader(count = uiState.todaysExercises.size) }
                items(uiState.todaysExercises, key = { "today-${it.order}-${it.exerciseId}" }) { exercise ->
                    TodayExerciseRow(exercise = exercise, onClick = { onNavigateToExerciseDetail(exercise.exerciseId) })
                }
                uiState.activePlan?.let { plan ->
                    item(key = "cta") { StartTodayButton(onClick = { onNavigateToSessionPreparation(plan.id) }) }
                }
            }

            ScheduleResolution.NoRoutineForDay -> item(key = "rest") { RestDayCard() }
            ScheduleResolution.Unknown -> item(key = "unknown") { ScheduleUnknownCard() }
            ScheduleResolution.NoActivePlan -> Unit // the hero already shows the no-plan card
        }
    }
}

@Composable
private fun GreetingHeader(greeting: Greeting, firstName: String?) {
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.home_greeting_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = greetingText(greeting, firstName),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun PlanProgressHero(
    plan: TreatmentPlan,
    progress: PlanProgress?,
    totalSeries: Int?,
    estMinutes: Int?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.home_progress_label).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (progress != null) {
                    ProgressRing(fraction = progress.fraction, percent = progress.percent, diameter = 108.dp)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    StatusPill(plan)
                    Text(
                        text = if (progress != null) {
                            stringResource(R.string.home_progress_day_of, progress.dayIndex, progress.totalDays)
                        } else {
                            stringResource(R.string.home_progress_no_dates)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                    if (totalSeries != null && estMinutes != null) {
                        Text(
                            text = stringResource(R.string.home_today_stats, totalSeries, estMinutes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(plan: TreatmentPlan) {
    val (container, content) = plan.status.statusColors()
    Surface(shape = RoundedCornerShape(50), color = container) {
        Text(
            text = plan.status.toUiText().asString(LocalContext.current),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = content,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun TodaySectionHeader(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.home_today_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = pluralStringResource(R.plurals.home_today_pending, count, count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StartTodayButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(stringResource(R.string.therapy_start_today))
    }
}

@Composable
private fun NoPlanCard() {
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
            Box(
                modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(30.dp)
                )
            }
            Text(
                text = stringResource(R.string.plan_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.plan_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RestDayCard() {
    val success = ExtendedTheme.colors.success
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = success.colorContainer
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(success.color.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SelfImprovement,
                    contentDescription = null,
                    tint = success.onColorContainer,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.home_rest_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = success.onColorContainer
                )
                Text(
                    text = stringResource(R.string.home_rest_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = success.onColorContainer.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun ScheduleUnknownCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Text(
            text = stringResource(R.string.home_schedule_unknown),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp)
        )
    }
}

@Composable
private fun HomeLoading() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1200), RepeatMode.Restart),
        label = "homeShimmerOffset"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "loading-greeting") {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SkeletonBlock(Modifier.fillMaxWidth(0.36f).height(14.dp), shimmerOffset)
                SkeletonBlock(Modifier.fillMaxWidth(0.68f).height(32.dp), shimmerOffset)
            }
        }

        item(key = "loading-hero") { PlanProgressHeroSkeleton(shimmerOffset) }

        item(key = "loading-today-title") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SkeletonBlock(Modifier.width(128.dp).height(26.dp), shimmerOffset)
                SkeletonBlock(Modifier.width(72.dp).height(18.dp), shimmerOffset)
            }
        }

        items(3, key = { "loading-exercise-$it" }) { TodayExerciseRowSkeleton(shimmerOffset) }

        item(key = "loading-cta") {
            SkeletonBlock(Modifier.fillMaxWidth().height(48.dp), shimmerOffset, RoundedCornerShape(16.dp))
        }
    }
}

@Composable
private fun SkeletonBlock(
    modifier: Modifier,
    shimmerOffset: Float,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    val base = MaterialTheme.colorScheme.surfaceContainerLow
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(shimmerOffset - 350f, 0f),
        end = Offset(shimmerOffset, 350f)
    )
    Box(modifier = modifier.background(brush, shape))
}

@Composable
private fun PlanProgressHeroSkeleton(shimmerOffset: Float) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SkeletonBlock(Modifier.width(104.dp).height(14.dp), shimmerOffset)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                SkeletonBlock(Modifier.size(108.dp), shimmerOffset, CircleShape)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SkeletonBlock(Modifier.fillMaxWidth(0.9f).height(24.dp), shimmerOffset)
                    SkeletonBlock(Modifier.width(88.dp).height(26.dp), shimmerOffset, RoundedCornerShape(50))
                    SkeletonBlock(Modifier.fillMaxWidth().height(16.dp), shimmerOffset)
                    SkeletonBlock(Modifier.fillMaxWidth(0.72f).height(14.dp), shimmerOffset)
                }
            }
        }
    }
}

@Composable
private fun TodayExerciseRowSkeleton(shimmerOffset: Float) {
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
            SkeletonBlock(Modifier.size(44.dp), shimmerOffset, CircleShape)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBlock(Modifier.fillMaxWidth(0.78f).height(20.dp), shimmerOffset)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SkeletonBlock(Modifier.width(72.dp).height(20.dp), shimmerOffset, RoundedCornerShape(8.dp))
                    SkeletonBlock(Modifier.width(88.dp).height(20.dp), shimmerOffset, RoundedCornerShape(8.dp))
                }
                SkeletonBlock(Modifier.fillMaxWidth(0.48f).height(14.dp), shimmerOffset)
            }
            SkeletonBlock(Modifier.size(24.dp), shimmerOffset, CircleShape)
        }
    }
}

@Composable
private fun HomeErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = stringResource(R.string.home_error_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Button(onClick = onRetry, shape = RoundedCornerShape(18.dp)) {
            Text(stringResource(R.string.plan_retry))
        }
    }
}
