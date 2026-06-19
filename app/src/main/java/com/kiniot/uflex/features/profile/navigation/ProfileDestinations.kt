package com.kiniot.uflex.features.profile.navigation

import kotlinx.serialization.Serializable

@Serializable object ProfileRoute
@Serializable
data class EditContactInfoRoute(
    val email: String,
    val countryCode: String,
    val phoneNumber: String
)
