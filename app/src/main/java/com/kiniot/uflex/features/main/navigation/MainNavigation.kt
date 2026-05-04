package com.kiniot.uflex.features.main.navigation

import com.kiniot.uflex.core.navigation.MainGraph
import com.kiniot.uflex.features.main.presentation.base.MainShell
import com.kiniot.uflex.features.main.presentation.dashboard.DashboardScreen
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation

fun NavGraphBuilder.mainGraph(navController: NavHostController) {
    navigation<MainGraph>(startDestination = DashboardRoute) {
        composable<DashboardRoute> {
            MainShell(navController) { padding ->
                DashboardScreen(modifier = Modifier.padding(padding))
            }
        }
    }
}
