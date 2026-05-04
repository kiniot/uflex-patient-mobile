package com.kiniot.uflex.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    RootNavGraph(navController = navController)
}
