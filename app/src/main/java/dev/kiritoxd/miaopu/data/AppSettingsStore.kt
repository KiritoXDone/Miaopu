package dev.kiritoxd.miaopu.data

import android.content.Context

internal class AppSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("miaopu_app_settings", Context.MODE_PRIVATE)

    fun floatingNavigationBarEnabled(): Boolean = preferences.getBoolean(KEY_FLOATING_NAVIGATION_BAR, false)

    fun saveFloatingNavigationBarEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_FLOATING_NAVIGATION_BAR, enabled).apply()
    }

    private companion object {
        const val KEY_FLOATING_NAVIGATION_BAR = "floating_navigation_bar"
    }
}
