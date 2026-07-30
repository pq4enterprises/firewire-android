package com.pioneer.nycfirewire.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.pioneer.nycfirewire.fragment.ImageDialogFragment
import com.pioneer.nycfirewire.model.incident.request.AddCommentRequest
import com.pioneer.nycfirewire.model.incident.response.CommentsResponse
import com.pioneer.nycfirewire.model.incident.response.Incident
import com.pioneer.nycfirewire.model.user.response.CommonResponse
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.IntentUtils.BUN_WIRE_DETAILS
import com.pioneer.nycfirewire.utils.IntentUtils.BUN_WIRE_LIST_DETAIL
import com.pioneer.nycfirewire.viewModel.FireWireViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.ActivityWireDetailBinding
import com.pioneer.nycfirewire.model.incident.response.IncidentByIdResponse
import com.pioneer.nycfirewire.model.incident.response.IncidentDataByID
import com.pioneer.nycfirewire.model.incident.response.IncidentSubLocality
import com.pioneer.nycfirewire.model.incident.response.Points
import com.pioneer.nycfirewire.utils.BitmapFromVector
import com.pioneer.nycfirewire.utils.Constants.CODE_SUCCESS
import com.pioneer.nycfirewire.utils.Constants.INCIDENT_DETAILS
import com.pioneer.nycfirewire.utils.Constants.INCIDENT_POST
import com.pioneer.nycfirewire.utils.Constants.PLAY_STORE_URL
import com.pioneer.nycfirewire.utils.DateUtils
import com.pioneer.nycfirewire.utils.IntentUtils
import com.pioneer.nycfirewire.utils.IntentUtils.INCIDENT_ID
import com.pioneer.nycfirewire.utils.NAV_COMMENT_LIST
import com.pioneer.nycfirewire.utils.NetworkUtils.isOnline
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.startNewActivity
import com.pioneer.nycfirewire.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlin.String
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.pioneer.nycfirewire.utils.FirehouseMarker
import com.pioneer.nycfirewire.utils.UnitCategory

