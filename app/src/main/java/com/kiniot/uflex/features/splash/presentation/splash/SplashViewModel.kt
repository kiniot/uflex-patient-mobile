package com.kiniot.uflex.features.splash.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiniot.uflex.features.auth.domain.usecase.HasActiveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
            delay(2000)
            navigationChannel.send(hasActiveSessionUseCase())
        }
    }
}
