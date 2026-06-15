package com.kiniot.uflex.features.auth.data.local.datasource

import com.kiniot.uflex.core.session.LocalSession

interface AuthLocalDataSource {
    suspend fun saveSession(session: LocalSession)
    suspend fun isSessionActive(): Boolean
    suspend fun clearSession()
}
