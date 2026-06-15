package com.kiniot.uflex.features.main.presentation.shell

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
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kiniot.uflex.R
import com.kiniot.uflex.features.home.navigation.HomeRoute
import com.kiniot.uflex.features.home.presentation.home.HomeScreen
import com.kiniot.uflex.features.main.navigation.MainDevicesRoute
import com.kiniot.uflex.features.main.navigation.MainExercisesRoute
import com.kiniot.uflex.features.main.navigation.MainHistoryRoute

@Composable
fun MainShell() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        topBar = {
            MainTopBar()
        },
        bottomBar = {
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
    ) { innerPadding ->
        MainShellNavHost(
            innerPadding = innerPadding,
            navController = navController
        )
    }
}

@Composable
private fun MainShellNavHost(
    innerPadding: PaddingValues,
    navController: NavHostController
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
