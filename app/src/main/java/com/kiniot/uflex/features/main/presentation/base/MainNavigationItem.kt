package com.kiniot.uflex.features.main.presentation.base

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.kiniot.uflex.features.main.navigation.DashboardRoute

sealed class MainNavigationItem<T : Any>(
    val route: T,
    val label: String,
    val icon: ImageVector
) {
    object Dashboard : MainNavigationItem<DashboardRoute>(
        route = DashboardRoute,
        label = "Dashboard",
        icon = Icons.Default.Home
    )

    companion object {
        val items = listOf(Dashboard)
    }
}
