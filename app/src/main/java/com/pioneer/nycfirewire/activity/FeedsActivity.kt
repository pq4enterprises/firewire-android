package com.pioneer.nycfirewire.activity

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.exoplayer.ExoPlayer
import com.pioneer.nycfirewire.adapter.setUpAdapter
import com.pioneer.nycfirewire.model.user.response.FeedResponse
import com.pioneer.nycfirewire.model.user.response.FeedsGroupList
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.visible
import com.pioneer.nycfirewire.viewModel.FireWireViewModel
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.ActivityFeedBinding
import com.pioneer.nycfirewire.databinding.ItemMainFeedBinding
import com.pioneer.nycfirewire.databinding.ItemSubFeedBinding
import com.pioneer.nycfirewire.model.user.response.Feed
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.service.BackgroundAudioService
import com.pioneer.nycfirewire.utils.Constants.AUDIO_BROADCAST
import com.pioneer.nycfirewire.utils.Constants.RADIO_CLICKED
import com.pioneer.nycfirewire.utils.Constants.RADIO_FEED
import com.pioneer.nycfirewire.utils.Constants.USER_BASIC_USER
import com.pioneer.nycfirewire.utils.IntentUtils.FROM_ACCOUNT
import com.pioneer.nycfirewire.utils.IntentUtils.OTHER
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class FeedsActivity : BaseActivity() {
    private lateinit var vm: FireWireViewModel

    private lateinit var binding: ActivityFeedBinding
    private var feedsGroupList= ArrayList<FeedsGroupList>()
    private val mTAG= FeedsActivity::class.java.canonicalName
    private var localityIds= ArrayList<String>()
    private val players = mutableMapOf<Int, ExoPlayer?>()
    private val pauseAction = "ACTION_PAUSE"

    private lateinit var receiver: BroadcastReceiver
    val groupMainList= ArrayList<FeedsGroupList>()

    private var isActivityPaused=false

    var isBroadCastReceived= false
    var mainPosition=-1
    var subPosition=-1


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)

        clickEvent()
        initApiCall()


        // Register the receiver to listen for the broadcast
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AUDIO_BROADCAST) {
                    isBroadCastReceived=true
                    mainPosition= intent.getIntExtra("mainPosition",-1)
                    subPosition= intent.getIntExtra("position",-1)
                    if (!isActivityPaused) {
                        updateAudioUI(mainPosition, subPosition)
                    }
                }
            }
        }

        // Step 2: Register the receiver with an IntentFilter
        val filter = IntentFilter(AUDIO_BROADCAST)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)
    }

    override fun onResume() {
        super.onResume()
        analyticMethod(RADIO_FEED,"FeedsActivity")

        isActivityPaused=false
        if(isBroadCastReceived) {
            updateAudioUI(mainPosition,subPosition)
            mainPosition=-1
            subPosition=-1
            isBroadCastReceived=false
            prefs.feedMainPosition=-1
            prefs.feedMainPosition=-1
        }

    }


    override fun onPause() {
        super.onPause()
        isActivityPaused= true
    }

    private fun updateAudioPlayUI(mainPosition: Int,subPosition: Int) {
        if(groupMainList.isNotEmpty()) {
            groupMainList[mainPosition].feedUrlList?.get(subPosition)?.isChecked = true
        }
    }


    private fun updateAudioUI(mainPosition: Int,subPosition: Int) {
      //  groupMainList[mainPosition].feedUrlList?.get(subPosition)?.isChecked=false
        groupMainList.forEach{
            it.feedUrlList?.forEach {
                it.isChecked=false
            }
        }

        binding.rvFeed.adapter?.notifyDataSetChanged()
    }

    private fun initApiCall() {
        vm.getFeedList(localityIds)
        vm.feedLiveData.observe(this, Observer {
          updateFeedList(it)
        })
    }

    @SuppressLint("SuspiciousIndentation")
    private fun updateFeedList(response: Resource<FeedResponse> ) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
               // if(response.data?.code== Constants.CODE_SUCCESS) {
                    val feedsList= response.data?.data
                groupMainList.clear()

                //val groupMainList= ArrayList<FeedsGroupList>()

                val groupByLocality= feedsList?.groupBy { it.locality?._id }

                val feedSongList= ArrayList<Feed>()
                groupByLocality?.forEach { mapData->
                    var feedsGroup = FeedsGroupList()
                     feedsGroup.feedStateName= mapData.value.get(0).locality?.name.toString()
                    feedsGroup.feedUrlList= mapData.value
                    //localMainList.add(feedsGroup)
                    groupMainList.add(feedsGroup)
                    feedSongList.addAll(mapData.value)
                }
                if(prefs.feedMainPosition!=-1 && prefs.feedSubPosition!=-1){
                    updateAudioPlayUI(prefs.feedMainPosition, prefs.feedSubPosition)
                }

                setupAdapter(feedSongList)

            }
            ResourceState.ERROR -> {
                binding.progress.gone()

            }
        }
    }

    private fun moveToPaymentPage(){
        val intent = Intent(this, MyAccountActivity::class.java)
        intent.putExtra(FROM_ACCOUNT, OTHER)
        startActivity(intent)
    }



    private fun setupAdapter(
        feedSongList: java.util.ArrayList<Feed>
    ) {
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
                //feedSongList.clear()
                 //feedSongList.addAll(ArrayList(it.feedUrlList?:ArrayList()))

                bindingItem.rvFeedSong.setUpAdapter(
                    ArrayList(it.feedUrlList),
                  R.layout.item_sub_feed,
                  ItemSubFeedBinding::inflate,
                    {it1,pos1,subBindingItem->
                        subBindingItem.tvMusicName.text= it1.name
                        if(it1.isChecked) {
                            subBindingItem.llPause.visible()
                            subBindingItem.ivPlay.gone()
                            subBindingItem.ivMusicPlay.visible()

                            val mediaUrl = it1.url.toString()

                           playAudio(mediaUrl,pos,pos1)

                        }else{
                            subBindingItem.llPause.gone()
                            subBindingItem.ivPlay.visible()
                            subBindingItem.ivMusicPlay.gone()
                        }
                        subBindingItem.ivPlay.setOnClickListener {
                            if(prefs.userRole==USER_BASIC_USER){
                                moveToPaymentPage()
                            }else {
                                //stopAudio(false, pos,pos1)

                                val hasCheckedItem = groupMainList.any { it.feedUrlList?.any { feedUrl -> feedUrl.isChecked == true } == true }

                                if(hasCheckedItem) {
                                    groupMainList.forEachIndexed { i, it ->
                                        it.feedUrlList?.forEachIndexed { index, feed ->
                                            if (feed.isChecked == true) {
                                                AlertDialog.Builder(this@FeedsActivity)
                                                    .setTitle("Listening")
                                                    .setMessage("Do you want to play new feed?")
                                                    .setCancelable(false)
                                                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                                                    }
                                                    .setPositiveButton(android.R.string.ok) { _, _ ->
                                                        analyticMethod(RADIO_CLICKED,"FeedsActivity")
                                                        BackgroundAudioService.stopService(this@FeedsActivity)
                                                        feed.isChecked = false
                                                        binding.rvFeed.adapter?.notifyItemChanged(
                                                            pos
                                                        )

                                                        var localCheck = it1.isChecked
                                                        groupMainList.forEach {
                                                            it.feedUrlList?.forEachIndexed { index, feed ->
                                                                feed.isChecked = false
                                                            }
                                                        }
                                                        it1.isChecked = !localCheck
                                                        binding.rvFeed.adapter?.notifyDataSetChanged()
                                                    }
                                                    .show()
                                                return@forEachIndexed
                                            }
                                        }
                                    }
                                }else{
                                    stopAudio(false, pos,pos1)
                                    var localCheck= it1.isChecked
                                    groupMainList.forEach {
                                        it.feedUrlList?.forEachIndexed{ index, feed->
                                            feed.isChecked=false
                                        }
                                    }
                                    it1.isChecked= !localCheck
                                    binding.rvFeed.adapter?.notifyDataSetChanged()
                                }

                            }

                        }

                        subBindingItem.llPause.setOnClickListener {
                            stopAudio(true,pos,pos1)
                           // subBindingItem.ivPlay.visible()
                            //subBindingItem.llPause.gone()
                        }

                    }
                )

            }
        )
    }

    //"https://mediaserv33.live-streams.nl:8034/live"

    private fun playAudio(url: String, mainPosition: Int,position:Int) {
        // Start the audio service to handle playback in the background
        prefs.feedMainPosition=mainPosition
        prefs.feedSubPosition= position
        if(mainPosition!=-1 || position!=-1){
        BackgroundAudioService.startService(
            this,
            url,
            mainPosition,
            position,
            ""
        )}
    }

    private fun stopAudio(isShowAlert: Boolean, mainPos: Int,subPos:Int) {
        // Start the audio service to handle playback in the background
        if(isShowAlert){
            showAlert(getString(R.string.stop_listening_msg),mainPos,subPos)
        }else BackgroundAudioService.stopService(this)
    }

    fun showAlert(message: String? = "",mainPos:Int,position: Int) {
        AlertDialog.Builder(this)
            .setTitle(resources.getString(R.string.stop_listening))
            .setMessage(message)
            .setCancelable(false)
            .setNegativeButton(android.R.string.cancel) { _, _ ->
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                BackgroundAudioService.stopService(this)
                prefs.feedMainPosition= -1
                prefs.feedSubPosition=-1
                groupMainList[mainPos].feedUrlList?.get(position)?.isChecked=false
                binding.rvFeed.adapter?.notifyDataSetChanged()
            }
            .show()
    }


    private fun clickEvent() {
        binding.tvClose.setOnClickListener {
            prefs.isRecreate=true
            finish()
        }

        binding.tvFilter.setOnClickListener {
            val intent = Intent(this, FeedFilterOrCommentsActivity::class.java )
            startActivity(intent)
            finish()

        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
        prefs.isRecreate=true
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }



}

