package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.util.Constants

class PreferencesHelper(private val context: Context) {

    private val sharedPrefs: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "hifz_guard_secure_preferences",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Throwable) {
            Log.e("PreferencesHelper", "Failed to create EncryptedSharedPreferences, falling back to standard", e)
            context.getSharedPreferences("hifz_guard_fallback_preferences", Context.MODE_PRIVATE)
        }
    }

    private companion object {
        const val KEY_DAILY_GOAL = "daily_goal_minutes"
        const val KEY_LOCK_THRESHOLD_HOUR = "lock_threshold_hour"
        const val KEY_LOCK_THRESHOLD_MINUTE = "lock_threshold_minute"
        const val KEY_ACCOUNTABILITY_PHONE = "accountability_phone_number"
        const val KEY_CONSEQUENCE_ENABLED = "consequence_enabled"
        const val KEY_CONSEQUENCE_MESSAGE_TEMPLATE = "consequence_message_template"
        const val KEY_EMERGENCY_OVERRIDE_UNTIL = "emergency_override_until"
        const val KEY_JUZ_MEMORIZED_COUNT = "juz_memorized_count"
        const val KEY_STREAK_COUNT = "streak_count"
        const val KEY_LAST_STREAK_DATE = "last_streak_date"
        const val KEY_TOTAL_LIFETIME_MINUTES = "total_lifetime_minutes"
        const val KEY_BEST_STREAK = "best_streak"
        const val KEY_COMMITTED_TARGET_MINUTES = "committed_target_minutes"
        const val KEY_TEMPORARY_UNLOCK_UNTIL = "temporary_unlock_until"
    }

    var dailyGoalMinutes: Int
        get() = sharedPrefs.getInt(KEY_DAILY_GOAL, Constants.DEFAULT_GOAL_MINUTES)
        set(value) = sharedPrefs.edit().putInt(KEY_DAILY_GOAL, value).apply()

    var committedTargetMinutes: Int
        get() = sharedPrefs.getInt(KEY_COMMITTED_TARGET_MINUTES, 0)
        set(value) = sharedPrefs.edit().putInt(KEY_COMMITTED_TARGET_MINUTES, value).apply()

    var temporaryUnlockUntil: Long
        get() = sharedPrefs.getLong(KEY_TEMPORARY_UNLOCK_UNTIL, 0L)
        set(value) = sharedPrefs.edit().putLong(KEY_TEMPORARY_UNLOCK_UNTIL, value).apply()

    var lockThresholdHour: Int
        get() = sharedPrefs.getInt(KEY_LOCK_THRESHOLD_HOUR, Constants.DEFAULT_LOCK_HOUR)
        set(value) = sharedPrefs.edit().putInt(KEY_LOCK_THRESHOLD_HOUR, value).apply()

    var lockThresholdMinute: Int
        get() = sharedPrefs.getInt(KEY_LOCK_THRESHOLD_MINUTE, Constants.DEFAULT_LOCK_MINUTE)
        set(value) = sharedPrefs.edit().putInt(KEY_LOCK_THRESHOLD_MINUTE, value).apply()

    var accountabilityPhone: String
        get() = sharedPrefs.getString(KEY_ACCOUNTABILITY_PHONE, "") ?: ""
        set(value) = sharedPrefs.edit().putString(KEY_ACCOUNTABILITY_PHONE, value).apply()

    var consequenceEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_CONSEQUENCE_ENABLED, false)
        set(value) = sharedPrefs.edit().putBoolean(KEY_CONSEQUENCE_ENABLED, value).apply()

    var consequenceMessageTemplate: String
        get() = sharedPrefs.getString(KEY_CONSEQUENCE_MESSAGE_TEMPLATE, "Assalamu Alaikum. I missed my daily Quran goal today. Please remind me to do better tomorrow. - Sent by HifzGuard") ?: ""
        set(value) = sharedPrefs.edit().putString(KEY_CONSEQUENCE_MESSAGE_TEMPLATE, value).apply()

    var emergencyOverrideUntil: Long
        get() = sharedPrefs.getLong(KEY_EMERGENCY_OVERRIDE_UNTIL, 0L)
        set(value) = sharedPrefs.edit().putLong(KEY_EMERGENCY_OVERRIDE_UNTIL, value).apply()

    var streakCount: Int
        get() = sharedPrefs.getInt(KEY_STREAK_COUNT, 0)
        set(value) = sharedPrefs.edit().putInt(KEY_STREAK_COUNT, value).apply()

    var bestStreak: Int
        get() = sharedPrefs.getInt(KEY_BEST_STREAK, 0)
        set(value) = sharedPrefs.edit().putInt(KEY_BEST_STREAK, value).apply()

    var lastStreakDate: String
        get() = sharedPrefs.getString(KEY_LAST_STREAK_DATE, "") ?: ""
        set(value) = sharedPrefs.edit().putString(KEY_LAST_STREAK_DATE, value).apply()

    var totalLifetimeMinutes: Int
        get() = sharedPrefs.getInt(KEY_TOTAL_LIFETIME_MINUTES, 0)
        set(value) = sharedPrefs.edit().putInt(KEY_TOTAL_LIFETIME_MINUTES, value).apply()

    fun isEmergencyOverrideActive(): Boolean {
        return System.currentTimeMillis() < emergencyOverrideUntil
    }

    fun isTemporaryUnlockActive(): Boolean {
        return System.currentTimeMillis() < temporaryUnlockUntil
    }

    fun triggerEmergencyOverride() {
        emergencyOverrideUntil = System.currentTimeMillis() + (30 * 60 * 1000) // 30 mins
        committedTargetMinutes = 0 // Also clear any stuck committed target
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
