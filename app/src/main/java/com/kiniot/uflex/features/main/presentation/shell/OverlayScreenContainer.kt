package com.kiniot.uflex.features.main.presentation.shell

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry

/**
 * Full-screen container for a detail/overlay destination: a [Surface] over the app background
 * with the screen's own top bar stacked above its content. Detail screens draw their own top bar
 * (with `statusBarsPadding`) instead of the shell's [MainTopBar]/bottom bar, so they render
 * edge-to-edge as siblings of the tab shell in `MainGraph`.
 */
@Composable
internal fun OverlayScreenContainer(
    topBar: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            topBar()
            content()
        }
    }
}

/**
 * Cross-slide transitions between the tab shell (`MainShellRoute`) and a detail destination in
 * `MainGraph`. The detail (foreground) and the shell (background) move in lockstep so the two
 * full-screen panes tile perfectly at every frame — which makes the animation visible regardless
 * of z-order (nav-compose 2.9.x draws the pop-entering pane over the pop-exiting one, so a static
 * `EnterTransition.None` shell would otherwise occlude the detail's slide-out and look abrupt).
 * No fade, to match the "barrido" feel. Durations are paired per direction (open 240 / back 220).
 *
 * - Open (shell -> detail): detail slides in from the right ([overlayEnterTransition]); shell slides
 *   out to the left ([overlayShellExitTransition]).
 * - Back (detail -> shell): detail slides out to the right ([overlayPopExitTransition]); shell slides
 *   in from the left ([overlayShellPopEnterTransition]).
 */
internal val overlayEnterTransition:
    AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(durationMillis = 280)
    )
}

internal val overlayShellExitTransition:
    AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth },
        animationSpec = tween(durationMillis = 280)
    )
}

internal val overlayPopExitTransition:
    AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(durationMillis = 280)
    )
}

internal val overlayShellPopEnterTransition:
    AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth },
        animationSpec = tween(durationMillis = 280)
    )
}
