package com.kiniot.uflex.features.splash.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiniot.uflex.features.auth.domain.usecase.HasActiveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.os.SystemClock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val hasActiveSessionUseCase: HasActiveSessionUseCase
) : ViewModel() {
    private val navigationChannel = Channel<Boolean>(capacity = Channel.BUFFERED)
    val navigation = navigationChannel.receiveAsFlow()

    fun resolveDestination() {
        viewModelScope.launch {
            // Run the (near-instant) session check immediately, but keep the branded splash on
            // screen for a minimum so the reveal animation is seen — no artificial "loading" delay.
            val startedAt = SystemClock.elapsedRealtime()
            val hasSession = hasActiveSessionUseCase()
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            val remaining = MIN_DISPLAY_MS - elapsed
            if (remaining > 0) delay(remaining)
            navigationChannel.send(hasSession)
        }
    }

    private companion object {
        const val MIN_DISPLAY_MS = 1100L
    }
}
