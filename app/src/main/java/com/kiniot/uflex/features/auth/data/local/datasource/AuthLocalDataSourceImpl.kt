package com.kiniot.uflex.features.auth.data.local.datasource

import com.kiniot.uflex.core.session.LocalSession
import com.kiniot.uflex.core.session.SessionStore
import javax.inject.Inject

class AuthLocalDataSourceImpl @Inject constructor(
    private val sessionStore: SessionStore
) : AuthLocalDataSource {
    override suspend fun saveSession(session: LocalSession) {
        sessionStore.saveSession(session)
    }

    override suspend fun isSessionActive(): Boolean {
        return sessionStore.isSessionActive()
    }

    override suspend fun clearSession() {
        sessionStore.clearSession()
    }
}
