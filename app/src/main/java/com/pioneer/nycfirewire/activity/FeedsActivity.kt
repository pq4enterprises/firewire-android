package com.pioneer.nycfirewire.activity

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.service.BackgroundAudioService
import com.pioneer.nycfirewire.utils.Constants.AUDIO_BROADCAST
import com.pioneer.nycfirewire.utils.Constants.RADIO_CLICKED
import com.pioneer.nycfirewire.utils.Constants.RADIO_FEED
import com.pioneer.nycfirewire.utils.Constants.USER_BASIC_USER
import com.pioneer.nycfirewire.utils.IntentUtils.FROM_ACCOUNT
import com.pioneer.nycfirewire.utils.IntentUtils.OTHER
import dagger.hilt.android.AndroidEntryPoint


/**
 * Scanner — "Instrument Panel" (Direction 1A, 2026 design system).
 *
 * The re-skin is presentation only: playback still runs through
 * [BackgroundAudioService] (foreground service + ExoPlayer), and the paywall
 * gate, the AUDIO_BROADCAST receiver, the feed-switch / stop confirmations and
 * the Firebase screen + radio_clicked analytics are the production behaviour,
 * unchanged. The console at the top is a readout of that existing state — it
 * derives everything from whichever Feed currently has isChecked = true.
 */
@AndroidEntryPoint
class FeedsActivity : BaseActivity() {
    private lateinit var vm: FireWireViewModel

    private lateinit var binding: ActivityFeedBinding
    private val mTAG= FeedsActivity::class.java.canonicalName

