package dev.kiritoxd.miaopu.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/** Copy the original comment, even when its displayed preview is ellipsized. */
@Composable
internal fun Modifier.copyableComment(content: String, onClick: () -> Unit): Modifier {
    val onCopy = commentCopyAction(content) ?: return clickable(onClick = onClick)
    return combinedClickable(
        onClick = onClick,
        onLongClickLabel = "复制评论",
        onLongClick = onCopy,
    )
}

@Composable
internal fun commentCopyAction(content: String): (() -> Unit)? {
    if (content.isBlank()) return null
    val context = LocalContext.current
    return {
        context.getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText("评论", content))
        // Android 13 and newer provide their own clipboard confirmation.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, "评论已复制", Toast.LENGTH_SHORT).show()
        }
    }
}
