package com.kiniot.uflex.features.main.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.PaddingValues
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.kiniot.uflex.core.navigation.AuthGraph
import com.kiniot.uflex.core.navigation.MainGraph
import com.kiniot.uflex.features.main.presentation.shell.MainShell
import com.kiniot.uflex.features.main.presentation.shell.OverlayScreenContainer
import com.kiniot.uflex.features.main.presentation.shell.overlayEnterTransition
import com.kiniot.uflex.features.main.presentation.shell.overlayPopExitTransition
import com.kiniot.uflex.features.main.presentation.shell.overlayShellExitTransition
import com.kiniot.uflex.features.main.presentation.shell.overlayShellPopEnterTransition
import com.kiniot.uflex.features.plan.navigation.ExerciseDetailRoute
import com.kiniot.uflex.features.plan.presentation.detail.ExerciseDetailScreen
import com.kiniot.uflex.features.plan.presentation.detail.ExerciseDetailTopBar
import com.kiniot.uflex.features.profile.navigation.EditContactInfoRoute
import com.kiniot.uflex.features.profile.navigation.ProfileRoute
import com.kiniot.uflex.features.profile.presentation.EditContactInfoScreen
import com.kiniot.uflex.features.profile.presentation.EditContactInfoTopBar
import com.kiniot.uflex.features.profile.presentation.ProfileScreen
import com.kiniot.uflex.features.profile.presentation.ProfileTopBar
import com.kiniot.uflex.features.therapy.navigation.SessionExecutionRoute
import com.kiniot.uflex.features.therapy.navigation.SessionPreparationRoute
import com.kiniot.uflex.features.therapy.presentation.execution.SessionExecutionScreen
import com.kiniot.uflex.features.therapy.presentation.execution.SessionExecutionTopBar
import com.kiniot.uflex.features.therapy.presentation.execution.SessionExecutionViewModel
import com.kiniot.uflex.features.therapy.presentation.preparation.SessionPreparationScreen
import com.kiniot.uflex.features.therapy.presentation.preparation.SessionPreparationTopBar

/**
 * The authenticated area. `MainShellRoute` renders the tab shell; the detail screens are its
 * siblings here (not nested inside the shell) so they share the root back stack: system back and
 * the top-bar arrow are the same `popBackStack()`, one press, returning to the tab of origin.
 * `MainShellRoute` holds static (no exit/pop-enter animation) while a detail slides over it.
 */
fun NavGraphBuilder.mainGraph(navController: NavHostController) {
    navigation<MainGraph>(startDestination = MainShellRoute) {
        composable<MainShellRoute>(
            exitTransition = overlayShellExitTransition,
            popEnterTransition = overlayShellPopEnterTransition
        ) {
            MainShell(
                onNavigateToExerciseDetail = { exerciseId ->
                    navController.navigate(ExerciseDetailRoute(exerciseId)) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSessionPreparation = { treatmentPlanId ->
                    navController.navigate(SessionPreparationRoute(treatmentPlanId)) {
                        launchSingleTop = true
                    }
                },
                onNavigateToProfile = {
                    navController.navigate(ProfileRoute) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<ProfileRoute>(
            enterTransition = overlayEnterTransition,
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = overlayPopExitTransition
        ) {
            OverlayScreenContainer(
                topBar = {
                    ProfileTopBar(
                        onBackClick = { navController.popBackStack() }
                    )
                }
            ) {
                ProfileScreen(
                    paddingValues = PaddingValues(),
                    onEditContactInfo = { profile ->
                        navController.navigate(
                            EditContactInfoRoute(
                                email = profile.email,
                                countryCode = profile.countryCode,
                                phoneNumber = profile.phoneNumber
                            )
                        )
                    },
                    onSignedOut = {
                        navController.navigate(AuthGraph) {
                            popUpTo<MainGraph> { inclusive = true }
                        }
                    }
                )
            }
        }

        composable<EditContactInfoRoute>(
            enterTransition = overlayEnterTransition,
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = overlayPopExitTransition
        ) { backStackEntry ->
            val args = backStackEntry.toRoute<EditContactInfoRoute>()
            OverlayScreenContainer(
                topBar = {
                    EditContactInfoTopBar(
                        onBackClick = { navController.popBackStack() }
                    )
                }
            ) {
                EditContactInfoScreen(
                    paddingValues = PaddingValues(),
                    initialEmail = args.email,
                    initialCountryCode = args.countryCode,
                    initialPhoneNumber = args.phoneNumber,
                    onSaved = {
                        navController.navigate(ProfileRoute) {
                            popUpTo<ProfileRoute> { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        composable<ExerciseDetailRoute>(
            enterTransition = overlayEnterTransition,
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = overlayPopExitTransition
        ) {
            OverlayScreenContainer(
                topBar = {
                    ExerciseDetailTopBar(
                        onBackClick = { navController.popBackStack() }
                    )
                }
            ) {
                ExerciseDetailScreen(
                    paddingValues = PaddingValues()
                )
            }
        }

        composable<SessionPreparationRoute>(
            enterTransition = overlayEnterTransition,
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = overlayPopExitTransition
        ) {
            OverlayScreenContainer(
                topBar = {
                    SessionPreparationTopBar(
                        onBackClick = { navController.popBackStack() }
                    )
                }
            ) {
                SessionPreparationScreen(
                    paddingValues = PaddingValues(),
                    onBack = { navController.popBackStack() },
                    onNavigateToExecution = { sessionId ->
                        navController.navigate(SessionExecutionRoute(sessionId)) {
                            popUpTo<SessionPreparationRoute> { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        composable<SessionExecutionRoute>(
            enterTransition = overlayEnterTransition,
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = overlayPopExitTransition
        ) {
            // Hoist the VM so the top-bar arrow and the screen's own back share one instance:
            // both route "back" through onBackPressed(), which confirms termination mid-session.
            val executionViewModel: SessionExecutionViewModel = hiltViewModel()
            OverlayScreenContainer(
                topBar = {
                    SessionExecutionTopBar(
                        onBackClick = { executionViewModel.onBackPressed() }
                    )
                }
            ) {
                SessionExecutionScreen(
                    paddingValues = PaddingValues(),
                    onBack = { navController.popBackStack() },
                    viewModel = executionViewModel
                )
            }
        }
    }
}
