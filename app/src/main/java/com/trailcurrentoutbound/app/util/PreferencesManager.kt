package com.trailcurrentoutbound.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "trailcurrentoutbound_settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Server Configuration
        val SERVER_URL = stringPreferencesKey("server_url")
        val WEBSOCKET_URL = stringPreferencesKey("websocket_url")
        // API Key Authentication
        val API_KEY = stringPreferencesKey("api_key")
        // App Settings
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val TIMEZONE = stringPreferencesKey("timezone")
        val CLOCK_FORMAT = stringPreferencesKey("clock_format")
        // Defaults
        const val DEFAULT_SERVER_URL = "https://trailcurrentoutbound.example.com"
    }

    // Server URL
    val serverUrlFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[SERVER_URL] ?: DEFAULT_SERVER_URL }

    suspend fun getServerUrl(): String {
        return context.dataStore.data.first()[SERVER_URL] ?: DEFAULT_SERVER_URL
    }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_URL] = url
        }
    }

    // WebSocket URL (derived from server URL)
    suspend fun getWebSocketUrl(): String {
        val serverUrl = getServerUrl()
        return if (serverUrl.startsWith("https://")) {
            serverUrl.replace("https://", "wss://") + "/ws"
        } else {
            serverUrl.replace("http://", "ws://") + "/ws"
        }
    }

    // API Key
    val apiKeyFlow: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[API_KEY] }

    suspend fun getApiKey(): String? {
        return context.dataStore.data.first()[API_KEY]
    }

    suspend fun setApiKey(apiKey: String?) {
        context.dataStore.edit { preferences ->
            if (apiKey != null) {
                preferences[API_KEY] = apiKey
            } else {
                preferences.remove(API_KEY)
            }
        }
    }

    fun hasApiKey(): Flow<Boolean> = context.dataStore.data
        .map { preferences -> !preferences[API_KEY].isNullOrBlank() }

    // Dark Mode
    val darkModeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[DARK_MODE] ?: false }

    suspend fun getDarkMode(): Boolean {
        return context.dataStore.data.first()[DARK_MODE] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE] = enabled
        }
    }

    // Timezone
    val timezoneFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[TIMEZONE] ?: "America/New_York" }

    suspend fun getTimezone(): String {
        return context.dataStore.data.first()[TIMEZONE] ?: "America/New_York"
    }

    suspend fun setTimezone(timezone: String) {
        context.dataStore.edit { preferences ->
            preferences[TIMEZONE] = timezone
        }
    }

    // Clock Format
    val clockFormatFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[CLOCK_FORMAT] ?: "12h" }

    suspend fun getClockFormat(): String {
        return context.dataStore.data.first()[CLOCK_FORMAT] ?: "12h"
    }

    suspend fun setClockFormat(format: String) {
        context.dataStore.edit { preferences ->
            preferences[CLOCK_FORMAT] = format
        }
    }

    // Check if configured (has server URL set)
    val isConfiguredFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            val serverUrl = preferences[SERVER_URL]
            !serverUrl.isNullOrBlank() && serverUrl != DEFAULT_SERVER_URL
        }

    suspend fun isConfigured(): Boolean {
        val serverUrl = context.dataStore.data.first()[SERVER_URL]
        return !serverUrl.isNullOrBlank() && serverUrl != DEFAULT_SERVER_URL
    }
}
