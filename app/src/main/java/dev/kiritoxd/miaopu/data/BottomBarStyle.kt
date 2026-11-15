package dev.kiritoxd.miaopu.data

internal enum class BottomBarStyle {
    CLASSIC,
    FLOATING,
    FLOATING_ICON,
    LIQUID_GLASS,
    OS4_GLASS;

    companion object {
        fun restore(value: String?, legacyFloating: Boolean): BottomBarStyle =
            entries.firstOrNull { it.name == value }
                ?: if (legacyFloating) FLOATING_ICON else CLASSIC
    }
}
