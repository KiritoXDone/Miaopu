package dev.kiritoxd.miaopu.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.glass.GlassIconButton

/** A solid-color OS4 action for use outside GlassTopAppBar. */
@Composable
internal fun TopBarIconButton(
    onClick: () -> Unit,
    fill: Color = Color.Transparent,
    content: @Composable () -> Unit,
) {
    GlassIconButton(
        onClick = onClick,
        modifier = Modifier.semantics { role = Role.Button },
        inheritTopBarMaterial = false,
        backdrop = null,
        size = 40.dp,
        fill = fill,
        stroke = null,
        shadow = null,
        content = content,
    )
}
