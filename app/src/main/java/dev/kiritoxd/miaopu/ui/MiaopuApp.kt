package dev.kiritoxd.miaopu.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun MiaopuApp(viewModel: MiaopuViewModel) {
    val backStack = rememberNavBackStack<AppScreen>(AppScreen.Schedule)
    val navigator = remember(backStack) { MiaopuNavigator(backStack) }
    DisposableEffect(viewModel, navigator) {
        viewModel.attachNavigator(navigator)
        onDispose { viewModel.detachNavigator(navigator) }
    }
    val themeController = remember { ThemeController(ColorSchemeMode.System) }
    MiuixTheme(controller = themeController) {
        Box(Modifier.fillMaxSize()) {
            val cornerRadius = rememberNavSystemCornerRadius()
            val backdropColor = MiuixTheme.colorScheme.surface
            val navEffects = remember(cornerRadius, backdropColor) {
                NavDisplayEffects(
                    enableCornerClip = false,
                    cornerClipRadius = cornerRadius,
                    backdropColor = backdropColor,
                )
            }
            val navigationMotion = rememberMiaopuNavigationMotion(cornerRadius)
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.fillMaxSize(),
                onBack = viewModel::goBack,
                transition = navigationMotion,
                effects = navEffects,
            ) {
                entry<AppScreen.Schedule>(contentKey = { it.navigationContentKey }) { ScheduleScreen(viewModel) }
                entry<AppScreen.Subscriptions>(contentKey = { it.navigationContentKey }) { SubscriptionScreen(viewModel) }
                entry<AppScreen.Ratings>(contentKey = { it.navigationContentKey }) { screen ->
                    RatingsScreen(viewModel, screen.match.toModel())
                }
                entry<AppScreen.Stage>(contentKey = { it.navigationContentKey }) { screen ->
                    MatchStageScreen(
                        viewModel = viewModel,
                        match = screen.match.toModel(),
                        stage = screen.stage.toModel(),
                        stageNumber = screen.stageNumber,
                        showStageNumber = screen.returnToStagePicker,
                    )
                }
                entry<AppScreen.Comments>(contentKey = { it.navigationContentKey }) { screen ->
                    CommentsScreen(viewModel, screen.target.toModel())
                }
                entry<AppScreen.Web>(contentKey = { it.navigationContentKey }) { screen ->
                    HupuWebScreen(
                        title = screen.title,
                        url = screen.url,
                        login = screen.login,
                        onBack = viewModel::goBack,
                        onLoginDetected = viewModel::finishLogin,
                    )
                }
            }

            AnimatedVisibility(
                visible = viewModel.message != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .padding(16.dp),
            ) {
                Card(
                    onClick = viewModel::dismissMessage,
                    showIndication = true,
                    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    cornerRadius = 18.dp,
                ) {
                    Text(text = viewModel.message.orEmpty())
                }
            }
        }
    }
}
