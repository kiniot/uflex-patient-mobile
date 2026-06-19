package com.kiniot.uflex.core.navigation

import com.kiniot.uflex.features.auth.navigation.authGraph
import com.kiniot.uflex.features.main.navigation.mainGraph
import com.kiniot.uflex.features.splash.presentation.splash.SplashScreen
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun RootNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Splash
    ) {
        composable<Splash> {
            SplashScreen(
                onSessionActive = {
                    navController.navigate(MainGraph) {
                        popUpTo<Splash> { inclusive = true }
                    }
                },
                onSessionInactive = {
                    navController.navigate(AuthGraph) {
                        popUpTo<Splash> { inclusive = true }
                    }
                }
            )
        }

        authGraph(navController)

        mainGraph(navController)
    }
}
