package com.pioneer.nycfirewire.activity

import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.data.auth.ApiClient.Companion.BASE_INCIDENT_URL
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.utils.Constants.DARK
import com.pioneer.nycfirewire.utils.Constants.LIGHT
import com.pioneer.nycfirewire.utils.Constants.INCIDENT_POST

class WebViewActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_post)

        val webView: WebView = findViewById(R.id.webview)

        // Enable JavaScript (important for many modern websites)
        webView.settings.javaScriptEnabled = true

        // Optional: Enable other settings like loading images, zoom controls, etc.
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false


        // Set WebViewClient to handle redirects within the WebView
        webView.webViewClient = WebViewClient()


        // Add JavaScript interface
        webView.addJavascriptInterface(object {
            // Define the method to be called from React JS
            @JavascriptInterface
            fun onButtonClick(message: String) {
               if(message=="close"){
                   finish()
               }else{
                   Toast.makeText(this@WebViewActivity, "Successfully Posted", Toast.LENGTH_SHORT).show()
                   finish()
               }
            }
        }, "AndroidInterface")

        // Load a URL
        var theme= if(prefs.isDarkMode) DARK else LIGHT
        var url=BASE_INCIDENT_URL.plus("/noauth/create/incident?form=add&token=").plus(prefs.token).plus("&theme=").plus(theme)
       // var url="https://dev-firewire.atomgroups.work/noauth/create/incident?form=add&token=".plus(prefs.token).plus("&theme=").plus(theme)

        println("WebPage:"+url)
        webView.loadUrl(url)

    }

    override fun onResume() {
        super.onResume()
        analyticMethod(INCIDENT_POST,"IncidentPostWebView")
    }

    // Override onBackPressed to handle back navigation for the WebView
    override fun onBackPressed() {
        val webView: WebView = findViewById(R.id.webview)
        if (webView.canGoBack()) {
            webView.goBack() // Go back in WebView history
        } else {
            super.onBackPressed() // Default back behavior (exit activity)
        }
    }



}
