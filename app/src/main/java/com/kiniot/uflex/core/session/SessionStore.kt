package com.kiniot.uflex.core.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

@Singleton
class SessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) {
    private companion object {
        val KEY_USER_ID = stringPreferencesKey("session_user_id")
        val KEY_PATIENT_ID = stringPreferencesKey("session_patient_id")
        val KEY_EMAIL = stringPreferencesKey("session_email")
        val KEY_ROLES = stringPreferencesKey("session_roles")
        val KEY_TENANT_ID = stringPreferencesKey("session_tenant_id")
        val KEY_TOKEN = stringPreferencesKey("session_token")
    }

    suspend fun saveSession(session: LocalSession) {
        dataStore.edit { preferences ->
            preferences[KEY_USER_ID] = session.userId
            if (session.patientId.isNullOrBlank()) {
                preferences.remove(KEY_PATIENT_ID)
            } else {
                preferences[KEY_PATIENT_ID] = session.patientId
            }
            preferences[KEY_EMAIL] = session.email
            preferences[KEY_ROLES] = json.encodeToString(session.roles)
            preferences[KEY_TENANT_ID] = session.tenantId
            preferences[KEY_TOKEN] = session.token
        }
    }

    suspend fun savePatientId(patientId: String) {
        dataStore.edit { preferences ->
            preferences[KEY_PATIENT_ID] = patientId
        }
    }

    suspend fun saveEmail(email: String) {
        dataStore.edit { preferences ->
            preferences[KEY_EMAIL] = email
        }
    }

    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun getToken(): String? {
        return dataStore.data.map { it[KEY_TOKEN] }.first()
    }

    suspend fun isSessionActive(): Boolean {
        return !getToken().isNullOrBlank()
    }

    suspend fun getSession(): LocalSession? {
        val preferences = dataStore.data.first()
        val token = preferences[KEY_TOKEN] ?: return null
        val userId = preferences[KEY_USER_ID] ?: return null
        val patientId = preferences[KEY_PATIENT_ID]
        val email = preferences[KEY_EMAIL] ?: return null
        val tenantId = preferences[KEY_TENANT_ID] ?: return null
        val rolesJson = preferences[KEY_ROLES] ?: "[]"
        val roles = runCatching {
            json.decodeFromString<List<String>>(rolesJson)
        }.getOrDefault(emptyList())

        return LocalSession(
            userId = userId,
            patientId = patientId,
            email = email,
            roles = roles,
            tenantId = tenantId,
            token = token
        )
    }
}
