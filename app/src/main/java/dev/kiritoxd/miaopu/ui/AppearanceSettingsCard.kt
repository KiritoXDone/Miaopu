package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
internal fun AppearanceSettingsCard(floatingNavigationBarEnabled: Boolean, onFloatingNavigationBarChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        insideMargin = PaddingValues(0.dp),
        cornerRadius = 20.dp,
    ) {
        SwitchPreference(
            checked = floatingNavigationBarEnabled,
            onCheckedChange = onFloatingNavigationBarChange,
            title = "悬浮底栏",
            summary = "开启后使用悬浮样式的底部导航",
            insideMargin = PaddingValues(18.dp),
        )
    }
}