    /**
     * Reloads the feed list after the user edits their areas, so a newly added or
     * removed region shows up straight away instead of on next launch. Scoped to
     * this one round trip rather than a blanket refresh in onResume, which would
     * rebuild the list — and disturb the playing-feed UI — every time the user
     * came back from anything at all.
     */
    private val areasAlertsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        vm.getFeedList(arrayListOf())
    }

    private lateinit var receiver: BroadcastReceiver
    val groupMainList= ArrayList<FeedsGroupList>()

    private var isActivityPaused=false

    var isBroadCastReceived= false
    var mainPosition=-1
    var subPosition=-1

    // instrument-console UI state — presentation only
    private val expandedRegions = HashMap<String, Boolean>()
    private val segmentViews = ArrayList<View>()
    private val barViews = ArrayList<View>()
    private val waveAnimators = ArrayList<ObjectAnimator>()


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)

        buildSignalMeter()
        buildWave()
        refreshConsole()
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
        refreshConsole()

    }


    override fun onPause() {
        super.onPause()
        isActivityPaused= true
    }

    override fun onStop() {
        super.onStop()
        // only the animation stops here — audio keeps running in the foreground service
        stopWave()
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
        refreshConsole()
    }

    private fun initApiCall() {
        vm.getFeedList(arrayListOf())
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

                val groupByLocality= feedsList?.groupBy { it.locality?._id }

                groupByLocality?.forEach { mapData->
                    var feedsGroup = FeedsGroupList()
                     feedsGroup.feedStateName= mapData.value.get(0).locality?.name.toString()
                    feedsGroup.feedUrlList= mapData.value
                    groupMainList.add(feedsGroup)
                }
                if(prefs.feedMainPosition!=-1 && prefs.feedSubPosition!=-1){
                    updateAudioPlayUI(prefs.feedMainPosition, prefs.feedSubPosition)
                }

                updateConsoleTotals()
                setupAdapter()
                refreshConsole()

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



    private fun setupAdapter() {
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
                val feedSongList= ArrayList(it.feedUrlList ?: ArrayList())
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
                        subBindingItem.tvMusicName.text= it1.name
                        subBindingItem.tvFeedMeta.text= sourceHost(it1.url)
                        if(it1.isChecked) {
                            subBindingItem.ivPause.visible()
                            subBindingItem.ivPlay.gone()
                            tintDot(subBindingItem.vFeedDot, R.color.fw_red)

                            val mediaUrl = it1.url.toString()

                           playAudio(mediaUrl,pos,pos1)

                        }else{
                            subBindingItem.ivPause.gone()
                            subBindingItem.ivPlay.visible()
                            tintDot(subBindingItem.vFeedDot, R.color.fw_success)
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
                                                        refreshConsole()
                                                    }
                                                    .show()
                                                return@forEachIndexed
                                            }
                                        }
                                    }
                                }else{
                                    // fires here too, not just on the switch-feed
                                    // confirmation, so a cold start is counted
                                    analyticMethod(RADIO_CLICKED,"FeedsActivity")
                                    stopAudio(false, pos,pos1)
                                    var localCheck= it1.isChecked
                                    groupMainList.forEach {
                                        it.feedUrlList?.forEachIndexed{ index, feed->
                                            feed.isChecked=false
                                        }
                                    }
                                    it1.isChecked= !localCheck
                                    binding.rvFeed.adapter?.notifyDataSetChanged()
                                    refreshConsole()
                                }

                            }

                        }

                        subBindingItem.ivPause.setOnClickListener {
                            stopAudio(true,pos,pos1)
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
                refreshConsole()
            }
            .show()
    }


    private fun clickEvent() {
        binding.tvClose.setOnClickListener {
            prefs.isRecreate=true
            finish()
        }

        binding.tvFilter.setOnClickListener {
            // Areas & Alerts is the only area-selection screen now. It writes
            // UserLocality, which is what GET /api/app/feed actually filters on — the
            // old feed-filter screen sent its selection as a query param the server
            // never reads, so it could not filter this list even in principle.
            // No finish(): the user comes back here, and we reload on return.
            areasAlertsLauncher.launch(Intent(this, AreasAlertsActivity::class.java))
        }

        // transport mirrors the row pause control for whatever is playing
        binding.btnTransport.setOnClickListener {
            val playing= playingIndex() ?: return@setOnClickListener
            stopAudio(true, playing.first, playing.second)
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
        prefs.isRecreate=true
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWave()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }


    // ===== instrument console (Direction 1A) — presentation only =====

    /** Index of the feed currently marked as playing, or null when idle. */
    private fun playingIndex(): Pair<Int, Int>? {
        groupMainList.forEachIndexed { i, group ->
            group.feedUrlList?.forEachIndexed { j, feed ->
                if (feed.isChecked) return i to j
            }
        }
        return null
    }

    private fun updateConsoleTotals() {
        val totalFeeds= groupMainList.sumOf { it.feedUrlList?.size ?: 0 }
        binding.tvLiveCount.text= totalFeeds.toString()
        binding.tvMetaFeeds.text= getString(R.string.scanner_feeds_meta, totalFeeds)
        binding.tvMetaRegions.text= getString(R.string.scanner_regions_meta, groupMainList.size)
    }

    /** Repaints the console from the current playback state. */
    private fun refreshConsole() {
        val playing= playingIndex()
        if (playing == null) {
            binding.tvChannelName.text= getString(R.string.scanner_no_channel)
            binding.tvReadoutRegion.text= getString(R.string.fw_dash)
            binding.tvReadoutSource.text= getString(R.string.fw_dash)
            setStatus(getString(R.string.scanner_standby), R.color.fw_warning)
            binding.ivTransport.setImageResource(R.drawable.fw_ic_play)
            binding.btnTransport.alpha= 0.4f
            litSignal(false)
            stopWave()
            binding.llWave.alpha= 0.28f
        } else {
            val group= groupMainList[playing.first]
            val feed= group.feedUrlList?.getOrNull(playing.second)
            binding.tvChannelName.text= feed?.name
            binding.tvReadoutRegion.text= group.feedStateName
            binding.tvReadoutSource.text= sourceHost(feed?.url)
            setStatus(getString(R.string.scanner_rx), R.color.fw_success)
            binding.ivTransport.setImageResource(R.drawable.fw_ic_pause)
            binding.btnTransport.alpha= 1f
            litSignal(true)
            startWave()
        }
    }

    private fun setStatus(label: String, colorRes: Int) {
        val color= ContextCompat.getColor(this, colorRes)
        binding.tvStatus.text= label
        binding.tvStatus.setTextColor(color)
        binding.tvReadoutStatus.text= label
        binding.tvReadoutStatus.setTextColor(color)
        tintDot(binding.vStatusDot, colorRes)
    }

    private fun tintDot(view: View, colorRes: Int) {
        ViewCompat.setBackgroundTintList(view, ContextCompat.getColorStateList(this, colorRes))
    }

    private fun sourceHost(url: String?): String {
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

    companion object {
        private const val SEG_COUNT= 16
        private const val SEG_LIT= 14
        private const val BAR_COUNT= 30
        private const val WAVE_MIN= 0.16f
    }

}
