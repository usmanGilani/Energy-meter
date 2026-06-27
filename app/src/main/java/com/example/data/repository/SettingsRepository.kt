package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "energy_settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val GOOGLE_APPS_SCRIPT_URL = stringPreferencesKey("google_apps_script_url")
        val ELECTRICITY_TARIFF = floatPreferencesKey("electricity_tariff")
        val REFRESH_INTERVAL_SEC = intPreferencesKey("refresh_interval_sec")
        val VOLTAGE_MIN = floatPreferencesKey("voltage_min")
        val VOLTAGE_MAX = floatPreferencesKey("voltage_max")
        val CURRENT_MAX = floatPreferencesKey("current_max")
        val POWER_MAX = floatPreferencesKey("power_max")
        val PF_MIN = floatPreferencesKey("pf_min")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val googleAppsScriptUrl: Flow<String> = context.dataStore.data.map { it[GOOGLE_APPS_SCRIPT_URL] ?: "https://docs.google.com/spreadsheets/d/10_5kvSRtBEp3vTJ3A-z-i5jBqHP3abA3Rx2jKHmQxkk/edit?usp=drivesdk" }
    val electricityTariff: Flow<Float> = context.dataStore.data.map { it[ELECTRICITY_TARIFF] ?: 0.15f }
    val refreshIntervalSec: Flow<Int> = context.dataStore.data.map { it[REFRESH_INTERVAL_SEC] ?: 5 }
    val voltageMin: Flow<Float> = context.dataStore.data.map { it[VOLTAGE_MIN] ?: 210f }
    val voltageMax: Flow<Float> = context.dataStore.data.map { it[VOLTAGE_MAX] ?: 250f }
    val currentMax: Flow<Float> = context.dataStore.data.map { it[CURRENT_MAX] ?: 15.0f }
    val powerMax: Flow<Float> = context.dataStore.data.map { it[POWER_MAX] ?: 3000f }
    val pfMin: Flow<Float> = context.dataStore.data.map { it[PF_MIN] ?: 0.85f }
    val themeMode: Flow<String> = context.dataStore.data.map { it[THEME_MODE] ?: "LIGHT" }

    suspend fun setGoogleAppsScriptUrl(url: String) {
        context.dataStore.edit { it[GOOGLE_APPS_SCRIPT_URL] = url }
    }

    suspend fun setElectricityTariff(tariff: Float) {
        context.dataStore.edit { it[ELECTRICITY_TARIFF] = tariff }
    }

    suspend fun setRefreshIntervalSec(interval: Int) {
        context.dataStore.edit { it[REFRESH_INTERVAL_SEC] = interval }
    }

    suspend fun setVoltageLimits(min: Float, max: Float) {
        context.dataStore.edit {
            it[VOLTAGE_MIN] = min
            it[VOLTAGE_MAX] = max
        }
    }

    suspend fun setCurrentMax(max: Float) {
        context.dataStore.edit { it[CURRENT_MAX] = max }
    }

    suspend fun setPowerMax(max: Float) {
        context.dataStore.edit { it[POWER_MAX] = max }
    }

    suspend fun setPfMin(min: Float) {
        context.dataStore.edit { it[PF_MIN] = min }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }
}
