package com.kiniot.uflex.core.designsystem.components.feedback

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.kiniot.uflex.core.ui.asString

class UFlexSnackbarState(
    val hostState: SnackbarHostState,
    internal val currentType: MutableState<SnackbarType>
) {
    suspend fun showMessage(
        context: Context,
        message: AppSnackbarMessage
    ) {
        currentType.value = message.type
        hostState.showSnackbar(
            message = message.message.asString(context),
            actionLabel = message.actionLabel?.asString(context),
            duration = SnackbarDuration.Short
        )
    }
}

@Composable
fun rememberUFlexSnackbarState(): UFlexSnackbarState {
    val hostState = remember { SnackbarHostState() }
    val currentType = remember { mutableStateOf(SnackbarType.Info) }
    return remember { UFlexSnackbarState(hostState, currentType) }
}