@AndroidEntryPoint
class WireDetailActivity : BaseActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityWireDetailBinding
    private var wireDetails= Incident()
    private var incidentList= ArrayList<Incident>()
    private lateinit var mMap: GoogleMap
    private lateinit var vm: FireWireViewModel
    private var isLikeSingle=false
    var type="view"
    var fireStationlist= ArrayList<Points>()
    var likeCount=0
    var updatedPosition= -1
    var incidentId=""

    private var pendingIncidentResponse: Resource<IncidentByIdResponse>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityWireDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initMap()

        clickEvent()
        initExtra()
        initViewModel()

        setupMapTouchHandler()
    }


    private fun setupMapTouchHandler() {
        binding.mapOverlay.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // When user touches the map, tell ScrollView: "Don't steal this touch!"
                    binding.nestedScrollView.requestDisallowInterceptTouchEvent(true)
                    false
                }
                MotionEvent.ACTION_UP -> {
                    // When user lifts finger, allow ScrollView to scroll normally again
                    binding.nestedScrollView.requestDisallowInterceptTouchEvent(false)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    binding.nestedScrollView.requestDisallowInterceptTouchEvent(true)
                    false
                }
                else -> false
            }
        }
    }


    private fun initViewModel() {
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)
        if(isOnline(this)) {

           /* val commentRequest = AddCommentRequest(
                prefs.userId.toString(), wireDetails._id, type
            )

            vm.postComment(commentRequest)*/

            vm.commentsLiveData.observe(this, Observer {
                updateView(it)
            })

            vm.addCommentLiveData.observe(this, Observer {
                if (type == "like")
                    updateLikeData(it)
            })

            vm.getIncidentById(incidentId)
            vm.incidentByIdLiveData.observe(this, Observer {
                updateIncidentDetails(it)
            })
        }else Toast.makeText(this,getString(R.string.check_network_connection), Toast.LENGTH_SHORT).show()
    }

    private fun updateIncidentDetails(response: Resource<IncidentByIdResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                if(response.data?.code==CODE_SUCCESS){
                    println("incidentDetails:"+response.data.data)
                    val dataItem= response.data.data?.get(0)!!
                    val subLocalityData = IncidentSubLocality()
                    subLocalityData.name= dataItem.subLocalityName


                  wireDetails =  Incident(   _id =dataItem._id,
                     latitude= dataItem.latitude,
                    longitude= dataItem.longitude,
                    address= dataItem.address,
                     field1Value = dataItem.field1Value,
                     field2Value = dataItem.field2Value,
                     field3Value = dataItem.field3Value,
                     commentCount= dataItem.commentCount,
                     likeCount= dataItem.likeCount,
                     featuredImageUrl= dataItem.featuredImageUrl,
                     isLiked= dataItem.isLiked,
                     // carried through so the redesigned units card and signal
                     // chip can render from wireDetails rather than the raw item
                     respondingUnits= dataItem.respondingUnits,
                     field4Value= dataItem.field4Value,
                     subLocalityDetails= arrayListOf(subLocalityData))
                      //response.data.data
                    isLikeSingle= wireDetails.isLiked
                    bindItems()

                    // redesign: signal chip in the summary card
                    val signal= dataItem.field4Value?.trim().orEmpty()
                    if(signal.isNotEmpty()){
                        binding.tvSignalChip.text= signal
                        binding.tvSignalChip.visible()
                    } else binding.tvSignalChip.gone()

                    // redesign: sub-locality (borough) line
                    val sub= dataItem.subLocalityName?.trim().orEmpty()
                    if(sub.isNotEmpty()){
                        binding.tvSubLocality.text= sub
                        binding.tvSubLocality.visible()
                    } else binding.tvSubLocality.gone()

                    // redesign: units rendered as coloured chips instead of a
                    // single comma-joined label
                    bindUnits()

                    if (!::mMap.isInitialized) {
                        pendingIncidentResponse = response
                        return
                    }else addingMarkerInMap(response.data.data)

                }
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                // Session renewal is silent and owned by TokenAuthenticator. If it could not
                // refresh, it has already cleared the session and routed the user to the login
                // screen with an explanation — so surfacing the raw server string here only
                // stacks a dead-end alert on top of that.
                if (response.message != getString(R.string.token_expired)) {
                    showAlert(response.message)
                }

            }
            else -> {}
        }
    }


    private fun addingMarkerInMap(dataItem: List<IncidentDataByID>?){
        val latLng = LatLng(
            wireDetails.latitude?.toDouble() ?: 0.0,
            wireDetails.longitude?.toDouble() ?: 0.0
        )

        mMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(wireDetails.address)
                .icon(
                    BitmapFromVector(
                        applicationContext,
                        R.drawable.frame
                    )
                )
        )

        if(dataItem?.get(0)?.points?.isNotEmpty() == true) {
            fireStationlist= ArrayList(dataItem.get(0).points!!)
            // Redesign: firehouses render as "Classic Pin" markers — red teardrop
            // plus firehouse glyph, with the station name baked into the bitmap in
            // the design-system label style (iOS parity). Same data and the same
            // markers this lineage already showed, just the new icon treatment.
            fireStationlist.forEach {
                val latLng = LatLng(
                    it.latitude?.toDouble() ?: 0.0,
                    it.longitude?.toDouble() ?: 0.0
                )

                val composed = FirehouseMarker.composedIcon(this, it.name.orEmpty())
                mMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(it.name.toString())
                        .snippet(it.address)
                        .icon(composed.descriptor)
                        // pin tip on the coordinate; the label hangs below it
                        .anchor(composed.anchorU, composed.anchorV))
            }

        }


        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
        mMap.animateCamera(CameraUpdateFactory.zoomTo(19.0f))
    }

    private fun updateLikeData(response: Resource<CommonResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
               // isLikeSingle= !isLikeSingle

                if(isLikeSingle) {
                    likeCount = likeCount + 1
                    binding.ivRating.setImageResource(R.drawable.ic_rating_red)
                }else{
                    likeCount= likeCount-1
                    binding.ivRating.setImageResource(R.drawable.ic_rating)
                }

                if(incidentList.isNotEmpty()){
                    incidentId= wireDetails._id.toString()
                }else {
                    var position = incidentList.indexOfFirst { it._id == wireDetails._id }
                    if (position != -1) updatedPosition = position
                }

                // redesign: bare count beside the icon, no "star/stars" phrasing
                binding.tvLikeCount.text= likeCount.toString()
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                // Session renewal is silent and owned by TokenAuthenticator. If it could not
                // refresh, it has already cleared the session and routed the user to the login
                // screen with an explanation — so surfacing the raw server string here only
                // stacks a dead-end alert on top of that.
                if (response.message != getString(R.string.token_expired)) {
                    showAlert(response.message)
                }

            }
            else -> {}
        }
    }

    private fun initMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun initExtra() {
        val intent= intent.getBundleExtra(IntentUtils.BUN_WIRE_DETAILS)
        if(intent?.containsKey(INCIDENT_ID) == true){
            incidentId = intent.getString(INCIDENT_ID)?:""
        }else {
            wireDetails = intent?.getParcelable(BUN_WIRE_DETAILS) ?: Incident()
            incidentList = intent?.getParcelableArrayList<Incident>(BUN_WIRE_LIST_DETAIL)
                ?: ArrayList<Incident>()
            incidentId= wireDetails._id.toString()
        }
    }

    private fun bindItems() {
        // Redesigned summary card: incident type is a badge, the ADDRESS is the
        // headline (the old layout had a separate tvAddress), field3 is the
        // subtitle and field2 the description.
        binding.tvLevelBadge.text= wireDetails.field1Value
        binding.tvTitle.text= wireDetails.address

        val signal= wireDetails.field4Value?.trim().orEmpty()
        if(signal.isNotEmpty()){
            binding.tvSignalChip.text= signal
            binding.tvSignalChip.visible()
        } else binding.tvSignalChip.gone()

        val subLocality= wireDetails.subLocalityDetails?.firstOrNull()?.name?.trim().orEmpty()
        if(subLocality.isNotEmpty()){
            binding.tvSubLocality.text= subLocality
            binding.tvSubLocality.visible()
        } else binding.tvSubLocality.gone()

        val subTitle= wireDetails.field3Value?.trim().orEmpty()
        if(subTitle.isNotEmpty()){
            binding.tvSubTitle.text= subTitle
            binding.tvSubTitle.visible()
        } else binding.tvSubTitle.gone()

        val desc= wireDetails.field2Value?.trim().orEmpty()
        if(desc.isNotEmpty()){
            binding.tvDesc.text= desc
            binding.tvDesc.visible()
        } else binding.tvDesc.gone()
        // tap to expand/collapse the description
        binding.tvDesc.setOnClickListener {
            binding.tvDesc.maxLines = if (binding.tvDesc.maxLines == 3) Integer.MAX_VALUE else 3
        }

        // like / comment are now plain counts beside icons rather than
        // compound-drawable text, so keep the running likeCount this lineage
        // maintains but render it into tvLikeCount.
        likeCount = wireDetails.likeCount?.takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
        binding.tvLikeCount.text= likeCount.toString()
        binding.tvCommentCount.text=
            if(wireDetails.commentCount.isNullOrEmpty()) "0" else wireDetails.commentCount

        binding.ivRating.setImageResource(
            if(isLikeSingle) R.drawable.ic_rating_red else R.drawable.ic_rating)

        if(!wireDetails.createdAt.isNullOrEmpty())
            binding.tvDateTime.text= DateUtils.formatDateTime(wireDetails.createdAt.toString())

        if(wireDetails.featuredImageUrl.isNullOrEmpty()){
            binding.cardBanner.gone()
        }else{
            binding.cardBanner.visible()
            Glide.with(this)
                .load(wireDetails.featuredImageUrl)
                .into(binding.ivBanner)
            // comment images have always opened full screen; the incident's own
            // featured image never did, on either lineage
            binding.ivBanner.setOnClickListener {
                ImageDialogFragment.newInstance(wireDetails.featuredImageUrl.toString())
                    .show(supportFragmentManager, "image_dialog")
            }
        }

        bindUnits()
    }

    /**
     * Responding units as colour-coded chips (engine / ladder / battalion / …),
     * replacing the single comma-joined tvUnitValue label.
     */
    private fun bindUnits() {
        val units = wireDetails.respondingUnits.orEmpty()
            .mapNotNull { it?.trim()?.uppercase() }
            .filter { it.isNotEmpty() }

        binding.chipGroupUnits.removeAllViews()

        if (units.isEmpty()) {
            binding.tvUnitsCount.gone()
            binding.llUnitsLegend.gone()
            binding.chipGroupUnits.gone()
            binding.tvNoUnits.visible()
            return
        }

        binding.tvUnitsCount.text = units.size.toString()
        binding.tvUnitsCount.visible()
        binding.llUnitsLegend.visible()
        binding.chipGroupUnits.visible()
        binding.tvNoUnits.gone()

        val padH = resources.getDimensionPixelSize(R.dimen.fw_unit_chip_pad_h)
        val padV = resources.getDimensionPixelSize(R.dimen.fw_unit_chip_pad_v)

        units.forEach { unit ->
            val category = UnitCategory.classify(unit)
            val chip = TextView(this).apply {
                text = unit
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.fw_text_caption))
                typeface = ResourcesCompat.getFont(context, R.font.poppins_semibold)
                setTextColor(ContextCompat.getColor(context, category.fgColorRes))
                background = ContextCompat.getDrawable(context, R.drawable.fw_chip_bg)
                backgroundTintList = ContextCompat.getColorStateList(context, category.bgColorRes)
                setPadding(padH, padV, padH, padV)
            }
            binding.chipGroupUnits.addView(chip)
        }
    }

    /**
     * Map type is now a three-segment text control (STREET / SATELLITE / HYBRID)
     * instead of three swapped ImageViews.
     */
    private fun setupMapTypeToggle() {
        val segments = mapOf(
            binding.tvMapHybrid to GoogleMap.MAP_TYPE_HYBRID,
            binding.tvMapStreet to GoogleMap.MAP_TYPE_NORMAL,
            binding.tvMapSatellite to GoogleMap.MAP_TYPE_SATELLITE
        )
        segments.forEach { (segmentView, mapType) ->
            segmentView.setOnClickListener {
                if (!::mMap.isInitialized) return@setOnClickListener
                mMap.mapType = mapType
                segments.keys.forEach { seg ->
                    val selected = seg == segmentView
                    seg.background = if (selected)
                        ContextCompat.getDrawable(this, R.drawable.fw_map_seg_selected)
                    else null
                    seg.setTextColor(
                        ContextCompat.getColor(
                            this,
                            if (selected) R.color.fw_map_seg_text_selected else R.color.white
                        )
                    )
                }
            }
        }
    }

    private fun clickEvent() {
        binding.toolbar.ivFeed.setOnClickListener {
            val intent = Intent(this, FeedsActivity::class.java)
            startActivity(intent)
        }
        binding.toolbar.ivMenu.setOnClickListener {
            prefs.isRecreate=true
            finish()
        }
        binding.ivShare.setOnClickListener {
            val shareContent= wireDetails.field1Value.plus("\n").plus(wireDetails.address).plus("\n").plus(PLAY_STORE_URL)
            shareText(this,shareContent)
        }

        binding.ivRating.setOnClickListener {
            type="like"
            isLikeSingle= !isLikeSingle
            vm.postComment(AddCommentRequest(prefs.userId,wireDetails._id,if(isLikeSingle)"like" else "unlike"))
        }

        // KEPT from this lineage: comments open FeedFilterOrCommentsActivity,
        // which carries the replies/reporting UI. The redesign pointed at the
        // simpler FeedFilterActivity, which would have been a regression.
        binding.ivCommand.setOnClickListener { view->
            val intent= Intent(this, FeedFilterOrCommentsActivity::class.java)
            intent.putExtra(NAV_COMMENT_LIST, wireDetails._id)
            startActivity(intent)
        }

        binding.toolbar.tvToolbarTitle.text = getString(R.string.incident_title)
        binding.toolbar.tvToolbarTitle.visible()
        setupMapTypeToggle()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_in_night))
        mMap.mapType = GoogleMap.MAP_TYPE_HYBRID

        pendingIncidentResponse?.let {
            addingMarkerInMap(it.data?.data)
            pendingIncidentResponse = null
        }

        mMap.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View? {
                // Return null if you don't want to customize the window
                return null
            }

            override fun getInfoContents(marker: Marker): View? {
                val view = layoutInflater.inflate(R.layout.custom_info_window, null)
                val titleTextView = view.findViewById<TextView>(R.id.title)
                val snippetTextView = view.findViewById<TextView>(R.id.snippet)

                if(marker.position.latitude.toString()== wireDetails.latitude && marker.position.longitude.toString()==wireDetails.longitude){
                    titleTextView.text = wireDetails.field1Value
                    snippetTextView.text =wireDetails.address
                    snippetTextView.visible()
                }
                fireStationlist.forEach {
                   if(it.latitude == marker.position.latitude.toString() && it.longitude== marker.position.longitude.toString()){
                       titleTextView.text = it.name
                       snippetTextView.gone()
                   }
                }



                return view
            }
        })
    }

    fun shareText(context: Context, text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)  // Add the text to share
            type = "text/plain"  // Specify the MIME type as text
        }

        // Show a system chooser dialog to pick the app for sharing
        context.startActivity(Intent.createChooser(sendIntent, "Share via"))
    }

    private fun updateView(response: Resource<CommentsResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                try {
                    binding.progress.gone()

                }catch (e:Exception){
                    e.printStackTrace()
                }

            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showAler(response.message)
            }
        }
    }

    fun showAler(message: String? = "") {
        AlertDialog.Builder(this)
            .setTitle(resources.getString(R.string.app_name))
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
            }
            .show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
      //  prefs.isRecreate=true
        val intent = Intent()
        intent.putExtra("wire_detail", updatedPosition)
        intent.putExtra("wire_id", incidentId)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        analyticMethod(INCIDENT_DETAILS,"WireDetailActivity")

        if(prefs.commentCount?.isNotEmpty() == true){
            var count= prefs.commentCount?.toInt()
            if(count?.toInt()!! >1)
                binding.tvCommentCount.text= count.toString()
            else binding.tvCommentCount.text= count.toString()
            prefs.commentCount=""
        }
    }


}