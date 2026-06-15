package com.kiniot.uflex.features.auth.navigation

import com.kiniot.uflex.core.navigation.AuthGraph
import com.kiniot.uflex.core.navigation.MainGraph
import com.kiniot.uflex.features.auth.presentation.signin.SignInScreen
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation

fun NavGraphBuilder.authGraph(navController: NavHostController) {
    navigation<AuthGraph>(startDestination = SignIn) {
        composable<SignIn> {
            SignInScreen(
                onSignInSuccess = {
                    navController.navigate(MainGraph) {
                        popUpTo<SignIn> { inclusive = true }
                    }
                }
            )
        }
    }
}
