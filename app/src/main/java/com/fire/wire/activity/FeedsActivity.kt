package com.fire.wire.activity

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.fire.wire.R
import com.fire.wire.adapter.setUpAdapter
import com.fire.wire.databinding.ActivityFeedBinding
import com.fire.wire.databinding.ItemMainFeedBinding
import com.fire.wire.databinding.ItemSubFeedBinding
import com.fire.wire.model.user.response.FeedResponse
import com.fire.wire.model.user.response.FeedsGroupList
import com.fire.wire.model.user.response.FeedsItem
import com.fire.wire.model.user.response.UserDetails
import com.fire.wire.resource.Resource
import com.fire.wire.resource.ResourceState
import com.fire.wire.service.ScannerPlaybackService
import com.fire.wire.utils.IntentUtils.UPDATE_PROFILE
import com.fire.wire.utils.gone
import com.fire.wire.utils.visible
import com.fire.wire.viewModel.FireWireViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class FeedsActivity : BaseActivity(), ScannerPlaybackService.Listener {
    private lateinit var vm: FireWireViewModel

    private lateinit var binding: ActivityFeedBinding
    private var feedsGroupList= ArrayList<FeedsGroupList>()
    private val mTAG= FeedsActivity::class.java.canonicalName
    private var localityIds= ArrayList<String>()

    // user details for the premium gate (fetched on create; see passesPremiumGate)
    private var gateUserDetails: UserDetails? = null

    // instrument-console UI state
    private var selectedFeed: FeedsItem? = null
    private var selectedRegion: String? = null
    private val expandedRegions = HashMap<String, Boolean>()
    private val segmentViews = ArrayList<View>()
    private val barViews = ArrayList<View>()
    private val waveAnimators = ArrayList<ObjectAnimator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)

        buildSignalMeter()
        buildWave()
        initExtra()
        clickEvent()
        initApiCall()
    }

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

        // role needed for the premium gate (no spinner: fetched quietly)
        vm.getUserDetails()
        vm.userLiveData.observe(this, Observer {
            if(it.state == ResourceState.SUCCESS) {
                gateUserDetails= it.data?.data
            }
        })
    }

    private fun updateFeedList(response: Resource<FeedResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
               // if(response.data?.code== Constants.CODE_SUCCESS) {
                    val feedsList= response.data?.data
                    val groupMainList= ArrayList<FeedsGroupList>()

                val groupByLocality= feedsList?.groupBy { it.locality?._id }

                groupByLocality?.forEach { mapData->
                    val groupSubList= ArrayList<FeedsItem>()
                  val localityName=  mapData.value[0].locality?.name

                    if(mapData.value.isNotEmpty())
                   mapData.value.map {
                       groupSubList.add(FeedsItem(it.name.toString(),it.url.toString()))
                   }

                    val mainObj= FeedsGroupList(localityName.toString(),groupSubList)
                    groupMainList.add(mainObj)

                }

                feedsGroupList= groupMainList
                updateConsoleTotals(groupMainList)
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
                val regionName= it.feedStateName
                bindingItem.tvLocTitle.text= regionName
                val feedSongList= ArrayList(it.feedUrlList?:ArrayList())
                bindingItem.tvFeedCount.text= getString(R.string.scanner_feed_count, feedSongList.size)

                val expanded= expandedRegions[regionName] ?: true
                bindingItem.ivChevron.rotation= if(expanded) 90f else 0f
                bindingItem.rvFeedSong.visibility= if(expanded) View.VISIBLE else View.GONE
                bindingItem.llRegionHeader.setOnClickListener {
                    expandedRegions[regionName]= !(expandedRegions[regionName] ?: true)
                    binding.rvFeed.adapter?.notifyItemChanged(pos)
                }

                bindingItem.rvFeedSong.setUpAdapter(
                    feedSongList,
                  R.layout.item_sub_feed,
                  ItemSubFeedBinding::inflate,
                    {it1,pos1,subBindingItem->
                        subBindingItem.tvMusicName.text= it1.feedName
                        subBindingItem.tvFeedMeta.text= sourceHost(it1.feedUrl)

                        val isSelected= selectedFeed?.feedUrl == it1.feedUrl && selectedFeed?.feedName == it1.feedName
                        val isActive= ScannerPlaybackService.isActiveFeed(it1.feedUrl)

                        ViewCompat.setBackgroundTintList(
                            subBindingItem.ivPlay,
                            ContextCompat.getColorStateList(this@FeedsActivity, if(isSelected) R.color.fw_red else R.color.fw_success_tint)
                        )
                        subBindingItem.ivPlay.imageTintList=
                            ContextCompat.getColorStateList(this@FeedsActivity, if(isSelected) R.color.white else R.color.fw_success)

                        // real playback state drives the row icon (recycled views: always set both)
                        if(isActive){
                            subBindingItem.ivPlay.gone()
                            subBindingItem.ivPause.visible()
                        }else{
                            subBindingItem.ivPause.gone()
                            subBindingItem.ivPlay.visible()
                        }

                        subBindingItem.ivPlay.setOnClickListener {
                            startPlayback(it1, regionName)
                        }

                        subBindingItem.ivPause.setOnClickListener {
                            stopPlayback()
                        }

                        /* Previous implementation launched the stream in the browser.
                           Kept only as a fallback in case a feed format ever proves
                           unplayable by the platform MediaPlayer (all known feeds are
                           Icecast audio/mpeg streams, which MediaPlayer handles):

                        val videoUrl = it1.feedUrl
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                        startActivity(intent)
                        */

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

        binding.btnTransport.setOnClickListener {
            val feed= selectedFeed ?: return@setOnClickListener
            if(ScannerPlaybackService.isActiveFeed(feed.feedUrl)){
                stopPlayback()
            }else{
                startPlayback(feed, selectedRegion ?: "")
            }
        }
    }

    // ===== playback control =====

    private fun startPlayback(feed: FeedsItem, region: String) {
        if(!passesPremiumGate()){
            launchPaywall()
            return
        }
        setNowMonitoring(feed, region)
        ScannerPlaybackService.play(this, feed.feedName, feed.feedUrl, region)
    }

    private fun stopPlayback() {
        ScannerPlaybackService.stop(this)
    }

    // =========================================================================
    // PREMIUM GATE — parity with iOS, which blocks scanner playback for the
    // "basic_user" role and shows the paywall. To remove the gate, delete
    // passesPremiumGate()/launchPaywall() and the single check at the top of
    // startPlayback() above.
    // =========================================================================

    /** Same rule as MyAccountActivity.isPremium(): any non-empty role other
     *  than "basic_user" is premium. If the role hasn't loaded (or the fetch
     *  failed) we do NOT block, to avoid locking out premium users on a slow
     *  network — the server still protects premium-only content. */
    private fun passesPremiumGate(): Boolean {
        val role = gateUserDetails?.role ?: return true
        return role.isNotEmpty() && role != "basic_user"
    }

    /** basic_user tapped play: send them to the account screen (merged paywall). */
    private fun launchPaywall() {
        val intent = Intent(this, MyAccountActivity::class.java)
        gateUserDetails?.let { intent.putExtra(UPDATE_PROFILE, it) }
        startActivity(intent)
    }

    // ===== ScannerPlaybackService.Listener (main thread) =====

    override fun onPlaybackStateChanged(state: ScannerPlaybackService.State) {
        renderPlaybackState(state)
    }

    override fun onPlaybackError(feedName: String?) {
        renderPlaybackState(ScannerPlaybackService.State.IDLE)
        showAlert(getString(R.string.scanner_feed_unavailable))
    }

    // ===== instrument console =====

    private fun updateConsoleTotals(groupMainList: ArrayList<FeedsGroupList>) {
        val totalFeeds= groupMainList.sumOf { it.feedUrlList?.size ?: 0 }
        binding.tvLiveCount.text= totalFeeds.toString()
        binding.tvMetaFeeds.text= getString(R.string.scanner_feeds_meta, totalFeeds)
        binding.tvMetaRegions.text= getString(R.string.scanner_regions_meta, groupMainList.size)
    }

    /** Locks the console readouts onto a feed. Status/wave/signal are driven
     *  separately by renderPlaybackState from real MediaPlayer state. */
    private fun setNowMonitoring(feed: FeedsItem, region: String) {
        selectedFeed= feed
        selectedRegion= region

        binding.tvChannelName.text= feed.feedName
        binding.tvReadoutRegion.text= region
        binding.tvReadoutSource.text= sourceHost(feed.feedUrl)
    }

    /** Mirrors true playback state onto the console: STANDBY -> BUFFERING -> RX. */
    private fun renderPlaybackState(state: ScannerPlaybackService.State) {
        val statusText: String
        val statusColor: Int
        when (state) {
            ScannerPlaybackService.State.PLAYING -> {
                statusText= getString(R.string.scanner_rx)
                statusColor= R.color.fw_success
                litSignal(true)
                startWave()
                binding.ivTransport.setImageResource(R.drawable.fw_ic_pause)
                binding.btnTransport.alpha= 1f
            }
            ScannerPlaybackService.State.BUFFERING -> {
                statusText= getString(R.string.scanner_buffering)
                statusColor= R.color.fw_warning
                litSignal(false)
                stopWave()
                binding.ivTransport.setImageResource(R.drawable.fw_ic_pause)
                binding.btnTransport.alpha= 1f
            }
            ScannerPlaybackService.State.IDLE -> {
                statusText= getString(R.string.scanner_standby)
                statusColor= R.color.fw_warning
                litSignal(false)
                stopWave()
                binding.ivTransport.setImageResource(R.drawable.fw_ic_play)
                binding.btnTransport.alpha= if(selectedFeed != null) 1f else 0.4f
            }
        }

        binding.tvStatus.text= statusText
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, statusColor))
        binding.tvReadoutStatus.text= statusText
        binding.tvReadoutStatus.setTextColor(ContextCompat.getColor(this, statusColor))
        ViewCompat.setBackgroundTintList(
            binding.vStatusDot,
            ContextCompat.getColorStateList(this, statusColor)
        )

        binding.rvFeed.adapter?.notifyDataSetChanged()
    }

    private fun sourceHost(url: String): String {
        return try {
            Uri.parse(url).host?.removePrefix("www.")?.uppercase()
                ?: getString(R.string.scanner_stream)
        } catch (e: Exception) {
            getString(R.string.scanner_stream)
        }
    }

    private fun buildSignalMeter() {
        val density= resources.displayMetrics.density
        binding.llSignal.removeAllViews()
        segmentViews.clear()
        for (i in 0 until SEG_COUNT) {
            val seg= View(this)
            val lp= LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            lp.marginStart= if(i == 0) 0 else (2 * density).toInt()
            seg.layoutParams= lp
            binding.llSignal.addView(seg)
            segmentViews.add(seg)
        }
        litSignal(false)
    }

    private fun litSignal(receiving: Boolean) {
        segmentViews.forEachIndexed { i, seg ->
            val color= when {
                !receiving || i >= SEG_LIT -> R.color.fw_scanner_seg_off
                i < 9 -> R.color.fw_success
                i < 13 -> R.color.fw_warning
                else -> R.color.fw_red
            }
            seg.setBackgroundColor(ContextCompat.getColor(this, color))
        }
    }

    private fun buildWave() {
        val density= resources.displayMetrics.density
        binding.llWave.removeAllViews()
        barViews.clear()
        for (i in 0 until BAR_COUNT) {
            val bar= View(this)
            val lp= LinearLayout.LayoutParams(0, (34 * density).toInt(), 1f)
            lp.marginStart= if(i == 0) 0 else (3 * density).toInt()
            bar.layoutParams= lp
            bar.setBackgroundResource(R.drawable.fw_scanner_wave_bar)
            bar.scaleY= WAVE_MIN
            binding.llWave.addView(bar)
            barViews.add(bar)
        }
    }

    private fun startWave() {
        stopWave()
        binding.llWave.alpha= 1f
        barViews.forEachIndexed { i, bar ->
            val anim= ObjectAnimator.ofFloat(bar, View.SCALE_Y, WAVE_MIN, 1f, WAVE_MIN)
            anim.duration= 900
            anim.repeatCount= ValueAnimator.INFINITE
            anim.startDelay= ((i * 37) % 100) * 9L
            anim.start()
            waveAnimators.add(anim)
        }
    }

    private fun stopWave() {
        waveAnimators.forEach { it.cancel() }
        waveAnimators.clear()
        barViews.forEach { it.scaleY= WAVE_MIN }
    }

    override fun onStart() {
        super.onStart()
        ScannerPlaybackService.listener= this
        syncFromService()
    }

    override fun onStop() {
        super.onStop()
        if(ScannerPlaybackService.listener === this) {
            ScannerPlaybackService.listener= null
        }
        // Only the visuals stop here — audio keeps playing in the foreground service.
        stopWave()
    }

    /** Re-sync console + list with the service after (re)entering the screen —
     *  playback may have continued in the background or been stopped from the
     *  notification while we weren't listening. */
    private fun syncFromService() {
        val state= ScannerPlaybackService.state
        if(state != ScannerPlaybackService.State.IDLE) {
            val name= ScannerPlaybackService.currentFeedName
            val url= ScannerPlaybackService.currentFeedUrl
            if(name != null && url != null) {
                setNowMonitoring(FeedsItem(name, url), ScannerPlaybackService.currentRegion ?: "")
            }
        }
        renderPlaybackState(state)
    }

    companion object {
        private const val SEG_COUNT= 16
        private const val SEG_LIT= 14
        private const val BAR_COUNT= 30
        private const val WAVE_MIN= 0.16f
    }

}
