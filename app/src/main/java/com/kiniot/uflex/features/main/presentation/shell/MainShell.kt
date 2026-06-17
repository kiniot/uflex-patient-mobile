package com.kiniot.uflex.features.main.presentation.shell

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.kiniot.uflex.R
import com.kiniot.uflex.features.device.presentation.DeviceScreen
import com.kiniot.uflex.features.exercise.navigation.ExercisePrepareRoute
import com.kiniot.uflex.features.exercise.navigation.ExerciseSessionRoute
import com.kiniot.uflex.features.exercise.navigation.SessionFeedbackRoute
import com.kiniot.uflex.features.exercise.presentation.ExercisePrepareScreen
import com.kiniot.uflex.features.exercise.presentation.ExerciseSessionScreen
import com.kiniot.uflex.features.exercise.presentation.ExerciseTopBar
import com.kiniot.uflex.features.exercise.presentation.ExercisesListScreen
import com.kiniot.uflex.features.exercise.presentation.SessionFeedbackScreen
import com.kiniot.uflex.features.home.navigation.HomeRoute
import com.kiniot.uflex.features.home.presentation.home.HomeScreen
import com.kiniot.uflex.features.main.navigation.MainDevicesRoute
import com.kiniot.uflex.features.main.navigation.MainExercisesRoute
import com.kiniot.uflex.features.history.presentation.HistoryScreen
import com.kiniot.uflex.features.main.navigation.MainHistoryRoute
import com.kiniot.uflex.features.profile.navigation.EditContactInfoRoute
import com.kiniot.uflex.features.profile.navigation.ProfileRoute
import com.kiniot.uflex.features.profile.presentation.EditContactInfoScreen
import com.kiniot.uflex.features.profile.presentation.EditContactInfoTopBar
import com.kiniot.uflex.features.profile.presentation.ProfileScreen
import com.kiniot.uflex.features.profile.presentation.ProfileTopBar
import kotlinx.serialization.Serializable

@Composable
fun MainShell(
    onSignedOut: () -> Unit,
) {
    val mainNavController = rememberNavController()
    val overlayNavController = rememberNavController()
    val mainBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val mainDestination = mainBackStackEntry?.destination
    val shouldShowBottomBar = mainDestination.shouldShowMainBottomBar()
    val overlayBackStackEntry by overlayNavController.currentBackStackEntryAsState()
    val overlayDestination = overlayBackStackEntry?.destination
    val isOverlayVisible = overlayDestination?.hierarchy?.any {
        it.hasRoute(ProfileRoute::class) || it.hasRoute(EditContactInfoRoute::class)
    } == true
    val isExerciseDetailRoute = mainDestination.isExerciseDetailRoute()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (isExerciseDetailRoute) {
                    ExerciseTopBar(onBackClick = { mainNavController.popBackStack() })
                } else {
                    MainTopBar(
                        onProfileClick = {
                            if (!isOverlayVisible) {
                                overlayNavController.navigate(ProfileRoute) {
                                    launchSingleTop = true
                                }
                            }
                        },
                    )
                }
            },
            bottomBar = if (shouldShowBottomBar) {
                {
                    NavigationBar {
                        MainNavigationItem.items.forEach { item ->
                            val isExercisesTab = item is MainNavigationItem.Exercises
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        item.icon,
                                        contentDescription = stringResource(item.labelRes),
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                selected = mainDestination?.hierarchy?.any {
                                    it.hasRoute(item.route::class)
                                } == true || (isExercisesTab && isExerciseDetailRoute),
                                onClick = {
                                    mainNavController.navigate(item.route) {
                                        popUpTo(mainNavController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            )
                        }
                    }
                }
            } else {
                {}
            },
        ) { innerPadding ->
            MainTabsNavHost(
                innerPadding = innerPadding,
                navController = mainNavController,
            )
        }

        MainOverlayNavHost(
            navController = overlayNavController,
            onSignedOut = onSignedOut,
        )
    }
}

@Composable
private fun MainTabsNavHost(
    innerPadding: PaddingValues,
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
    ) {
        composable<HomeRoute> {
            HomeScreen(paddingValues = innerPadding)
        }

        composable<MainDevicesRoute> {
            DeviceScreen(paddingValues = innerPadding)
        }

        composable<MainExercisesRoute> {
            ExercisesListScreen(
                paddingValues = innerPadding,
                onStartExercise = {
                    navController.navigate(ExercisePrepareRoute) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<ExercisePrepareRoute> {
            ExercisePrepareScreen(
                paddingValues = innerPadding,
                onStart = {
                    navController.navigate(ExerciseSessionRoute) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<ExerciseSessionRoute> {
            ExerciseSessionScreen(
                paddingValues = innerPadding,
                onFinish = {
                    navController.navigate(SessionFeedbackRoute) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<SessionFeedbackRoute> {
            SessionFeedbackScreen(
                paddingValues = innerPadding,
                onDone = {
                    navController.popBackStack<MainExercisesRoute>(inclusive = false)
                },
            )
        }

        composable<MainHistoryRoute> {
            HistoryScreen(paddingValues = innerPadding)
        }
    }
}

@Composable
private fun MainOverlayNavHost(
    navController: NavHostController,
    onSignedOut: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = OverlayPlaceholderRoute,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable<OverlayPlaceholderRoute> {
            Box(modifier = Modifier.fillMaxSize())
        }

        composable<ProfileRoute>(
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 240),
                )
            },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 220),
                )
            },
        ) {
            OverlayScreenContainer(
                topBar = { ProfileTopBar(onBackClick = { navController.popBackStack() }) },
            ) {
                ProfileScreen(
                    paddingValues = PaddingValues(),
                    onEditContactInfo = { profile ->
                        navController.navigate(
                            EditContactInfoRoute(
                                email = profile.email,
                                countryCode = profile.countryCode,
                                phoneNumber = profile.phoneNumber,
                            ),
                        )
                    },
                    onSignedOut = onSignedOut,
                )
            }
        }

        composable<EditContactInfoRoute>(
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 240),
                )
            },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 220),
                )
            },
        ) { backStackEntry ->
            val args = backStackEntry.toRoute<EditContactInfoRoute>()
            OverlayScreenContainer(
                topBar = { EditContactInfoTopBar(onBackClick = { navController.popBackStack() }) },
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
                    },
                )
            }
        }
    }
}

@Composable
private fun OverlayScreenContainer(
    topBar: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            topBar()
            content()
        }
    }
}

@Serializable
private object OverlayPlaceholderRoute

@Composable
private fun MainPlaceholderScreen(
    title: String,
    paddingValues: PaddingValues,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

private fun NavDestination?.shouldShowMainBottomBar(): Boolean {
    if (this == null) return true
    return hierarchy.any {
        it.hasRoute(HomeRoute::class) ||
            it.hasRoute(MainDevicesRoute::class) ||
            it.hasRoute(MainExercisesRoute::class) ||
            it.hasRoute(ExercisePrepareRoute::class) ||
            it.hasRoute(ExerciseSessionRoute::class) ||
            it.hasRoute(SessionFeedbackRoute::class) ||
            it.hasRoute(MainHistoryRoute::class)
    }
}

private fun NavDestination?.isExerciseDetailRoute(): Boolean {
    if (this == null) return false
    return hierarchy.any {
        it.hasRoute(ExercisePrepareRoute::class) ||
            it.hasRoute(ExerciseSessionRoute::class) ||
            it.hasRoute(SessionFeedbackRoute::class)
    }
}
