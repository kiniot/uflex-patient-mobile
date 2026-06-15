package com.kiniot.uflex.features.main.presentation.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.kiniot.uflex.features.main.navigation.MainHomeRoute

sealed class MainNavigationItem<T : Any>(
    val route: T,
    val label: String,
    val icon: ImageVector
) {
    object Home : MainNavigationItem<MainHomeRoute>(
        route = MainHomeRoute,
        label = "Inicio",
        icon = Icons.Default.Home
    )

    companion object {
        val items = listOf(Home)
    }
}
