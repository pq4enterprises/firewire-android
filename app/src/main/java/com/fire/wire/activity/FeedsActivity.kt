package com.fire.wire.activity

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.fire.wire.R
import com.fire.wire.adapter.setUpAdapter
import com.fire.wire.databinding.ActivityFeedBinding
import com.fire.wire.databinding.ItemLocalityBinding
import com.fire.wire.databinding.ItemMainFeedBinding
import com.fire.wire.databinding.ItemSubFeedBinding
import com.fire.wire.fragment.FeedFilterFragment
import com.fire.wire.model.user.response.FeedResponse
import com.fire.wire.model.user.response.FeedsGroupList
import com.fire.wire.model.user.response.FeedsItem
import com.fire.wire.resource.Resource
import com.fire.wire.resource.ResourceState
import com.fire.wire.utils.Constants
import com.fire.wire.utils.gone
import com.fire.wire.utils.replaceFragment
import com.fire.wire.utils.visible
import com.fire.wire.viewModel.FireWireViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class FeedsActivity : BaseActivity() {
    private lateinit var vm: FireWireViewModel

    private lateinit var binding: ActivityFeedBinding
    private var feedsGroupList= ArrayList<FeedsGroupList>()
    private var mediaPlayer: MediaPlayer? = null
    private val mTAG= FeedsActivity::class.java.canonicalName
    private var localityIds= ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)

        initExtra()
        clickEvent()
        initApiCall()
    }

   /* override fun onResume() {
        super.onResume()
        initExtra()
        clickEvent()
        initApiCall()
        binding.flMain.gone()
    }*/

    private fun initExtra() {
        if(intent!=null){
            if(intent.hasExtra("LocalityId")){
                localityIds= intent.getStringArrayListExtra("LocalityId")!!
            }
        }
    }

    private fun initApiCall() {

        vm.getFeedList(localityIds)
        vm.feedLiveData.observe(this, Observer {
          updateFeedList(it)
        })
    }

    private fun updateFeedList(response: Resource<FeedResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
               // if(response.data?.code== Constants.CODE_SUCCESS) {
                    val feedsList= response.data?.data
                    val groupSubList= ArrayList<FeedsItem>()
                    val groupMainList= ArrayList<FeedsGroupList>()

                val groupByLocality= feedsList?.groupBy { it.locality?._id }

                groupByLocality?.forEach { mapData->
                    groupSubList.clear()
                  val localityName=  mapData.value[0].locality?.name

                    if(mapData.value.isNotEmpty())
                   mapData.value.map {
                       groupSubList.add(FeedsItem(it.name.toString(),it.url.toString()))
                   }

                    val mainObj= FeedsGroupList(localityName.toString(),groupSubList)
                    groupMainList.add(mainObj)

                }

                setupAdapter(groupMainList)






               /* }else{
                    showAlert(response.data?.message.toString())
                }*/


            }
            ResourceState.ERROR -> {
                binding.progress.gone()

            }
        }
    }

    private fun setupAdapter(groupMainList: ArrayList<FeedsGroupList>) {
        if(groupMainList.isNotEmpty()){
            binding.tvNoData.gone()
            binding.rvFeed.visible()
        }else{
            binding.tvNoData.visible()
            binding.rvFeed.gone()
        }

        binding.rvFeed.setUpAdapter(
            groupMainList,
            R.layout.item_main_feed,
            ItemMainFeedBinding::inflate,
            { it,pos,bindingItem->
                bindingItem.tvLocTitle.text= it.feedStateName
                val feedSongList= ArrayList(it.feedUrlList?:ArrayList())

                bindingItem.rvFeedSong.setUpAdapter(
                    feedSongList,
                  R.layout.item_sub_feed,
                  ItemSubFeedBinding::inflate,
                    {it1,pos1,subBindingItem->
                        subBindingItem.tvMusicName.text= it1.feedName
                        subBindingItem.ivPlay.setOnClickListener {

                            val videoUrl = it1.feedUrl
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                            startActivity(intent)



                            /*subBindingItem.ivPause.visible()
                            subBindingItem.ivPlay.gone()
                            val mediaUrl = "http://52.73.63.36:8000/manhattan"

                            mediaPlayer = MediaPlayer().apply {
                                setDataSource(mediaUrl)
                                prepareAsync() // Prepare asynchronously to avoid blocking the UI thread
                                setOnPreparedListener {
                                    start() // Start playing once prepared
                                }
                            }*/
                        }

                        subBindingItem.ivPause.setOnClickListener {
                            mediaPlayer?.pause()
                            subBindingItem.ivPlay.visible()
                            subBindingItem.ivPause.gone()
                        }

                    }
                )

            }
        )
    }

    private fun clickEvent() {
        binding.tvClose.setOnClickListener {
            finish()
        }

        binding.tvFilter.setOnClickListener {
            val intent = Intent(this, FeedFilterActivity::class.java )
            startActivity(intent)
            finish()

        }
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.release()
    }

    /*private fun displayFragment(fragment: Fragment, flag:Boolean){
        replaceFragment(
            fragment,
            mTAG,
            allowStateLoss = true,
            containerViewId = R.id.fl_main,
            allowBackStack = flag
        )
    }*/


}