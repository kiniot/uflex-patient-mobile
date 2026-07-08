package com.kiniot.uflex.features.history.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiniot.uflex.R
import com.kiniot.uflex.core.designsystem.theme.ExtendedTheme
import com.kiniot.uflex.core.ui.asString
import com.kiniot.uflex.features.history.domain.model.HistorySession
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val errorMessage = uiState.errorMessage

    Box(modifier = modifier.fillMaxSize().padding(paddingValues)) {
        when {
            uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            errorMessage != null -> HistoryMessage(
                title = errorMessage.asString(LocalContext.current),
                body = stringResource(R.string.history_load_error_body),
                modifier = Modifier.align(Alignment.Center).padding(24.dp)
            )

            else -> HistoryContent(uiState = uiState)
        }
    }
}

@Composable
private fun HistoryContent(uiState: HistoryUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header") { HistoryHeader() }

        if (uiState.sessions.isEmpty()) {
            item(key = "empty") {
                HistoryMessage(
                    title = stringResource(R.string.history_empty_title),
                    body = stringResource(R.string.history_empty_body)
                )
            }
        } else {
            item(key = "chart") {
                RomEvolutionCard(points = uiState.metrics.romPoints)
            }
            item(key = "metrics") {
                MetricsRow(metrics = uiState.metrics)
            }
            item(key = "recent-title") {
                Text(
                    text = stringResource(R.string.history_recent_sessions_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(uiState.sessions, key = { it.sessionId }) { session ->
                HistorySessionCard(session = session)
            }
        }
    }
}

@Composable
private fun HistoryHeader() {
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.history_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RomEvolutionCard(points: List<RomHistoryPoint>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.history_rom_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (points.isEmpty()) {
                HistoryMessage(
                    title = stringResource(R.string.history_no_rom_title),
                    body = stringResource(R.string.history_no_rom_body),
                    compact = true
                )
            } else {
                RomChart(points = points)
                Text(
                    text = stringResource(R.string.history_rom_sessions_count, points.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RomChart(points: List<RomHistoryPoint>) {
    val container = MaterialTheme.colorScheme.primaryContainer
    val axisColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.32f)
    val barColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.22f)
    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.secondary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .padding(8.dp)
    ) {
        val left = 28.dp.toPx()
        val top = 18.dp.toPx()
        val right = size.width - 16.dp.toPx()
        val bottom = size.height - 28.dp.toPx()
        val width = right - left
        val height = bottom - top
        val maxY = max(120f, points.maxOf { it.value }.coerceAtLeast(1f))

        drawLine(axisColor, Offset(left, top), Offset(left, bottom), strokeWidth = 1.dp.toPx())
        drawLine(axisColor, Offset(left, bottom), Offset(right, bottom), strokeWidth = 1.dp.toPx())

        val offsets = points.mapIndexed { index, point ->
            val x = if (points.size == 1) {
                left + width / 2f
            } else {
                left + (width * index / (points.lastIndex).toFloat())
            }
            val y = bottom - (point.value / maxY).coerceIn(0f, 1f) * height
            Offset(x, y)
        }

        offsets.forEach { point ->
            drawLine(
                color = barColor,
                start = Offset(point.x, bottom),
                end = point,
                strokeWidth = 7.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        offsets.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = lineColor,
                start = start,
                end = end,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        offsets.forEach { point ->
            drawCircle(pointColor, radius = 5.dp.toPx(), center = point)
            drawCircle(container, radius = 2.dp.toPx(), center = point)
        }

        if (offsets.size == 1) {
            drawCircle(
                color = lineColor.copy(alpha = 0.2f),
                radius = 14.dp.toPx(),
                center = offsets.first(),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
private fun MetricsRow(metrics: HistoryMetrics) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            modifier = Modifier.weight(1f),
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
            value = metrics.correctRepPercentage?.let {
                stringResource(R.string.history_percent_value, it)
            } ?: stringResource(R.string.history_not_available),
            label = stringResource(R.string.history_correct_reps_label)
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            icon = { Icon(Icons.Default.TrendingUp, contentDescription = null) },
            value = metrics.improvementDegrees?.formatSignedDegrees()
                ?: stringResource(R.string.history_not_available),
            label = stringResource(R.string.history_improvement_label)
        )
    }
}

@Composable
private fun MetricCard(
    value: String,
    label: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(112.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.secondary
                ) {
                    icon()
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HistorySessionCard(session: HistorySession) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.history_session_completed),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatHistoryDate(session.finalizedAt ?: session.savedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (session.requiresClinicalReview) {
                    ReviewBadge()
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = session.averageAchievedRom?.let {
                        stringResource(R.string.history_session_rom, it.roundToDisplay())
                    } ?: stringResource(R.string.history_session_no_rom),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(
                        R.string.history_session_reps,
                        session.goodRepetitions,
                        session.totalRepetitions
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                session.painLevel?.let {
                    Text(
                        text = stringResource(R.string.history_session_pain, it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (session.compensatoryMovementsDetected > 0) {
                    Text(
                        text = stringResource(
                            R.string.history_session_compensations,
                            session.compensatoryMovementsDetected
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewBadge() {
    val colors = ExtendedTheme.colors.warning
    Surface(
        shape = RoundedCornerShape(50),
        color = colors.colorContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = colors.onColorContainer
            )
            Text(
                text = stringResource(R.string.history_session_review),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onColorContainer
            )
        }
    }
}

@Composable
private fun HistoryMessage(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (compact) 16.dp else 24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 16.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = if (compact) Alignment.Start else Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = if (compact) FontStyle.Normal else FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Int.formatSignedDegrees(): String =
    if (this > 0) "+$this°" else "$this°"

private fun Double.roundToDisplay(): Int = roundToInt()

private fun formatHistoryDate(value: String): String {
    val formatter = DateTimeFormatter
        .ofPattern("d MMM yyyy", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
    return try {
        formatter.format(Instant.parse(value))
    } catch (_: DateTimeParseException) {
        value.take(10)
    }
}
