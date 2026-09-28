package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Shared compact composer matching the comment and reply reference. */
@Composable
internal fun CommentComposer(
    value: String,
    placeholder: String,
    action: String,
    inputEnabled: Boolean,
    actionEnabled: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    focusKey: String? = null,
) {
    val colors = MiuixTheme.colorScheme
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(focusKey) {
        if (focusKey != null && inputEnabled) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
    Row(
        Modifier.fillMaxWidth().background(colors.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = value, onValueChange = onValueChange, enabled = inputEnabled,
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
            textStyle = TextStyle(color = colors.onSurface, fontSize = 14.sp, lineHeight = 20.sp),
            cursorBrush = SolidColor(colors.primary), minLines = 1, maxLines = 4,
            decorationBox = { field ->
                Box(
                    Modifier.fillMaxWidth().background(colors.onSurface.copy(alpha = 0.055f), RoundedCornerShape(22.dp))
                        .heightIn(min = 44.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) Text(placeholder, fontSize = 14.sp, lineHeight = 20.sp,
                        color = colors.onSurfaceVariantSummary, maxLines = 1)
                    field()
                }
            },
        )
        Box(
            Modifier.widthIn(min = 40.dp).heightIn(min = 44.dp)
                .clickable(enabled = actionEnabled, role = Role.Button, onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            Text(action, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = colors.primary.copy(alpha = if (actionEnabled) 1f else 0.45f))
        }
    }
}
