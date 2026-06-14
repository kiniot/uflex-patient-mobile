package com.kiniot.uflex.features.auth.navigation

import kotlinx.serialization.Serializable

@Serializable object SignIn
@Serializable object SignUp
@Serializable data class VerifyEmail(val email: String)
