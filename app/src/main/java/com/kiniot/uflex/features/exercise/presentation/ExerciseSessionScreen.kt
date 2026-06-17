package com.kiniot.uflex.features.exercise.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiniot.uflex.R
import com.kiniot.uflex.core.designsystem.theme.ExtendedTheme

@Composable
fun ExerciseSessionScreen(
    paddingValues: PaddingValues,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseSessionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        // Status badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            LiveStatusBadge(postureGood = state.postureGood)
        }

        // Body illustration + overlay card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(268.dp),
        ) {
            BodyIllustration(
                postureGood = state.postureGood,
                modifier = Modifier.fillMaxSize(),
            )
            ExerciseInfoCard(
                postureGood = state.postureGood,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .fillMaxWidth(),
            )
        }

        // Rep counter
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.exercise_session_reps_label),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = androidx.compose.ui.unit.TextUnit(
                        2f,
                        androidx.compose.ui.unit.TextUnitType.Sp,
                    ),
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${state.reps}",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "/${state.maxReps}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { state.reps.toFloat() / state.maxReps },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
            )
        }

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = viewModel::togglePause,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(
                    imageVector = if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (state.isPaused)
                        stringResource(R.string.exercise_session_resume)
                    else
                        stringResource(R.string.exercise_session_pause),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.exercise_session_finish),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

// ── Body illustration ─────────────────────────────────────────────────────────

@Composable
private fun BodyIllustration(postureGood: Boolean, modifier: Modifier = Modifier) {
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val errorColor = MaterialTheme.colorScheme.error
    val bgColor = MaterialTheme.colorScheme.surfaceContainerHighest

    val glowColor by animateColorAsState(
        targetValue = if (postureGood) secondaryColor else errorColor,
        animationSpec = tween(400),
        label = "glow_color",
    )
    val iconTint by animateColorAsState(
        targetValue = if (postureGood)
            secondaryColor.copy(alpha = 0.18f)
        else
            errorColor.copy(alpha = 0.15f),
        animationSpec = tween(400),
        label = "icon_tint",
    )

    Box(modifier = modifier.background(bgColor)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.35f), glowColor.copy(alpha = 0f)),
                    center = Offset(size.width * 0.5f, size.height * 0.45f),
                    radius = size.minDimension * 0.5f,
                ),
            )
        }
        Icon(
            imageVector = Icons.Default.AccessibilityNew,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.Center),
        )
        // Glowing joint dot (elbow area)
        Box(
            modifier = Modifier
                .size(22.dp)
                .align(Alignment.Center)
                .padding(bottom = 0.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor.copy(alpha = 0.9f), glowColor.copy(alpha = 0f)),
                    ),
                )
            }
        }
    }
}

// ── Exercise info card (overlay) ──────────────────────────────────────────────

@Composable
private fun ExerciseInfoCard(postureGood: Boolean, modifier: Modifier = Modifier) {
    val successColors = ExtendedTheme.colors.success

    val iconBg by animateColorAsState(
        targetValue = if (postureGood) successColors.colorContainer
        else MaterialTheme.colorScheme.errorContainer,
        animationSpec = tween(300),
        label = "icon_bg",
    )
    val iconTint by animateColorAsState(
        targetValue = if (postureGood) successColors.onColorContainer
        else MaterialTheme.colorScheme.onErrorContainer,
        animationSpec = tween(300),
        label = "icon_tint",
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (postureGood) Icons.Default.FitnessCenter else Icons.Default.Warning,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.exercise_name_elbow_flexion),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (postureGood)
                        stringResource(R.string.exercise_session_posture_good)
                    else
                        stringResource(R.string.exercise_session_posture_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Live status badge ─────────────────────────────────────────────────────────

@Composable
private fun LiveStatusBadge(postureGood: Boolean) {
    val successColors = ExtendedTheme.colors.success

    val badgeBg by animateColorAsState(
        targetValue = if (postureGood) successColors.colorContainer
        else MaterialTheme.colorScheme.errorContainer,
        animationSpec = tween(300),
        label = "badge_bg",
    )
    val badgeFg by animateColorAsState(
        targetValue = if (postureGood) successColors.onColorContainer
        else MaterialTheme.colorScheme.onErrorContainer,
        animationSpec = tween(300),
        label = "badge_fg",
    )
    val dotColor by animateColorAsState(
        targetValue = if (postureGood) successColors.color
        else MaterialTheme.colorScheme.error,
        animationSpec = tween(300),
        label = "dot_color",
    )

    Surface(
        shape = RoundedCornerShape(50),
        color = badgeBg,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Text(
                text = if (postureGood)
                    stringResource(R.string.exercise_session_status_good)
                else
                    stringResource(R.string.exercise_session_status_error),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = badgeFg,
            )
        }
    }
}
