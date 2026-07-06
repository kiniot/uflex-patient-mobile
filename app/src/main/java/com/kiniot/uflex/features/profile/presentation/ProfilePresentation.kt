package com.kiniot.uflex.features.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kiniot.uflex.R
import com.kiniot.uflex.core.designsystem.theme.ExtendedTheme
import com.kiniot.uflex.core.result.AppError
import com.kiniot.uflex.core.result.toUserMessage
import com.kiniot.uflex.core.ui.UiText
import com.kiniot.uflex.core.ui.asString
import com.kiniot.uflex.features.profile.domain.model.PatientGender
import com.kiniot.uflex.features.profile.domain.model.PatientStatus

fun AppError.toProfileUserMessage(): UiText = toUserMessage()

fun PatientGender.toUiText(): UiText {
    return when (this) {
        PatientGender.Male -> UiText.Resource(R.string.profile_gender_male)
        PatientGender.Female -> UiText.Resource(R.string.profile_gender_female)
        PatientGender.Unknown -> UiText.Resource(R.string.profile_gender_unknown)
    }
}

fun PatientStatus.toUiText(): UiText {
    return when (this) {
        PatientStatus.Unassigned -> UiText.Resource(R.string.profile_status_unassigned)
        PatientStatus.InTreatment -> UiText.Resource(R.string.profile_status_in_treatment)
        PatientStatus.Completed -> UiText.Resource(R.string.profile_status_completed)
        PatientStatus.Discharged -> UiText.Resource(R.string.profile_status_discharged)
        PatientStatus.Inactive -> UiText.Resource(R.string.profile_status_inactive)
        PatientStatus.Unknown -> UiText.Resource(R.string.profile_status_unknown)
    }
}

@Composable
fun PatientStatus.statusContainerColor(): Color {
    val extendedColors = ExtendedTheme.colors
    val colors = MaterialTheme.colorScheme
    return when (this) {
        PatientStatus.Completed -> extendedColors.success.colorContainer
        PatientStatus.InTreatment -> extendedColors.info.colorContainer
        PatientStatus.Unassigned -> extendedColors.warning.colorContainer
        PatientStatus.Discharged -> colors.secondaryContainer
        PatientStatus.Inactive -> colors.surfaceContainerHighest
        PatientStatus.Unknown -> colors.surfaceVariant
    }
}

@Composable
fun PatientStatus.statusContentColor(): Color {
    val extendedColors = ExtendedTheme.colors
    val colors = MaterialTheme.colorScheme
    return when (this) {
        PatientStatus.Completed -> extendedColors.success.onColorContainer
        PatientStatus.InTreatment -> extendedColors.info.onColorContainer
        PatientStatus.Unassigned -> extendedColors.warning.onColorContainer
        PatientStatus.Discharged -> colors.onSecondaryContainer
        PatientStatus.Inactive -> colors.onSurfaceVariant
        PatientStatus.Unknown -> colors.onSurfaceVariant
    }
}

// -------------------------------------------------------------------------------------------------
// Visual atoms
// -------------------------------------------------------------------------------------------------

/** Circular avatar with the patient's initials (no avatar image exists in the domain model). */
@Composable
fun InitialsAvatar(
    firstName: String,
    lastName: String,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    val initials = buildString {
        firstName.trim().firstOrNull()?.let { append(it.uppercaseChar()) }
        lastName.trim().firstOrNull()?.let { append(it.uppercaseChar()) }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (initials.isBlank()) {
            Icon(
                Icons.Outlined.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size * 0.45f)
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/** Rounded status pill colored by the patient's treatment status. */
@Composable
fun StatusChip(status: PatientStatus) {
    Surface(shape = RoundedCornerShape(50), color = status.statusContainerColor()) {
        Text(
            text = status.toUiText().asString(LocalContext.current),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = status.statusContentColor(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

/** A titled card grouping related [InfoRow]s. */
@Composable
fun InfoSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        }
    }
}

/** A labeled value with a leading icon disc. */
@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
