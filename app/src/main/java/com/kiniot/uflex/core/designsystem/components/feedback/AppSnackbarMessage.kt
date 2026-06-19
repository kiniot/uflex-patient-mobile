package com.kiniot.uflex.core.designsystem.components.feedback

import com.kiniot.uflex.core.ui.UiText

data class AppSnackbarMessage(
    val message: UiText,
    val type: SnackbarType = SnackbarType.Info,
    val actionLabel: UiText? = null
)
