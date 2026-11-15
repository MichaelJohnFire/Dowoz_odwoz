package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("bus_attendance_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_ACTIVE_GUARDIAN_ID = "key_active_guardian_id"
        private const val KEY_ACTIVE_GUARDIAN_NAME = "key_active_guardian_name"
        private const val KEY_ROUTES_INITIALIZED = "key_routes_initialized"
        private const val KEY_DARK_MODE = "key_dark_mode"
    }

    fun isDarkMode(): Boolean? {
        if (!prefs.contains(KEY_DARK_MODE)) return null
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkMode(isDark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, isDark).apply()
    }

    fun areRoutesInitialized(): Boolean {
        return prefs.getBoolean(KEY_ROUTES_INITIALIZED, false)
    }

    fun setRoutesInitialized(initialized: Boolean) {
        prefs.edit().putBoolean(KEY_ROUTES_INITIALIZED, initialized).apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false) &&
                !getActiveGuardianName().isNullOrBlank()
    }

    fun getActiveGuardianId(): String? {
        return prefs.getString(KEY_ACTIVE_GUARDIAN_ID, null)
    }

    fun getActiveGuardianName(): String? {
        return prefs.getString(KEY_ACTIVE_GUARDIAN_NAME, null)
    }

    fun setActiveGuardian(id: String, fullName: String) {
        prefs.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .putString(KEY_ACTIVE_GUARDIAN_ID, id)
            .putString(KEY_ACTIVE_GUARDIAN_NAME, fullName.trim())
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
