package com.kiniot.uflex.features.exercise.presentation

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kiniot.uflex.R

private data class PainOption(
    val labelRes: Int,
    val icon: ImageVector,
    val centerValue: Float,
    val range: IntRange,
)

private val painOptions = listOf(
    PainOption(R.string.feedback_pain_none, Icons.Default.SentimentVerySatisfied, 0f, 0..1),
    PainOption(R.string.feedback_pain_mild, Icons.Default.SentimentSatisfied, 2.5f, 2..3),
    PainOption(R.string.feedback_pain_moderate, Icons.Default.SentimentNeutral, 5f, 4..6),
    PainOption(R.string.feedback_pain_strong, Icons.Default.SentimentDissatisfied, 7.5f, 7..8),
    PainOption(R.string.feedback_pain_intense, Icons.Default.SentimentVeryDissatisfied, 10f, 9..10),
)

private val sensationOptions = listOf(
    R.string.feedback_sensation_fatigue,
    R.string.feedback_sensation_fluid,
    R.string.feedback_sensation_stiffness,
    R.string.feedback_sensation_pulses,
)

@Composable
fun SessionFeedbackScreen(
    paddingValues: PaddingValues,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var painLevel by remember { mutableFloatStateOf(5f) }
    var selectedSensations by remember { mutableStateOf(setOf(R.string.feedback_sensation_fluid)) }
    var comment by remember { mutableStateOf("") }

    val selectedPainIndex = when (painLevel.toInt()) {
        in 0..1 -> 0
        in 2..3 -> 1
        in 4..6 -> 2
        in 7..8 -> 3
        else -> 4
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        // Header
        Text(
            text = stringResource(R.string.feedback_section_label),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = androidx.compose.ui.unit.TextUnit(
                    1.5f,
                    androidx.compose.ui.unit.TextUnitType.Sp,
                ),
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.feedback_pain_question),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(28.dp))

        // Emoji selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            painOptions.forEachIndexed { index, option ->
                PainEmojiItem(
                    option = option,
                    isSelected = index == selectedPainIndex,
                    onClick = { painLevel = option.centerValue },
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Slider
        Slider(
            value = painLevel,
            onValueChange = { painLevel = it },
            valueRange = 0f..10f,
            steps = 9,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.feedback_slider_min),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.feedback_slider_max),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(28.dp))

        // Sensations section
        Text(
            text = stringResource(R.string.feedback_sensations_question),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))

        // Two chips per row
        sensationOptions.chunked(2).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowOptions.forEach { optionRes ->
                    FilterChip(
                        selected = optionRes in selectedSensations,
                        onClick = {
                            selectedSensations = if (optionRes in selectedSensations) {
                                selectedSensations - optionRes
                            } else {
                                selectedSensations + optionRes
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(optionRes),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))

        // Comment field
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            placeholder = {
                Text(
                    text = stringResource(R.string.feedback_comment_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            maxLines = 4,
        )

        Spacer(Modifier.height(28.dp))

        // Finalize button
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = stringResource(R.string.feedback_finalize),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.size(8.dp))
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PainEmojiItem(
    option: PainOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val surface = MaterialTheme.colorScheme.surfaceContainerHighest

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() },
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isSelected) onBg else surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = stringResource(option.labelRes),
                tint = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(option.labelRes),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            ),
            color = if (isSelected) MaterialTheme.colorScheme.onBackground
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
