package com.kiniot.uflex.features.main.presentation.shell

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
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
import com.kiniot.uflex.features.home.navigation.HomeRoute
import com.kiniot.uflex.features.home.presentation.home.HomeScreen
import com.kiniot.uflex.features.main.navigation.MainDevicesRoute
import com.kiniot.uflex.features.main.navigation.MainExercisesRoute
import com.kiniot.uflex.features.main.navigation.MainHistoryRoute
import com.kiniot.uflex.features.profile.navigation.EditContactInfoRoute
import com.kiniot.uflex.features.profile.navigation.ProfileRoute
import com.kiniot.uflex.features.profile.presentation.EditContactInfoScreen
import com.kiniot.uflex.features.profile.presentation.EditContactInfoTopBar
import com.kiniot.uflex.features.profile.presentation.ProfileTopBar
import com.kiniot.uflex.features.profile.presentation.ProfileScreen

@Composable
fun MainShell(
    onSignedOut: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val shouldShowBottomBar = currentDestination.shouldShowMainBottomBar()
    val isEditContactInfoRoute = currentDestination?.hierarchy?.any {
        it.hasRoute(EditContactInfoRoute::class)
    } == true
    val isProfileRoute = currentDestination?.hierarchy?.any {
        it.hasRoute(ProfileRoute::class)
    } == true

    Scaffold(
        topBar = {
            if (isEditContactInfoRoute) {
                EditContactInfoTopBar(
                    onBackClick = { navController.popBackStack() }
                )
            } else if (isProfileRoute) {
                ProfileTopBar(
                    onBackClick = { navController.popBackStack() }
                )
            } else {
                MainTopBar(
                    onProfileClick = {
                        val isProfileSection = currentDestination?.hierarchy?.any { destination ->
                            destination.hasRoute(ProfileRoute::class) ||
                                destination.hasRoute(EditContactInfoRoute::class)
                        } == true

                        if (!isProfileSection) {
                            navController.navigate(ProfileRoute) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        },
        bottomBar = if (shouldShowBottomBar) {
            {
            NavigationBar {
                MainNavigationItem.items.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = stringResource(item.labelRes)
                            )
                        },
                        label = { Text(stringResource(item.labelRes)) },
                        selected = currentDestination?.hierarchy?.any {
                            it.hasRoute(item.route::class)
                        } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
        } else {
            {}
        }
    ) { innerPadding ->
        MainShellNavHost(
            innerPadding = innerPadding,
            navController = navController,
            onSignedOut = onSignedOut
        )
    }
}

@Composable
private fun MainShellNavHost(
    innerPadding: PaddingValues,
    navController: NavHostController,
    onSignedOut: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute
    ) {
        composable<HomeRoute> {
            HomeScreen(
                paddingValues = innerPadding
            )
        }

        composable<MainDevicesRoute> {
            MainPlaceholderScreen(
                title = stringResource(R.string.main_devices_placeholder),
                paddingValues = innerPadding
            )
        }

        composable<MainExercisesRoute> {
            MainPlaceholderScreen(
                title = stringResource(R.string.main_exercises_placeholder),
                paddingValues = innerPadding
            )
        }

        composable<MainHistoryRoute> {
            MainPlaceholderScreen(
                title = stringResource(R.string.main_history_placeholder),
                paddingValues = innerPadding
            )
        }

        composable<ProfileRoute>(
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth / 6 },
                    animationSpec = tween(durationMillis = 220)
                ) + fadeIn(animationSpec = tween(durationMillis = 180))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth / 12 },
                    animationSpec = tween(durationMillis = 220)
                ) + fadeOut(animationSpec = tween(durationMillis = 180))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth / 6 },
                    animationSpec = tween(durationMillis = 220)
                ) + fadeIn(animationSpec = tween(durationMillis = 180))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth / 12 },
                    animationSpec = tween(durationMillis = 220)
                ) + fadeOut(animationSpec = tween(durationMillis = 180))
            }
        ) {
            ProfileScreen(
                paddingValues = innerPadding,
                onEditContactInfo = { profile ->
                    navController.navigate(
                        EditContactInfoRoute(
                            email = profile.email,
                            countryCode = profile.countryCode,
                            phoneNumber = profile.phoneNumber
                        )
                    )
                },
                onSignedOut = onSignedOut
            )
        }

        composable<EditContactInfoRoute>(
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth / 6 },
                    animationSpec = tween(durationMillis = 220)
                ) + fadeIn(animationSpec = tween(durationMillis = 180))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth / 12 },
                    animationSpec = tween(durationMillis = 220)
                ) + fadeOut(animationSpec = tween(durationMillis = 180))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth / 6 },
                    animationSpec = tween(durationMillis = 220)
                ) + fadeIn(animationSpec = tween(durationMillis = 180))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth / 12 },
                    animationSpec = tween(durationMillis = 220)
                ) + fadeOut(animationSpec = tween(durationMillis = 180))
            }
        ) { backStackEntry ->
            val args = backStackEntry.toRoute<EditContactInfoRoute>()
            EditContactInfoScreen(
                paddingValues = innerPadding,
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
}

@Composable
private fun MainPlaceholderScreen(
    title: String,
    paddingValues: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

private fun NavDestination?.shouldShowMainBottomBar(): Boolean {
    if (this == null) return true

    return hierarchy.any {
        it.hasRoute(HomeRoute::class) ||
            it.hasRoute(MainDevicesRoute::class) ||
            it.hasRoute(MainExercisesRoute::class) ||
            it.hasRoute(MainHistoryRoute::class)
    }
}
