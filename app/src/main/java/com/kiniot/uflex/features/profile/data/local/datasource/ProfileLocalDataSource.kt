package com.kiniot.uflex.features.profile.data.local.datasource

interface ProfileLocalDataSource {
    suspend fun saveEmail(email: String)
}
