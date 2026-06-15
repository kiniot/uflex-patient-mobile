package com.kiniot.uflex.features.auth.navigation

import com.kiniot.uflex.core.navigation.AuthGraph
import com.kiniot.uflex.core.navigation.MainGraph
import com.kiniot.uflex.features.auth.presentation.signin.SignInScreen
import com.kiniot.uflex.features.auth.presentation.signup.SignUpScreen
import com.kiniot.uflex.features.auth.presentation.verifyaccount.VerifyAccountScreen
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute

fun NavGraphBuilder.authGraph(navController: NavHostController) {
    navigation<AuthGraph>(startDestination = SignIn) {
        composable<SignIn> {
            SignInScreen(
                onSignInSuccess = {
                    navController.navigate(MainGraph) {
                        popUpTo<SignIn> { inclusive = true }
                    }
                },
                onGoToSignUp = { navController.navigate(SignUp) }
            )
        }

        composable<SignUp> {
            SignUpScreen(
                onSignUpSuccess = { email ->
                    navController.navigate(VerifyEmail(email))
                }
            )
        }

        composable<VerifyEmail> { backStackEntry ->
            val args = backStackEntry.toRoute<VerifyEmail>()
            VerifyAccountScreen(email = args.email)
        }
    }
}
