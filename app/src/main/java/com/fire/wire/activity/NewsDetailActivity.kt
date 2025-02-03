package com.fire.wire.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.fire.wire.databinding.ActivityNewsDetailBinding
import com.fire.wire.utils.DateUtils
import com.fire.wire.utils.IntentUtils.NEWS_DETAILS
import com.fire.wire.utils.IntentUtils.PUB_DATE
import com.fire.wire.utils.gone
import com.fire.wire.utils.visible
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class NewsDetailActivity :AppCompatActivity() {

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
        initBinding(date,content)
    }

    private fun initBinding(date: String, content: String) {
        binding.tvDateTime.text= DateUtils.convertDate(date)
        try{
            // Parse the HTML content
            val document: Document = Jsoup.parse(content)


            val links: List<Element> = document.select("a")

            if(links.isNotEmpty())
            sharingLink= links.get(0).attr("href")



            var htmString=""
            val data= document.body().parentNode()?.childNodes()?.get(1)?.childNodes()

            if(data?.isNotEmpty() == true)
            data.forEachIndexed { index, node ->

                if(index!=0) {
                    if (htmString.isEmpty()) {
                        htmString = node.toString()
                    } else {
                        htmString = htmString.plus("\n").plus(node.toString())
                    }
                }
            }


            val webSettings= binding.webView.settings
            webSettings.setSupportZoom(true)
            webSettings.javaScriptEnabled=true
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
            val intent = Intent(this,FeedsActivity::class.java)
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



}
