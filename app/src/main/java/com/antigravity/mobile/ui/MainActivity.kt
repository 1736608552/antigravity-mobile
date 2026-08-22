package com.antigravity.mobile.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.antigravity.mobile.network.TokenManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// ============================================================
// 主 Activity (WebView 封装方案)
// 直接加载 Antigravity Web 端，UI 和功能与 Web 端完全一致
// ============================================================

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var rootContainer: FrameLayout
    private val tokenManager by lazy { TokenManager(applicationContext) }

    private var filePathCallback: android.webkit.ValueCallback<Array<Uri>?>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris?.toTypedArray())
        filePathCallback = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 开启 edge-to-edge 布局，由 WindowInsets 统一控制 Padding 避免挡住任务栏和输入框
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val targetUrl = "http://fn0.xx.kg:3100"
        val token = runBlocking { tokenManager.getToken() } ?: ""
        showWebView(targetUrl, token)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showWebView(baseUrl: String, token: String) {
        webView = WebView(this)

        // --- WebView 配置 ---
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            allowUniversalAccessFromFileURLs = true
        }

        // WebView 背景设为与网页一致的深色，加载期间不闪黑
        webView.setBackgroundColor(Color.parseColor("#1A1A1A"))

        // --- Cookie 管理 ---
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
        CookieManager.getInstance().setCookie(baseUrl, "token=$token; path=/; max-age=2592000")

        // --- 下拉刷新组件 ---
        swipeRefreshLayout = SwipeRefreshLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setColorSchemeColors(
                Color.parseColor("#3186FF"),
                Color.parseColor("#FBBC04"),
                Color.parseColor("#00B95C"),
                Color.parseColor("#FC413D")
            )
            setProgressBackgroundColorSchemeColor(Color.parseColor("#222428"))
            setOnRefreshListener {
                webView.reload()
            }
            setOnChildScrollUpCallback { _, _ ->
                webView.scrollY > 0
            }
        }
        swipeRefreshLayout.addView(webView)

        // --- 容器（用原生 padding 处理系统栏） ---
        rootContainer = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#1A1A1A"))
        }
        rootContainer.addView(swipeRefreshLayout)

        // --- JavaScript 接口 ---
        webView.addJavascriptInterface(
            object {
                @JavascriptInterface
                fun getToken(): String = token

                @JavascriptInterface
                fun setToken(t: String) {
                    if (t.isNotBlank()) {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            tokenManager.saveAuthInfo(t, "DJSeven")
                        }
                    }
                }

                @JavascriptInterface
                fun getServerUrl(): String = baseUrl

                @JavascriptInterface
                fun setSystemBarColor(color: String) {
                    runOnUiThread {
                        try {
                            val parsed = parseWebColor(color)
                            rootContainer.setBackgroundColor(parsed)
                            window.statusBarColor = parsed
                            window.navigationBarColor = parsed
                            // 根据背景亮度自动切换状态栏文字颜色
                            // 浅色背景 → 深色文字，深色背景 → 浅色文字
                            val isLight = isColorLight(parsed)
                            WindowInsetsControllerCompat(window, window.decorView).apply {
                                isAppearanceLightStatusBars = isLight
                                isAppearanceLightNavigationBars = isLight
                            }
                        } catch(e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            },
            "AndroidBridge"
        )

        // --- WebViewClient ---
        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()
                if (!url.startsWith(baseUrl)) {
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                    return true
                }
                return false
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (::swipeRefreshLayout.isInitialized) {
                    swipeRefreshLayout.isRefreshing = false
                }

                // 注入 token 到 localStorage
                view.evaluateJavascript(
                    """
                    (function() {
                        try {
                            if (typeof localStorage !== 'undefined') {
                                var token = AndroidBridge.getToken();
                                if (token) {
                                    localStorage.setItem('token', token);
                                    localStorage.setItem('accessToken', token);
                                    localStorage.setItem('jwt_token', token);
                                }
                            }
                        } catch(e) { console.log('Token inject error:', e); }
                    })();
                    """.trimIndent(),
                    null
                )

                // 实时监听网页主题/背景色变化，动态同步 Android 状态栏与导航栏颜色
                view.evaluateJavascript(
                    """
                    (function() {
                        function updateSystemBar() {
                            try {
                                function getBg(el) {
                                    if (!el) return null;
                                    var bg = window.getComputedStyle(el).backgroundColor;
                                    if (bg && bg !== 'rgba(0, 0, 0, 0)' && bg !== 'transparent') return bg;
                                    return null;
                                }
                                var color = getBg(document.querySelector('header'))
                                    || getBg(document.body)
                                    || getBg(document.documentElement)
                                    || getBg(document.getElementById('root'))
                                    || '#181A1F';
                                AndroidBridge.setSystemBarColor(color);
                            } catch(e) {}
                        }
                        updateSystemBar();
                        if (!window.__themeObserverAttached) {
                            window.__themeObserverAttached = true;
                            var obs = new MutationObserver(updateSystemBar);
                            obs.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme', 'class', 'style'] });
                            if (document.body) obs.observe(document.body, { attributes: true, attributeFilter: ['class', 'style'] });
                            setInterval(updateSystemBar, 1500);
                        }
                    })();
                    """.trimIndent(),
                    null
                )

                // 注入 CSS：优化移动端适配
                view.evaluateJavascript(
                    """
                    (function() {
                        if (document.getElementById('mobile-fix-css')) return;
                        var style = document.createElement('style');
                        style.id = 'mobile-fix-css';
                        style.textContent = `
                            html, body {
                                overscroll-behavior: none !important;
                            }
                            [class*="install-prompt"], [class*="pwa-install"] { display: none !important; }
                        `;
                        document.head.appendChild(style);
                    })();
                    """.trimIndent(),
                    null
                )
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: android.webkit.SslErrorHandler,
                error: android.net.http.SslError
            ) {
                handler.proceed()
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: android.webkit.WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                if (::swipeRefreshLayout.isInitialized) {
                    swipeRefreshLayout.isRefreshing = false
                }
                if (request.isForMainFrame) {
                    showErrorPage(baseUrl)
                }
            }
        }

        // --- WebChromeClient ---
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                android.util.Log.d("AntigravityWeb", "${consoleMessage?.message()} (Line ${consoleMessage?.lineNumber()})")
                return true
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: android.webkit.ValueCallback<Array<Uri>?>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.filePathCallback = filePathCallback
                fileChooserLauncher.launch("*/*")
                return true
            }
        }

        // 加载 Web 端
        webView.loadUrl(baseUrl)

        // 应用 WindowInsets：避免状态栏挡住顶部 UI，避免底部手势条/导航栏挡住输入框
        ViewCompat.setOnApplyWindowInsetsListener(rootContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomPadding = if (ime.bottom > 0) ime.bottom else systemBars.bottom
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding)
            insets
        }

        setContentView(rootContainer)
    }

    /**
     * 解析网页返回的颜色字符串为 Android Color int。
     * 支持 "rgb(r, g, b)"、"rgba(r, g, b, a)"、"#RRGGBB" 格式。
     */
    private fun parseWebColor(color: String): Int {
        return try {
            val trimmed = color.trim()
            when {
                trimmed.startsWith("#") -> Color.parseColor(trimmed)
                trimmed.startsWith("rgb") -> {
                    val nums = trimmed
                        .replace(Regex("[^0-9,.]"), "")
                        .split(",")
                        .map { it.trim().toFloat() }
                    val r = nums.getOrElse(0) { 0f }.toInt()
                    val g = nums.getOrElse(1) { 0f }.toInt()
                    val b = nums.getOrElse(2) { 0f }.toInt()
                    Color.rgb(r, g, b)
                }
                else -> Color.parseColor("#1A1A1A")
            }
        } catch(e: Exception) {
            Color.parseColor("#1A1A1A")
        }
    }

    /**
     * 判断颜色是否为浅色（用于决定状态栏文字用深色还是浅色）。
     * 使用 W3C 标准的相对亮度公式。
     */
    private fun isColorLight(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        // 使用感知亮度公式：0.299R + 0.587G + 0.114B
        val brightness = 0.299 * r + 0.587 * g + 0.114 * b
        return brightness > 128
    }

    private fun showErrorPage(baseUrl: String) {
        val html = """
            <html><body style="background:#1a1a2e;color:#e0e0e0;font-family:sans-serif;
            display:flex;flex-direction:column;align-items:center;justify-content:center;
            height:100vh;margin:0;padding:20px;text-align:center;">
            <h2 style="color:#e94560;">连接失败</h2>
            <p>无法连接到服务器</p>
            <p style="color:#888;font-size:14px;word-break:break-all;">$baseUrl</p>
            <p style="color:#666;font-size:13px;margin-top:20px;">请检查网络连接或服务器地址</p>
            <button onclick="location.reload()" style="margin-top:20px;padding:10px 30px;
            background:#e94560;color:white;border:none;border-radius:8px;font-size:16px;
            cursor:pointer;">重试</button>
            </body></html>
        """.trimIndent()
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::webView.isInitialized) webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) webView.onResume()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.destroy()
        }
        super.onDestroy()
    }
}
