package com.kiniot.uflex.features.splash.presentation.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.kiniot.uflex.R
import com.kiniot.uflex.core.designsystem.components.PulsingRings
import com.kiniot.uflex.core.designsystem.theme.onPrimaryContainerLight
import com.kiniot.uflex.core.designsystem.theme.primaryLight

// Monochromatic brand gradient (brightened teal -> primary -> deep container).
private val SplashGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF12889F), primaryLight, onPrimaryContainerLight)
)

@Composable
fun SplashScreen(
    onSessionActive: () -> Unit,
    onSessionInactive: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    LaunchedEffect(viewModel) {
        viewModel.resolveDestination()
        viewModel.navigation.collect { isSessionActive ->
            if (isSessionActive) {
                onSessionActive()
            } else {
                onSessionInactive()
            }
        }
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val tokenScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "tokenScale"
    )
    val tokenAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "tokenAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PulsingRings(
                ringColor = Color.White,
                diameter = 260.dp,
                modifier = Modifier.alpha(tokenAlpha)
            ) {
                BrandToken(
                    modifier = Modifier.scale(tokenScale)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(durationMillis = 600, delayMillis = 250)) +
                    slideInVertically(tween(durationMillis = 600, delayMillis = 250)) { it / 3 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.splash_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

/**
 * The uFlex brandmark on a white circle — mirrors the native SplashScreen icon so the system
 * splash hands off seamlessly into this animated screen.
 */
@Composable
private fun BrandToken(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(132.dp),
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Image(
            painter = painterResource(R.drawable.logo_uflex_brandmark_original),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.padding(28.dp)
        )
    }
}
