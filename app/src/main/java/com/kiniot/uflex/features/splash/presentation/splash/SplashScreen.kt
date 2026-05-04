package com.kiniot.uflex.features.splash.presentation.splash

import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun SplashScreen(
    onSessionActive: () -> Unit,
    onSessionInactive: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2000)
        val isLogged = false
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "uFlex", style = MaterialTheme.typography.displayLarge)
    }
}
