package com.kiniot.uflex.features.main.presentation.shell

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.ui.graphics.vector.ImageVector
import com.kiniot.uflex.R
import com.kiniot.uflex.features.home.navigation.HomeRoute
import com.kiniot.uflex.features.main.navigation.MainDevicesRoute
import com.kiniot.uflex.features.main.navigation.MainExercisesRoute
import com.kiniot.uflex.features.main.navigation.MainHistoryRoute

sealed class MainNavigationItem<T : Any>(
    val route: T,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector
) {
    object Home : MainNavigationItem<HomeRoute>(
        route = HomeRoute,
        labelRes = R.string.main_tab_home,
        icon = Icons.Default.Home
    )

    object Devices : MainNavigationItem<MainDevicesRoute>(
        route = MainDevicesRoute,
        labelRes = R.string.main_tab_devices,
        icon = Icons.Default.SettingsInputAntenna
    )

    object Exercises : MainNavigationItem<MainExercisesRoute>(
        route = MainExercisesRoute,
        labelRes = R.string.main_tab_exercises,
        icon = Icons.Default.FitnessCenter
    )

    object History : MainNavigationItem<MainHistoryRoute>(
        route = MainHistoryRoute,
        labelRes = R.string.main_tab_history,
        icon = Icons.Default.History
    )

    companion object {
        val items = listOf(Home, Devices, Exercises, History)
    }
}
