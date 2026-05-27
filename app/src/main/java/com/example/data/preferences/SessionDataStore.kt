package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hifzguard_session_datastore")

class SessionDataStore(private val context: Context) {

    companion object {
        private val KEY_ACCUMULATED_SECONDS = intPreferencesKey("accumulated_seconds")
        private val KEY_LAST_TRACKED_DATE = stringPreferencesKey("last_tracked_date")
        private val KEY_YESTERDAY_DEBT_MINUTES = intPreferencesKey("yesterday_debt_minutes")
        private val KEY_OVERLAY_ACTIVE_MANUAL = booleanPreferencesKey("overlay_active_manual")
    }

    val accumulatedSecondsFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_ACCUMULATED_SECONDS] ?: 0
    }

    val yesterdayDebtMinutesFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_YESTERDAY_DEBT_MINUTES] ?: 0
    }

    val lastTrackedDateFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_TRACKED_DATE] ?: ""
    }

    suspend fun setAccumulatedSeconds(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ACCUMULATED_SECONDS] = seconds
        }
    }

    suspend fun setYesterdayDebtMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_YESTERDAY_DEBT_MINUTES] = minutes
        }
    }

    suspend fun setLastTrackedDate(dateStr: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_TRACKED_DATE] = dateStr
        }
    }

    suspend fun checkDailyReset() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        context.dataStore.edit { preferences ->
            val lastDate = preferences[KEY_LAST_TRACKED_DATE] ?: ""
            if (lastDate.isNotEmpty() && lastDate != today) {
                // It is a new day! Calculate debt
                val goalMin = context.getSharedPreferences("hifz_guard_secure_preferences", Context.MODE_PRIVATE)
                    .getInt("daily_goal_minutes", 60)
                val currentSeconds = preferences[KEY_ACCUMULATED_SECONDS] ?: 0
                val currentMinutes = currentSeconds / 60
                
                if (currentMinutes < goalMin) {
                    val missedMinutes = goalMin - currentMinutes
                    val existingDebt = preferences[KEY_YESTERDAY_DEBT_MINUTES] ?: 0
                    preferences[KEY_YESTERDAY_DEBT_MINUTES] = existingDebt + missedMinutes
                } else {
                    // Reset debt or keep debt (if today's goal is met, let's decrement debt structure or reset)
                    // Let's keep it clean
                }
                
                // Reset seconds
                preferences[KEY_ACCUMULATED_SECONDS] = 0
            }
            preferences[KEY_LAST_TRACKED_DATE] = today
        }
    }

    suspend fun incrementAccumulatedSeconds(deltaSeconds: Int) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_ACCUMULATED_SECONDS] ?: 0
            preferences[KEY_ACCUMULATED_SECONDS] = current + deltaSeconds
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences[KEY_ACCUMULATED_SECONDS] = 0
        }
    }
}
