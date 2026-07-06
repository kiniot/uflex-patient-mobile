package com.kiniot.uflex.features.main.presentation.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kiniot.uflex.R
import com.kiniot.uflex.features.home.presentation.home.HomeScreen
import com.kiniot.uflex.features.device.presentation.DeviceConnectionScreen
import com.kiniot.uflex.features.plan.presentation.exercises.ExercisesScreen

/**
 * The authenticated tab shell: top bar, bottom navigation, and the four tab screens. Detail/overlay
 * screens are NOT here — they live one level up as siblings of `MainShellRoute` in `MainGraph`,
 * navigated on the root controller via the callbacks below.
 *
 * Tabs are switched with a lightweight index + [rememberSaveableStateHolder] rather than a nested
 * `NavHost`. This matters: when a detail opens, this shell leaves composition and must be rebuilt
 * on the way back; rebuilding a nested `NavHost` (with typed-route graph parsing) cost ~450 ms on a
 * mid-range device, which froze the start of the back transition into a snap. A plain switcher
 * rebuilds cheaply, so the pop animation stays smooth. Per-tab UI state (scroll, etc.) is preserved
 * across switches and across the shell's own dispose/restore by the SaveableStateHolder.
 */
@Composable
fun MainShell(
    onNavigateToExerciseDetail: (String) -> Unit,
    onNavigateToSessionPreparation: (String) -> Unit,
    onNavigateToProfile: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val stateHolder = rememberSaveableStateHolder()

    // Back from a secondary tab returns to Home first (mirrors the previous nested-nav behavior);
    // from Home, this is disabled so back falls through to the root graph (exits the app).
    BackHandler(enabled = selectedTab != 0) { selectedTab = 0 }

    Scaffold(
        topBar = {
            MainTopBar(onProfileClick = onNavigateToProfile)
        },
        bottomBar = {
            NavigationBar {
                MainNavigationItem.items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = stringResource(item.labelRes)
                            )
                        },
                        label = { Text(stringResource(item.labelRes)) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        stateHolder.SaveableStateProvider(key = selectedTab) {
            when (selectedTab) {
                0 -> HomeScreen(
                    paddingValues = innerPadding
                )

                1 -> DeviceConnectionScreen(
                    paddingValues = innerPadding
                )

                2 -> ExercisesScreen(
                    paddingValues = innerPadding,
                    onExerciseClick = onNavigateToExerciseDetail,
                    onStartSession = onNavigateToSessionPreparation
                )

                else -> MainPlaceholderScreen(
                    title = stringResource(R.string.main_history_placeholder),
                    paddingValues = innerPadding
                )
            }
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
