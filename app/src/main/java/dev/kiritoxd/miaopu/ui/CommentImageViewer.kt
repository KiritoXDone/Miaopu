package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import android.os.Build
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import dev.kiritoxd.miaopu.data.CommentImageSaveResult
import dev.kiritoxd.miaopu.data.saveCommentImage
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

private sealed interface ImageSaveDialogState {
    data object Confirm : ImageSaveDialogState
    data object Saving : ImageSaveDialogState
    data class Saved(val location: String) : ImageSaveDialogState
    data class Failed(val message: String) : ImageSaveDialogState
}

@Composable
internal fun CommentImages(imageUrls: List<String>) {
    if (imageUrls.isEmpty()) return
    var previewIndex by remember(imageUrls) { mutableStateOf<Int?>(null) }
    Spacer(Modifier.height(10.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(
            items = imageUrls,
            key = { index, imageUrl -> "$index-$imageUrl" },
            contentType = { _, _ -> "comment-image" },
        ) { index, imageUrl ->
            AsyncImage(
                model = imageUrl,
                contentDescription = "查看评论图片 ${index + 1}/${imageUrls.size}",
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MiuixTheme.colorScheme.surfaceVariant)
                    .clickable(role = Role.Button) { previewIndex = index },
                contentScale = ContentScale.Crop,
            )
        }
    }
    previewIndex?.let { index ->
        CommentImageViewer(
            imageUrls = imageUrls,
            initialIndex = index,
            onDismiss = { previewIndex = null },
        )
    }
}

@Composable
internal fun CommentImageViewer(
    imageUrls: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    if (imageUrls.isEmpty()) return
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(imageUrls.indices),
        pageCount = { imageUrls.size },
    )
    var zoomedPage by remember { mutableStateOf<Int?>(null) }
    var imageToSave by remember { mutableStateOf<String?>(null) }
    var saveDialogState by remember { mutableStateOf<ImageSaveDialogState>(ImageSaveDialogState.Confirm) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun dismissSaveDialog() {
        if (saveDialogState !is ImageSaveDialogState.Saving) imageToSave = null
    }

    fun startSaving() {
        val imageUrl = imageToSave ?: return
        saveDialogState = ImageSaveDialogState.Saving
        scope.launch {
            saveDialogState = when (val result = saveCommentImage(context, imageUrl)) {
                is CommentImageSaveResult.Saved -> ImageSaveDialogState.Saved(result.location)
                is CommentImageSaveResult.Failed -> ImageSaveDialogState.Failed(result.message)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val dialogView = LocalView.current
        SideEffect {
            (dialogView.parent as? DialogWindowProvider)?.window?.let { dialogWindow ->
                @Suppress("DEPRECATION")
                dialogWindow.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= 28) dialogWindow.navigationBarDividerColor = android.graphics.Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= 29) dialogWindow.isNavigationBarContrastEnforced = false
                WindowCompat.getInsetsController(dialogWindow, dialogView).isAppearanceLightNavigationBars = false
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = zoomedPage == null,
                key = { "$it-${imageUrls[it]}" },
            ) { page ->
                ZoomableCommentImage(
                    imageUrl = imageUrls[page],
                    onZoomChanged = { zoomed ->
                        zoomedPage = if (zoomed) page else null
                    },
                    onSaveRequest = {
                        imageToSave = imageUrls[page]
                        saveDialogState = ImageSaveDialogState.Confirm
                    },
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(16.dp),
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopStart),
                    backgroundColor = Color.Black.copy(alpha = 0.45f),
                    minWidth = 48.dp,
                    minHeight = 48.dp,
                ) {
                    Icon(
                        imageVector = LucideIcons.X,
                        contentDescription = "关闭图片预览",
                        tint = Color.White,
                    )
                }
                Text(
                    text = "${pagerState.currentPage + 1} / ${imageUrls.size}",
                    modifier = Modifier.align(Alignment.TopCenter),
                    style = MiuixTheme.textStyles.body1,
                    color = Color.White,
                )
                if (imageUrls.size > 1) {
                    Text(
                        text = "左右滑动",
                        modifier = Modifier.align(Alignment.BottomCenter),
                        style = MiuixTheme.textStyles.footnote1,
                        color = Color.White.copy(alpha = 0.72f),
                    )
                }
            }
        }
    }

    WindowDialog(
        show = imageToSave != null,
        title = "保存图片",
        summary = when (val state = saveDialogState) {
            ImageSaveDialogState.Confirm -> "确认将当前原图保存到设备吗？"
            ImageSaveDialogState.Saving -> "正在下载并保存原图…"
            is ImageSaveDialogState.Saved -> "已保存到${state.location}"
            is ImageSaveDialogState.Failed -> state.message
        },
        onDismissRequest = ::dismissSaveDialog,
    ) {
        when (saveDialogState) {
            ImageSaveDialogState.Confirm -> ImageSaveActions(
                confirmText = "保存",
                onCancel = ::dismissSaveDialog,
                onConfirm = ::startSaving,
            )
            ImageSaveDialogState.Saving -> Unit
            is ImageSaveDialogState.Saved -> Button(
                onClick = ::dismissSaveDialog,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text("完成")
            }
            is ImageSaveDialogState.Failed -> ImageSaveActions(
                confirmText = "重试",
                onCancel = ::dismissSaveDialog,
                onConfirm = ::startSaving,
            )
        }
    }
}

@Composable
private fun ImageSaveActions(
    confirmText: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(
            text = "取消",
            onClick = onCancel,
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColorsPrimary(),
        ) {
            Text(confirmText)
        }
    }
}

@Composable
private fun ZoomableCommentImage(
    imageUrl: String,
    onZoomChanged: (Boolean) -> Unit,
    onSaveRequest: () -> Unit,
) {
    var scale by remember(imageUrl) { mutableStateOf(1f) }
    var offset by remember(imageUrl) { mutableStateOf(Offset.Zero) }
    var viewportSize by remember(imageUrl) { mutableStateOf(IntSize.Zero) }
    val latestScale by rememberUpdatedState(scale)
    val latestSaveRequest by rememberUpdatedState(onSaveRequest)
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
        offset = if (nextScale == 1f) {
            Offset.Zero
        } else {
            val maxX = viewportSize.width * (nextScale - 1f) / 2f
            val maxY = viewportSize.height * (nextScale - 1f) / 2f
            Offset(
                x = (offset.x + panChange.x).coerceIn(-maxX, maxX),
                y = (offset.y + panChange.y).coerceIn(-maxY, maxY),
            )
        }
        scale = nextScale
        onZoomChanged(nextScale > 1f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewportSize = it }
            .semantics {
                onLongClick(label = "保存图片") {
                    latestSaveRequest()
                    true
                }
            }
            .pointerInput(imageUrl) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (latestScale > 1f) 1f else 2.5f
                        offset = Offset.Zero
                        onZoomChanged(scale > 1f)
                    },
                    onLongPress = { latestSaveRequest() },
                )
            }
            .transformable(
                state = transformState,
                enabled = scale > 1f,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "评论图片大图",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            contentScale = ContentScale.Fit,
        )
    }
}
