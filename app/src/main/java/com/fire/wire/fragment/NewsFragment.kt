package com.fire.wire.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.fire.wire.R
import com.fire.wire.ReplaceCallback
import com.fire.wire.activity.NewsDetailActivity
import com.fire.wire.adapter.setUpAdapter
import com.fire.wire.databinding.BottomSheetNewsBinding
import com.fire.wire.databinding.ItemBottomNewsBinding

import com.fire.wire.model.TrendingSearchItem
import com.fire.wire.model.TrendingSearchResponseWrapper
import com.fire.wire.resource.Resource
import com.fire.wire.resource.ResourceState
import com.fire.wire.utils.*
import com.fire.wire.utils.IntentUtils.NEWS_DETAILS
import com.fire.wire.utils.IntentUtils.PUB_DATE
import com.fire.wire.viewModel.FireWireViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element


@AndroidEntryPoint
class NewsFragment:Fragment() {

    private lateinit var binding: BottomSheetNewsBinding
    private var callback: ReplaceCallback?=null
    private lateinit var vm: FireWireViewModel

    companion object{
        fun newInstance()= NewsFragment().putArgs {  }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback= context as? ReplaceCallback
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = BottomSheetNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        clickEvent()
        //binding.rvMainFilter.isNestedScrollingEnabled=false
    }


    private fun initViewModel() {
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)
        vm.getNewsDetail()
        vm.newsLiveData.observe(viewLifecycleOwner){
            updateNewsData(it)
        }

    }

    private fun updateNewsData(response: Resource<TrendingSearchResponseWrapper>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                val totalNews= response.data?.channel?.itemList?.size.toString()
                binding.tvNewsTotal.text= getString(R.string.news_listed, totalNews)
                val list= response.data?.channel?.itemList?:ArrayList()
                setupAdapter(ArrayList(list))

            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showAlert(response.message)
            }
        }
    }


    private fun setupAdapter(list: ArrayList<TrendingSearchItem>) {
        if(list.isNotEmpty()){
            binding.tvNoData.gone()
            binding.rvMainFilter.visible()
        }else{
            binding.tvNoData.visible()
            binding.rvMainFilter.gone()
        }


        binding.rvMainFilter.setUpAdapter(
            list,
            R.layout.item_bottom_news,
            ItemBottomNewsBinding::inflate,
            { it,pos,bindingItem->

                bindingItem.tvTitle.text= it.title

                bindingItem.tvDateTime.text= DateUtils.convertDate(it.pubDate)


                try{
                    val document: Document = Jsoup.parse(it.description)

                    val descriptionData = document.select("p")
                    bindingItem.tvDesc.text= descriptionData[0].text()


                    val imgTags = document.select("img")
                    for (imgTag: Element in imgTags) {
                        val src = imgTag.attr("src")
                        Glide.with(this)
                            .load(src)
                            .into(bindingItem.ivBanner)
                    }
                }catch (e:Exception){
                    e.printStackTrace()
                }

       bindingItem.llNews.setOnClickListener { view->
           val intent= Intent(requireContext(),NewsDetailActivity::class.java)
           intent.putExtra(PUB_DATE,it.pubDate)
           intent.putExtra(NEWS_DETAILS,it.content)
           startActivity(intent)
       }

                bindingItem.llShare.setOnClickListener { view->
                    shareText(requireContext(), it.link)
                }


            }
        )
    }

    private fun clickEvent() {
        binding.tvFilter.setOnClickListener {
            callback?.replaceFragment(NAV_FILTER,"")
        }

    }

    fun showAlert(message: String? = "") {
        AlertDialog.Builder(requireContext())
            .setTitle(resources.getString(R.string.app_name))
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
            }
            .show()
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