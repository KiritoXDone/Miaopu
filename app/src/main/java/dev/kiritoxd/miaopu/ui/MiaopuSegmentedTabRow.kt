package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import top.yukonga.miuix.kmp.basic.TabRowDefaults
import top.yukonga.miuix.kmp.basic.TabRowWithContour
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
    val gap = GlassSegmentedTabRowDefaults.TabGap
    val tabWidth = with(density) { widestLabel.toDp() } + GlassSegmentedTabRowDefaults.TabPaddingHorizontal * 2
    val naturalWidth = tabWidth * labels.size + gap * (labels.size - 1) + GlassSegmentedTabRowDefaults.TrackPadding * 2
    val colors = MiuixTheme.colorScheme
    val indicator = if (colors.background.luminance() < 0.5f) {
        Color.White.copy(alpha = 0.18f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }

    BoxWithConstraints(modifier) {
        // GlassSegmentedTabRow uses horizontal drags to change selection.
        if (constraints.hasBoundedWidth && naturalWidth > maxWidth) {
            TabRowWithContour(
                tabs = labels,
                selectedTabIndex = selected,
                onTabSelected = onSelect,
                minWidth = tabWidth,
                maxWidth = tabWidth,
                height = height,
                itemSpacing = gap,
                cornerRadius = 15.dp,
                colors = TabRowDefaults.tabRowColors(
                    backgroundColor = colors.surfaceContainer,
                    contentColor = colors.onSurfaceVariantSummary,
                    selectedBackgroundColor = indicator,
                    selectedContentColor = colors.onSurface,
                ),
            )
        } else {
            GlassSegmentedTabRow(
                tabs = labels,
                selectedIndex = selected,
                onSelect = onSelect,
                backdrop = UnusedTabBackdrop,
                modifier = Modifier.width(if (constraints.hasBoundedWidth) maxWidth else naturalWidth),
                height = height,
                surfaceAlpha = 0f,
                trackColor = colors.surfaceContainer,
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
