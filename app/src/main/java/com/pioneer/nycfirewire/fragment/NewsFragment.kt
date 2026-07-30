package com.pioneer.nycfirewire.fragment

import android.net.Uri
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.pioneer.nycfirewire.ReplaceCallback
import com.pioneer.nycfirewire.activity.NewsDetailActivity
import com.pioneer.nycfirewire.adapter.setUpAdapter

import com.pioneer.nycfirewire.model.TrendingSearchItem
import com.pioneer.nycfirewire.model.TrendingSearchResponseWrapper
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.IntentUtils.NEWS_DETAILS
import com.pioneer.nycfirewire.utils.IntentUtils.PUB_DATE
import com.pioneer.nycfirewire.viewModel.FireWireViewModel
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.BottomSheetNewsBinding
import com.pioneer.nycfirewire.databinding.ItemBottomNewsBinding
import com.pioneer.nycfirewire.utils.Constants.FILTER_FRAGMENT
import com.pioneer.nycfirewire.utils.Constants.NEWS_FRAGMENT
import com.pioneer.nycfirewire.utils.DateUtils
import com.pioneer.nycfirewire.utils.NAV_FILTER
import com.pioneer.nycfirewire.utils.NetworkUtils.isOnline
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.putArgs
import com.pioneer.nycfirewire.utils.showToast
import com.pioneer.nycfirewire.utils.visible
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
        private const val NEWS_SITE_URL= "https://nycfirewire.net/news/"
        fun newInstance()= NewsFragment().putArgs {  }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback= context as? ReplaceCallback
    }

    override fun onResume() {
        super.onResume()
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, NEWS_FRAGMENT)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "NewsFragment")
        }

        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
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

        val itemTouchListener = object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                // Detect touch events here, for example, detect a click event
                rv.getParent().requestDisallowInterceptTouchEvent(true);

                return false // Return false to let other touch events be handled by the RecyclerView
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                // Handle the touch event (e.g., on a long press)
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                // Optional: Control whether RecyclerView should disallow intercepting touch events
            }
        }

        binding.rvMainFilter.addOnItemTouchListener(itemTouchListener)
    }


    private fun initViewModel() {
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)
        if(isOnline(requireContext())){
            vm.getNewsDetail()
        }else{
            showToast(requireContext(),getString(R.string.check_network_connection))
        }

        vm.newsLiveData.observe(viewLifecycleOwner){
            updateNewsData(it)
        }

    }

    private fun updateNewsData(response: Resource<TrendingSearchResponseWrapper>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                // tvNewsTotal is no longer a count — the redesign turns it into a
                // red uppercase link out to the FireWire news site (see clickEvent).
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
           val intent= Intent(requireContext(), NewsDetailActivity::class.java)
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
        // KEPT from this lineage: the redesign dropped this wiring, which would
        // have removed news filtering entirely. Preserved deliberately.
        binding.tvFilter.setOnClickListener {
            callback?.replaceFragment(NAV_FILTER,"")
        }

        // Added by the redesign: sheet-bar link out to the FireWire news site.
        binding.tvNewsTotal.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(NEWS_SITE_URL)))
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