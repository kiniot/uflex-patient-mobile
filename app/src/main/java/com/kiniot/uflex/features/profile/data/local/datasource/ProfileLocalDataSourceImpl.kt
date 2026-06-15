package com.kiniot.uflex.features.profile.data.local.datasource

import com.kiniot.uflex.core.session.SessionStore
import javax.inject.Inject

class ProfileLocalDataSourceImpl @Inject constructor(
    private val sessionStore: SessionStore
) : ProfileLocalDataSource {
    override suspend fun saveEmail(email: String) {
        sessionStore.saveEmail(email)
    }
}
