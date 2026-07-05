package com.kiniot.uflex.features.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiniot.uflex.R
import com.kiniot.uflex.core.ui.asString
import com.kiniot.uflex.features.profile.domain.model.PatientProfile
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ProfileScreen(
    paddingValues: PaddingValues,
    onEditContactInfo: (PatientProfile) -> Unit,
    onSignedOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = uiState.profile

    LaunchedEffect(viewModel) {
        viewModel.signOutSuccess.collect {
            onSignedOut()
        }
    }

    when {
        uiState.isLoading -> ProfileLoadingState(paddingValues)
        profile != null -> ProfileContent(
            profile = profile,
            paddingValues = paddingValues,
            isSigningOut = uiState.isSigningOut,
            onEditContactInfo = onEditContactInfo,
            onSignOut = viewModel::onSignOut
        )
        else -> ProfileErrorState(
            paddingValues = paddingValues,
            message = uiState.errorMessage,
            isSigningOut = uiState.isSigningOut,
            onRetry = viewModel::onRetry,
            onSignOut = viewModel::onSignOut
        )
    }
}

@Composable
private fun ProfileLoadingState(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ProfileErrorState(
    paddingValues: PaddingValues,
    message: com.kiniot.uflex.core.ui.UiText?,
    isSigningOut: Boolean,
    onRetry: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.profile_error_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = message?.asString(context) ?: stringResource(R.string.profile_error_generic),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = onRetry, enabled = !isSigningOut) {
            Text(text = stringResource(R.string.profile_retry))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sign out is also offered here so a patient stuck on a failed profile load
        // (e.g. a stale session) can still leave the account, like in the happy path.
        FilledTonalButton(
            onClick = onSignOut,
            enabled = !isSigningOut,
            shape = RoundedCornerShape(18.dp)
        ) {
            if (isSigningOut) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(text = stringResource(R.string.profile_sign_out))
            }
        }
    }
}

@Composable
private fun ProfileContent(
    profile: PatientProfile,
    paddingValues: PaddingValues,
    isSigningOut: Boolean,
    onEditContactInfo: (PatientProfile) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]
    val dateFormatter = DateTimeFormatter
        .ofLocalizedDate(FormatStyle.LONG)
        .withLocale(locale)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(paddingValues)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        ProfileHeader(profile = profile)

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { onEditContactInfo(profile) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(text = stringResource(R.string.profile_edit_contact_action))
        }

        Spacer(modifier = Modifier.height(20.dp))

        ProfileSection(title = stringResource(R.string.profile_personal_information)) {
            ProfileField(
                label = stringResource(R.string.profile_dni),
                value = profile.dni
            )
            ProfileField(
                label = stringResource(R.string.profile_birth_date),
                value = dateFormatter.format(profile.birthDate)
            )
            ProfileField(
                label = stringResource(R.string.profile_gender),
                value = profile.gender.toUiText().asString(context)
            )
            ProfileField(
                label = stringResource(R.string.profile_phone_number),
                value = "${profile.countryCode} ${profile.phoneNumber}"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ProfileSection(title = stringResource(R.string.profile_clinical_information)) {
            ProfileField(
                label = stringResource(R.string.profile_medical_condition),
                value = profile.medicalCondition
            )
            ProfileField(
                label = stringResource(R.string.profile_status),
                value = profile.status.toUiText().asString(context)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        FilledTonalButton(
            onClick = onSignOut,
            enabled = !isSigningOut,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            if (isSigningOut) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(text = stringResource(R.string.profile_sign_out))
            }
        }
    }
}

@Composable
private fun ProfileHeader(profile: PatientProfile) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${profile.firstName} ${profile.lastName}".trim(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = profile.email,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            AssistChip(
                onClick = {},
                enabled = false,
                label = {
                    Text(text = profile.status.toUiText().asString(LocalContext.current))
                },
                colors = AssistChipDefaults.assistChipColors(
                    disabledContainerColor = profile.status.statusContainerColor(),
                    disabledLabelColor = profile.status.statusContentColor()
                )
            )
        }
    }
}

@Composable
private fun ProfileSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(12.dp))
    }
}
