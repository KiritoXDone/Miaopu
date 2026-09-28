package dev.kiritoxd.miaopu.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.kiritoxd.miaopu.BuildConfig
import dev.kiritoxd.miaopu.data.HupuCookieSession
import dev.kiritoxd.miaopu.data.HupuUrls
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HupuWebScreen(
    title: String,
    url: String,
    login: Boolean,
    onBack: () -> Unit,
    onLoginDetected: () -> Boolean,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loginHandled by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }

    fun finishLoginIfPossible() {
        if (!loginHandled && onLoginDetected()) loginHandled = true
    }

    fun navigateBack() {
        val current = webView
        if (current?.canGoBack() == true) current.goBack() else onBack()
    }

    val webBackState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = webBackState,
        isBackEnabled = canGoBack,
        onBackCompleted = { webView?.goBack() },
    )

    Scaffold(
        containerColor = MiuixTheme.colorScheme.background,
        topBar = {
            SmallTopAppBar(
                title = title,
                subtitle = if (login) "仅在虎扑官方页面输入账号信息" else "虎扑官方互动页面",
                navigationIcon = {
                    IconButton(onClick = ::navigateBack) {
                        Icon(LucideIcons.ChevronLeft, contentDescription = "返回")
                    }
                },
                actions = {
                    if (login) {
                        TextButton(
                            text = "完成登录",
                            onClick = ::finishLoginIfPossible,
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                        )
                    }
                },
                bottomContent = {
                    if (loading) LinearProgressIndicator()
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.setSupportMultipleWindows(false)
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        settings.userAgentString = "${settings.userAgentString} Miaopu/0.1"
                        val targetWebView = this
                        CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                            setAcceptThirdPartyCookies(targetWebView, true)
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean {
                                if (!request.isForMainFrame) return false
                                return !isTrustedNavigation(request.url)
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                loading = true
                                canGoBack = view?.canGoBack() == true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                loading = false
                                canGoBack = view?.canGoBack() == true
                                if (login && hasLoginCookie(url)) finishLoginIfPossible()
                            }
                        }
                        webView = this
                        loadUrl(url)
                    }
                },
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                stopLoading()
                loadUrl("about:blank")
                clearHistory()
                removeAllViews()
                destroy()
            }
            webView = null
            canGoBack = false
        }
    }
}

private fun isTrustedNavigation(uri: Uri): Boolean = HupuUrls.isTrustedWebUrl(uri.toString())

private fun hasLoginCookie(url: String?): Boolean {
    val cookie = CookieManager.getInstance().getCookie(url ?: "https://hupu.com").orEmpty()
    return HupuCookieSession.isAuthenticated(cookie)
}
