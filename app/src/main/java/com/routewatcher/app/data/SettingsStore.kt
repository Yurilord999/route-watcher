package com.routewatcher.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.Calendar

// snapshot of this months Routes API usage against the free tier limit (5k)
data class RoutesApiUsage(val count: Int, val limit: Int)

// Stores the users personal Google Maps API key locally.
// Encrypted with a key held in the Android Keystore. Nothing here should leave the device.
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            "routewatcher_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun getApiKey(): String? = prefs.getString(KEY_API_KEY, null)

    fun setApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key.trim()).apply()
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply()
    }

    fun hasSeenOnboarding(): Boolean = prefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false)

    fun setHasSeenOnboarding(seen: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_SEEN_ONBOARDING, seen).apply()
    }

    fun getRoutesApiUsage(): RoutesApiUsage = RoutesApiUsage(count = currentMonthCount(), limit = ROUTES_API_MONTHLY_LIMIT)

    // Routes API usage gate
    fun tryConsumeRoutesApiCall(): Boolean {
        val count = currentMonthCount()
        if (count >= ROUTES_API_MONTHLY_LIMIT) return false
        prefs.edit().putInt(KEY_ROUTES_API_CALL_COUNT, count + 1).apply()
        return true
    }

    // Resets the counter to zero the first time it's touched in a new calendar month
    private fun currentMonthCount(): Int {
        val nowMonth = currentMonthKey()
        val storedMonth = prefs.getString(KEY_ROUTES_API_CALL_MONTH, null)
        if (storedMonth != nowMonth) {
            prefs.edit()
                .putString(KEY_ROUTES_API_CALL_MONTH, nowMonth)
                .putInt(KEY_ROUTES_API_CALL_COUNT, 0)
                .apply()
            return 0
        }
        return prefs.getInt(KEY_ROUTES_API_CALL_COUNT, 0)
    }

    private fun currentMonthKey(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}"
    }
    companion object {
        private const val KEY_API_KEY = "google_maps_api_key"
        private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"
        private const val KEY_ROUTES_API_CALL_COUNT = "routes_api_call_count"
        private const val KEY_ROUTES_API_CALL_MONTH = "routes_api_call_month"

        // Googles real free tier is 5000/month
        const val ROUTES_API_MONTHLY_LIMIT = 4900
    }
}