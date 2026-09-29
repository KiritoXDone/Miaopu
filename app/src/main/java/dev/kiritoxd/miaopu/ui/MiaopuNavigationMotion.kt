package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitionScope
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import androidx.compose.ui.Modifier

/** Keep miuix gesture ownership and settle physics; clip in the existing motion layer. */
@Composable
internal fun rememberMiaopuNavigationMotion(cornerRadius: Dp): NavTransition = remember(cornerRadius) {
    val leadingCorners = RoundedCornerShape(topStart = cornerRadius, bottomStart = cornerRadius)
    object : NavTransition by NavTransitions.MiuixDefault {
        override fun Modifier.transformEntry(scope: NavTransitionScope): Modifier = graphicsLayer {
            val depth = scope.relativeDepth
            translationX = navigationTranslationX(depth, scope.layoutSize.width.toFloat(), scope.layoutDirection == LayoutDirection.Rtl)
            alpha = 1f - 0.1f * depth.coerceIn(0f, 1f)
            shape = leadingCorners
            clip = depth > -1f && depth < 0f
        }
    }
}

internal fun navigationTranslationX(depth: Float, width: Float, rtl: Boolean): Float {
    val translation = if (depth <= 0f) (-depth).coerceIn(0f, 1f) * width
        else -depth.coerceIn(0f, 1f) * width * 0.25f
    return if (rtl) -translation else translation
}
