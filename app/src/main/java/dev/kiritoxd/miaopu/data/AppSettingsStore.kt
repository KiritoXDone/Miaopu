package dev.kiritoxd.miaopu.data

import android.content.Context

internal class AppSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("miaopu_app_settings", Context.MODE_PRIVATE)

    fun bottomBarStyle(): BottomBarStyle = BottomBarStyle.restore(
        value = preferences.getString(KEY_BOTTOM_BAR_STYLE, null),
        legacyFloating = preferences.getBoolean(KEY_FLOATING_NAVIGATION_BAR, false),
    )

    fun saveBottomBarStyle(style: BottomBarStyle) {
        preferences.edit().putString(KEY_BOTTOM_BAR_STYLE, style.name).apply()
    }

    private companion object {
        const val KEY_FLOATING_NAVIGATION_BAR = "floating_navigation_bar"
        const val KEY_BOTTOM_BAR_STYLE = "bottom_bar_style"
    }
}
