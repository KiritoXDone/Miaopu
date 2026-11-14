package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRow
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRowDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

// The native control requires a backdrop argument even when its material is disabled.
private object UnusedTabBackdrop : Backdrop {
    override val isCoordinatesDependent = false

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        downscaleFactor: Float,
    ) = Unit
}

@Composable
internal fun MiaopuSegmentedTabRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (labels.isEmpty()) return
    val selected = selectedIndex.coerceIn(labels.indices)
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val selectedStyle = MiuixTheme.textStyles.subtitle
    val unselectedStyle = MiuixTheme.textStyles.body2
    val widestLabel = remember(labels, measurer, density, selectedStyle, unselectedStyle) {
        labels.maxOf { label ->
            val text = AnnotatedString(label)
            maxOf(
                measurer.measure(text, selectedStyle, maxLines = 1, softWrap = false).size.width,
                measurer.measure(text, unselectedStyle, maxLines = 1, softWrap = false).size.width,
            )
        }
    }
    val height = GlassSegmentedTabRowDefaults.Height
    val gap = with(density) { GlassSegmentedTabRowDefaults.TabGap.toPx() }
    val inset = with(density) { GlassSegmentedTabRowDefaults.TrackPadding.toPx() }
    val textPadding = with(density) { GlassSegmentedTabRowDefaults.TabPaddingHorizontal.toPx() }
    val naturalWidth = ceil((widestLabel + textPadding * 2) * labels.size + gap * (labels.size - 1) + inset * 2).toInt()
    val scrollState = rememberScrollState()
    val colors = MiuixTheme.colorScheme
    val indicator = if (colors.background.luminance() < 0.5f) {
        Color.White.copy(alpha = 0.18f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }

    BoxWithConstraints(modifier.fillMaxWidth().height(height)) {
        val viewportWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else naturalWidth
        val contentWidth = maxOf(viewportWidth, naturalWidth)
        val slotWidth = (contentWidth - inset * 2 - gap * (labels.size - 1)) / labels.size
        val center = inset + selected * (slotWidth + gap) + slotWidth / 2
        val target = (center - viewportWidth / 2f).roundToInt().coerceIn(0, contentWidth - viewportWidth)
        LaunchedEffect(selected, viewportWidth, contentWidth, labels, scrollState.maxValue) {
            if (scrollState.maxValue != Int.MAX_VALUE) {
                scrollState.animateScrollTo(target.coerceAtMost(scrollState.maxValue))
            }
        }
        Box(
            Modifier.fillMaxWidth().height(height)
                .clip(GlassSegmentedTabRowDefaults.shape(height))
                .background(colors.surfaceContainer),
        ) {
            Box(Modifier.fillMaxSize().horizontalScroll(scrollState, overscrollEffect = null)) {
                GlassSegmentedTabRow(
                    tabs = labels,
                    selectedIndex = selected,
                    onSelect = onSelect,
                    backdrop = UnusedTabBackdrop,
                    modifier = Modifier.width(with(density) { contentWidth.toDp() }),
                    height = height,
                    surfaceAlpha = 0f,
                    trackColor = Color.Transparent,
                    material = null,
                    stroke = null,
                    shadow = null,
                    indicatorColor = indicator,
                    selectedContentColor = colors.onSurface,
                    contentColor = colors.onSurfaceVariantSummary,
                )
            }
        }
    }
}
