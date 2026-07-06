package com.kiniot.uflex.features.profile.presentation

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiniot.uflex.R
import com.kiniot.uflex.core.ui.UiText
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
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Hero
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InitialsAvatar(firstName = profile.firstName, lastName = profile.lastName, size = 96.dp)
            Text(
                text = "${profile.firstName} ${profile.lastName}".trim(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = profile.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            StatusChip(profile.status)
        }

        OutlinedButton(
            onClick = { onEditContactInfo(profile) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.profile_edit_contact_action))
        }

        InfoSectionCard(title = stringResource(R.string.profile_personal_information)) {
            InfoRow(Icons.Outlined.Badge, stringResource(R.string.profile_dni), profile.dni)
            InfoRow(Icons.Outlined.Cake, stringResource(R.string.profile_birth_date), dateFormatter.format(profile.birthDate))
            InfoRow(Icons.Outlined.Person, stringResource(R.string.profile_gender), profile.gender.toUiText().asString(context))
            InfoRow(
                Icons.Outlined.Phone,
                stringResource(R.string.profile_phone_number),
                "+${profile.countryCode.trimStart('+')} ${profile.phoneNumber}"
            )
        }

        InfoSectionCard(title = stringResource(R.string.profile_clinical_information)) {
            InfoRow(Icons.Outlined.MedicalServices, stringResource(R.string.profile_medical_condition), profile.medicalCondition)
            InfoRow(Icons.Outlined.VerifiedUser, stringResource(R.string.profile_status), profile.status.toUiText().asString(context))
        }

        Spacer(Modifier.height(4.dp))

        FilledTonalButton(
            onClick = onSignOut,
            enabled = !isSigningOut,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            if (isSigningOut) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            } else {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.profile_sign_out))
            }
        }

        // Clear the system navigation bar so the sign-out button isn't hidden behind it.
        Spacer(Modifier.height(4.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun ProfileLoadingState(paddingValues: PaddingValues) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 850), RepeatMode.Reverse),
        label = "shimmerAlpha"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        SkeletonBlock(Modifier.size(96.dp), alpha, CircleShape)
        SkeletonBlock(Modifier.fillMaxWidth(0.5f).height(24.dp), alpha)
        SkeletonBlock(Modifier.fillMaxWidth(0.7f).height(16.dp), alpha)
        Spacer(Modifier.height(4.dp))
        SkeletonBlock(Modifier.fillMaxWidth().height(150.dp), alpha, RoundedCornerShape(24.dp))
        SkeletonBlock(Modifier.fillMaxWidth().height(100.dp), alpha, RoundedCornerShape(24.dp))
    }
}

@Composable
private fun SkeletonBlock(
    modifier: Modifier,
    alpha: Float,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha), shape))
}

@Composable
private fun ProfileErrorState(
    paddingValues: PaddingValues,
    message: UiText?,
    isSigningOut: Boolean,
    onRetry: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.profile_error_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = message?.asString(context) ?: stringResource(R.string.profile_error_generic),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        Button(onClick = onRetry, enabled = !isSigningOut, shape = RoundedCornerShape(18.dp)) {
            Text(text = stringResource(R.string.profile_retry))
        }

        Spacer(Modifier.height(12.dp))

        // Sign out is also offered here so a patient stuck on a failed profile load
        // (e.g. a stale session) can still leave the account, like in the happy path.
        FilledTonalButton(
            onClick = onSignOut,
            enabled = !isSigningOut,
            shape = RoundedCornerShape(18.dp)
        ) {
            if (isSigningOut) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(text = stringResource(R.string.profile_sign_out))
            }
        }
    }
}
