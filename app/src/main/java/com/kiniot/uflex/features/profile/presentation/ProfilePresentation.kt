package com.kiniot.uflex.features.profile.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kiniot.uflex.R
import com.kiniot.uflex.core.designsystem.theme.ExtendedTheme
import com.kiniot.uflex.core.result.AppError
import com.kiniot.uflex.core.result.toUserMessage
import com.kiniot.uflex.core.ui.UiText
import com.kiniot.uflex.features.profile.domain.model.PatientGender
import com.kiniot.uflex.features.profile.domain.model.PatientStatus
import androidx.compose.material3.MaterialTheme

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
