package com.kiniot.uflex.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.kiniot.uflex.core.designsystem.components.feedback.SnackbarManager
import com.kiniot.uflex.core.designsystem.components.feedback.UFlexSnackbarHost
import com.kiniot.uflex.core.designsystem.components.feedback.rememberUFlexSnackbarState
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SnackbarManagerEntryPoint {
    fun snackbarManager(): SnackbarManager
}

@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val snackbarState = rememberUFlexSnackbarState()
    val snackbarManager = EntryPointAccessors
        .fromApplication(context, SnackbarManagerEntryPoint::class.java)
        .snackbarManager()

    LaunchedEffect(snackbarManager, context) {
        snackbarManager.messages.collect { message ->
            snackbarState.showMessage(context, message)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RootNavGraph(navController = navController)

        UFlexSnackbarHost(
            snackbarState = snackbarState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .padding(16.dp)
        )
    }
}
