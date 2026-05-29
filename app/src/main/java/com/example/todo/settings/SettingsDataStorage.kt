package com.example.todo.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val NAME = "todo_settings"
private val Context.dataStore by preferencesDataStore(NAME)

/**
 * Definicje kluczy używanych w DataStore.
 */
object SettingsKeys {
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val LOCATION_ENABLED = booleanPreferencesKey("location_enabled")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val CATEGORIES = stringSetPreferencesKey("categories")
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Repozytorium zarządzające ustawieniami aplikacji przy użyciu DataStore.
 * Zapewnia bezpieczny dostęp do preferencji użytkownika w sposób reaktywny.
 */
private val DEFAULT_CATEGORIES = listOf("Praca", "Osobiste", "Zakupy", "Zdrowie", "Inne")

class SettingsRepository(private val context: Context) {

    /** Strumień stanu powiadomień. */
    val notificationsEnabled = context.dataStore.data
        .map { prefs -> prefs[SettingsKeys.NOTIFICATIONS_ENABLED] ?: true }

    /** Strumień stanu lokalizacji. */
    val locationEnabled = context.dataStore.data
        .map { prefs -> prefs[SettingsKeys.LOCATION_ENABLED] ?: true }

    /** Strumień wybranego trybu motywu. */
    val themeMode = context.dataStore.data
        .map { prefs ->
            when (prefs[SettingsKeys.THEME_MODE]) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        }

    /** * Strumień połączonej listy kategorii (domyślne + użytkownika).
     */
    val categories = context.dataStore.data
        .map { prefs ->
            val stored = prefs[SettingsKeys.CATEGORIES] ?: emptySet()
            val extras = stored.filter { it !in DEFAULT_CATEGORIES }
            DEFAULT_CATEGORIES + extras
        }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setLocationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.LOCATION_ENABLED] = enabled
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.THEME_MODE] = mode.name
        }
    }

    /** Pobiera jednorazowy stan powiadomień. Używane w serwisach w tle. */
    suspend fun isNotificationsEnabled(): Boolean {
        val prefs: Preferences = context.dataStore.data.first()
        return prefs[SettingsKeys.NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun isLocationEnabled(): Boolean {
        val prefs: Preferences = context.dataStore.data.first()
        return prefs[SettingsKeys.LOCATION_ENABLED] ?: true
    }

    suspend fun getThemeMode(): ThemeMode {
        val prefs: Preferences = context.dataStore.data.first()
        return when (prefs[SettingsKeys.THEME_MODE]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    suspend fun addCategory(category: String) {
        if (category.isBlank()) return
        context.dataStore.edit { prefs ->
            val set = prefs[SettingsKeys.CATEGORIES]?.toMutableSet() ?: mutableSetOf()
            set.add(category.trim())
            prefs[SettingsKeys.CATEGORIES] = set
        }
    }

    suspend fun removeCategory(category: String) {
        context.dataStore.edit { prefs ->
            val set = prefs[SettingsKeys.CATEGORIES]?.toMutableSet() ?: mutableSetOf()
            if (set.remove(category)) {
                prefs[SettingsKeys.CATEGORIES] = set
            }
        }
    }
}