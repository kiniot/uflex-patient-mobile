package com.kiniot.uflex.features.history.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiniot.uflex.R
import com.kiniot.uflex.core.designsystem.theme.ExtendedTheme
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// ── Chart palette (dark-themed) ───────────────────────────────────────────────

private val ChartBg = Color(0xFF052B3C)
private val ChartBarTop = Color(0xFF22C5C5)
private val ChartBarBottom = Color(0xFF0B6E7E)
private val ChartLine = Color(0xFF89D0ED)
private val ChartGrid = Color(0xFF0F3D4E)
private val ChartLabel = Color(0xFF7AAFC4)

// ── Mock data ─────────────────────────────────────────────────────────────────

private val activeRomData = listOf(35f, 45f, 52f, 60f, 68f, 76f, 85f, 95f, 105f)
private val passiveRomData = listOf(42f, 54f, 62f, 72f, 80f, 88f, 96f, 104f, 112f)

enum class RomMode { Active, Passive }

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun HistoryScreen(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var romMode by remember { mutableStateOf(RomMode.Active) }
    val romData = if (romMode == RomMode.Active) activeRomData else passiveRomData

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding() + 24.dp,
            bottom = paddingValues.calculateBottomPadding() + 24.dp,
            start = 16.dp,
            end = 16.dp,
        ),
    ) {
        item { HeaderSection() }
        item { Spacer(Modifier.height(20.dp)) }
        item { RomEvolutionCard(romMode = romMode, romData = romData, onModeChange = { romMode = it }) }
        item { Spacer(Modifier.height(20.dp)) }
        item { StatsRow() }
        item { Spacer(Modifier.height(20.dp)) }
        item { DoctorNoteCard() }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun HeaderSection() {
    Column {
        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.history_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── ROM Evolution card ────────────────────────────────────────────────────────

@Composable
private fun RomEvolutionCard(
    romMode: RomMode,
    romData: List<Float>,
    onModeChange: (RomMode) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title + toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.history_rom_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                RomModeToggle(romMode, onModeChange)
            }
            Spacer(Modifier.height(12.dp))
            RomBarChart(data = romData)
        }
    }
}

@Composable
private fun RomModeToggle(current: RomMode, onSelect: (RomMode) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        listOf(RomMode.Active to R.string.history_rom_active, RomMode.Passive to R.string.history_rom_passive)
            .forEach { (mode, labelRes) ->
                val isSelected = current == mode
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { onSelect(mode) },
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.outlineVariant,
                            ),
                    )
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
    }
}

// ── Bar chart (Canvas) ────────────────────────────────────────────────────────

@Composable
private fun RomBarChart(data: List<Float>, maxValue: Float = 120f) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 9.sp, color = ChartLabel)
    val yLabels = listOf("120", "90", "60", "30", "0")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(192.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ChartBg),
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val leftPad = 34.dp.toPx()
            val rightPad = 12.dp.toPx()
            val topPad = 12.dp.toPx()
            val bottomPad = 24.dp.toPx()
            val chartW = size.width - leftPad - rightPad
            val chartH = size.height - topPad - bottomPad

            // Grid lines + y-axis labels
            yLabels.forEachIndexed { i, label ->
                val fraction = i / (yLabels.size - 1f)
                val y = topPad + chartH * fraction
                drawLine(
                    color = ChartGrid,
                    start = Offset(leftPad, y),
                    end = Offset(leftPad + chartW, y),
                    strokeWidth = 0.6.dp.toPx(),
                )
                val measured = textMeasurer.measure(label, labelStyle)
                drawText(
                    measured,
                    topLeft = Offset(
                        leftPad - measured.size.width - 4.dp.toPx(),
                        y - measured.size.height / 2f,
                    ),
                )
            }

            // Bars
            val barSlot = chartW / data.size
            val barW = barSlot * 0.55f

            data.forEachIndexed { index, value ->
                val barH = chartH * (value / maxValue)
                val bx = leftPad + barSlot * index + (barSlot - barW) / 2f
                val by = topPad + chartH - barH
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(ChartBarTop, ChartBarBottom),
                        startY = by,
                        endY = by + barH,
                    ),
                    topLeft = Offset(bx, by),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }

            // Trend line (smooth bezier through bar tops)
            val trendPoints = data.mapIndexed { i, v ->
                Offset(
                    leftPad + barSlot * i + barSlot / 2f,
                    topPad + chartH * (1f - v / maxValue),
                )
            }
            val path = Path()
            trendPoints.forEachIndexed { i, pt ->
                if (i == 0) {
                    path.moveTo(pt.x, pt.y)
                } else {
                    val prev = trendPoints[i - 1]
                    val cpX = (prev.x + pt.x) / 2f
                    path.cubicTo(cpX, prev.y, cpX, pt.y, pt.x, pt.y)
                }
            }
            drawPath(path, color = ChartLine, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round))

            // Arrowhead at end of trend line
            val last = trendPoints.last()
            val prev = trendPoints[trendPoints.size - 2]
            val angle = atan2(last.y - prev.y, last.x - prev.x)
            val arrowLen = 10.dp.toPx()
            val arrowSpread = 0.4f
            fun arrowWing(side: Float) = Offset(
                last.x - arrowLen * cos(angle - side * arrowSpread),
                last.y - arrowLen * sin(angle - side * arrowSpread),
            )
            drawLine(ChartLine, last, arrowWing(1f), 1.8.dp.toPx(), cap = StrokeCap.Round)
            drawLine(ChartLine, last, arrowWing(-1f), 1.8.dp.toPx(), cap = StrokeCap.Round)

            // SEM label bottom-right
            val sem = textMeasurer.measure("SEM", labelStyle)
            drawText(
                sem,
                topLeft = Offset(
                    size.width - sem.size.width - 6.dp.toPx(),
                    size.height - sem.size.height - 3.dp.toPx(),
                ),
            )
        }
    }
}

// ── Stats row ─────────────────────────────────────────────────────────────────

@Composable
private fun StatsRow() {
    val successColors = ExtendedTheme.colors.success

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Adherencia
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(successColors.colorContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = successColors.onColorContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.history_adherence_value),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.history_adherence_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Mejoría Total
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.history_improvement_value),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.history_improvement_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Doctor note card ──────────────────────────────────────────────────────────

@Composable
private fun DoctorNoteCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Doctor header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                // Doctor info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.history_doctor_name),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.history_doctor_specialty),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 0.5.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = stringResource(R.string.history_doctor_subject_tag),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                // Date + chevron
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.history_note_date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Note subject
            Text(
                text = stringResource(R.string.history_note_subject),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                ),
            )
            Spacer(Modifier.height(6.dp))
            // Note body
            Text(
                text = stringResource(R.string.history_note_body),
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
