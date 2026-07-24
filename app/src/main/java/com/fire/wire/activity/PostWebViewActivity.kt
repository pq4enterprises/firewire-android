package com.fire.wire.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.fire.wire.R
import com.fire.wire.databinding.ActivityPostWebviewBinding
import com.fire.wire.prefs
import com.fire.wire.utils.gone
import com.fire.wire.utils.visible

/**
 * Admin POST — in-app WebView over the portal's authenticated create-incident
 * form, mirroring iOS PostWebViewController:
 *   https://admin.nycfirewireapp.com/noauth/create/incident?form=add&token=…&theme=…
 * (iOS APIEndpoints.postAdminUrl). The token is the same JWT the app sends as
 * its API Authorization bearer (prefs.token). It stays inside this WebView —
 * never the external browser, so it can't leak into browser history.
 *
 * The portal signals completion via the "closeWebView" message handler with
 * "close" / "submit" (iOS WKScriptMessageHandler); a shim maps the iOS-style
 * window.webkit.messageHandlers.closeWebView.postMessage(...) call onto the
 * Android bridge so the same portal code works here.
 */
class PostWebViewActivity : BaseActivity() {

    private lateinit var binding: ActivityPostWebviewBinding

    companion object {
        /** iOS APIEndpoints.postAdminUrl. */
        private const val POST_ADMIN_URL =
            "https://admin.nycfirewireapp.com/noauth/create/incident?form=add&token=%s&theme=%s"

        private const val CLOSE_BRIDGE = "closeWebViewAndroid"

        /** Maps the portal's iOS-style webkit message handler onto our bridge. */
        private const val WEBKIT_SHIM = """
            window.webkit = window.webkit || {};
            window.webkit.messageHandlers = window.webkit.messageHandlers || {};
            window.webkit.messageHandlers.closeWebView = {
                postMessage: function(m) { $CLOSE_BRIDGE.postMessage(String(m)); }
            };
        """
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.tvToolbarTitle.text = getString(R.string.menu_post)
        binding.toolbar.tvToolbarTitle.visible()
        binding.toolbar.ivFeed.gone()
        binding.toolbar.ivMenu.setOnClickListener { finish() }

        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        val requestUrl = String.format(
            POST_ADMIN_URL,
            prefs.token.orEmpty(),
            if (isDark) "dark" else "light"
        )

        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
        }

        binding.webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun postMessage(value: String) {
                if (value == "close" || value == "submit") {
                    binding.webView.post { finish() }
                }
            }
        }, CLOSE_BRIDGE)

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.progress.visible()
                view?.evaluateJavascript(WEBKIT_SHIM, null)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progress.gone()
                view?.evaluateJavascript(WEBKIT_SHIM, null)
            }

            // iOS parity: facebook links break out to the system handler,
            // everything else stays inside the authenticated WebView
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.contains("facebook.com")) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    return true
                }
                return false
            }
        }

        binding.webView.loadUrl(requestUrl)
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }
}
