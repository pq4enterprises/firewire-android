package com.pioneer.nycfirewire.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.bumptech.glide.Glide
import com.pioneer.nycfirewire.utils.DateUtils
import com.pioneer.nycfirewire.utils.IntentUtils.NEWS_DETAILS
import com.pioneer.nycfirewire.utils.IntentUtils.PUB_DATE
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.visible
import com.pioneer.nycfirewire.databinding.ActivityNewsDetailBinding
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class NewsDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityNewsDetailBinding
    private var sharingLink=""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityNewsDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        clickEvent()
        initUi()
        initExtra()
    }

    private fun initExtra() {
        val date= intent.getStringExtra(PUB_DATE)?:""
        val content= intent.getStringExtra(NEWS_DETAILS)?:""
       // initBinding(date,content)
        setUrlWebView(content)

    }

  /*  override fun onResume() {
        super.onResume()
        analyticMethod(NEWS_DETAIL,"NewsDetailActivity")
    }*/

    private fun initBinding(date: String, content: String) {
        binding.tvDateTime.text= DateUtils.convertDate(date)
        try{
            // Parse the HTML content
            val document: Document = Jsoup.parse(content)


            val links: List<Element> = document.select("a")

            if(links.isNotEmpty()) sharingLink= links.get(0).attr("href")

            println("sharingLink:"+sharingLink)



            var htmString=""
            val data= document.body().parentNode()?.childNodes()?.get(1)?.childNodes()

            if(data?.isNotEmpty() == true){
            data.forEachIndexed { index, node ->

                if(index!=0) {
                    if (htmString.isEmpty()) {
                        htmString = node.toString()
                    } else {
                        htmString = htmString.plus("\n").plus(node.toString())
                    }
                }
            }}


            val webSettings= binding.webView.settings
            webSettings.setSupportZoom(true)
            webSettings.javaScriptEnabled=true
            webSettings.domStorageEnabled = true // Enable DOM Storage if needed
            webSettings.setAllowFileAccess(true) // Enable access to local files if needed
            webSettings.setAllowContentAccess(true) // Allow content access
            webSettings.loadsImagesAutomatically = true
            binding.webView.loadData(htmString, "text/html", "UTF-8")



            val articleTitle: String = document.select("a[title]").attr("title")
            binding.tvTitle.text= articleTitle


            // Extract the image URL (src attribute) of the first image
            val imageElement: Element? = document.select("img").first()
            val imageUrl: String = imageElement?.attr("src") ?: "No image found"
            Glide.with(this)
                .load(imageUrl)
                .into(binding.ivBanner)


        }catch (e:Exception){
            e.printStackTrace()
        }



    }


    private fun initUi() {
        binding.toolbar.ivFeed.gone()
        binding.toolbar.tvShare.visible()
    }

    private fun clickEvent() {
        binding.toolbar.ivFeed.setOnClickListener {
            val intent = Intent(this, FeedsActivity::class.java)
            startActivity(intent)
        }
        binding.toolbar.ivMenu.setOnClickListener {
            finish()
        }

        binding.toolbar.tvShare.setOnClickListener {
            shareText(this,sharingLink)
        }
    }

    fun shareText(context: Context, text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text) // Add the text to share
            type = "text/plain"  // Specify the MIME type as text
        }

        // Show a system chooser dialog to pick the app for sharing
        context.startActivity(Intent.createChooser(sendIntent, "Share via"))
    }


    private fun setUrlWebView(content: String){

        // Parse the HTML content
        val document: Document = Jsoup.parse(content)
        val links: List<Element> = document.select("a")
        if(links.isNotEmpty()) sharingLink= links.get(0).attr("href")
        val webView = binding.webView

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.progress.visibility = View.VISIBLE
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progress.visibility = View.GONE
                super.onPageFinished(view, url)
            }
        }

        // Enable JavaScript (important for many modern websites)
        webView.settings.javaScriptEnabled = true

        // Optional: Enable other settings like loading images, zoom controls, etc.
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false

        // Set WebViewClient to handle redirects within the WebView
        webView.webViewClient = WebViewClient()

        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun closeWebView() {
                // Close the activity or perform the back action
                Toast.makeText(this@NewsDetailActivity, "Successfully Posted", Toast.LENGTH_SHORT).show()
                finish() // or use super.onBackPressed() if you want to handle back action
            }
        }, "AndroidInterface")

        println("WebPage:"+sharingLink)
        webView.loadUrl(sharingLink)
    }


    }




