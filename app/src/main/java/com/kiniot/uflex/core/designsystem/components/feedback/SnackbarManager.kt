package com.kiniot.uflex.core.designsystem.components.feedback

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class SnackbarManager @Inject constructor() {
    private val _messages = MutableSharedFlow<AppSnackbarMessage>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messages: SharedFlow<AppSnackbarMessage> = _messages.asSharedFlow()

    suspend fun showMessage(message: AppSnackbarMessage) {
        _messages.emit(message)
    }
}
