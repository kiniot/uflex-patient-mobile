package com.kiniot.uflex.features.main.navigation

import com.kiniot.uflex.core.navigation.MainGraph
import com.kiniot.uflex.features.main.presentation.shell.MainShell
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation

fun NavGraphBuilder.mainGraph(navController: NavHostController) {
    navigation<MainGraph>(startDestination = MainShellRoute) {
        composable<MainShellRoute> {
            MainShell()
        }
    }
}
