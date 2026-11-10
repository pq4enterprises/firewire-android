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
import com.pioneer.nycfirewire.utils.MapTypePrefs
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
import com.pioneer.nycfirewire.utils.UnitColors
import android.content.ActivityNotFoundException
import android.content.res.ColorStateList
import android.net.Uri
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.pioneer.nycfirewire.databinding.ItemProgressionRowBinding
import com.pioneer.nycfirewire.model.incident.response.ProgressionEntry
import com.pioneer.nycfirewire.utils.IncidentPage
import com.pioneer.nycfirewire.utils.StreetViewProbe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

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

    // Variation D (map hero)
    /** The Street View availability probe runs once per screen. */
    private var streetViewProbed = false
    /** Top margin of the 360 pill when it shares the row with the map-type toggle. */
    private var streetViewButtonTopMargin = 0

    companion object {
        /** Dim over the map when the hero is fully collapsed. */
        private const val MAP_SCRIM_MAX_ALPHA = 0.45f
        /** Map controls shrink slightly as the hero collapses (mockup: 0.9). */
        private const val MAP_CONTROLS_MIN_SCALE = 0.9f
        /** Height reserved at the top of the map for the toggle / 360 pill. */
        private const val MAP_CONTROLS_HEIGHT_DP = 52f
        /** Extra clearance between the Google logo and the overlapping card. */
        private const val MAP_LOGO_CLEARANCE_DP = 8f
        /** Collapsed strip: distance from its bottom edge to the incident pin's tip. */
        private const val MAP_STRIP_PIN_INSET_DP = 24f

        const val EVENT_360_OPENED = "incident_360_opened"
        const val EVENT_FIREHOUSE_TAPPED = "incident_firehouse_tapped"

        private const val INCIDENT_MARKER_TAG = "incident"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityWireDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupMapHero()
        initMap()

        clickEvent()
        initExtra()
        initViewModel()

        setupMapTouchHandler()
    }


    /**
     * Variation D: the map is a collapsing hero under the top bar.
     *
     *  - the app bar itself is NOT draggable, so a drag that starts on the map
     *    pans the map; only scrolling the cards collapses the hero
     *  - while it collapses (380dp -> 120dp strip, never gone) the map dims,
     *    the controls stay pinned to the top of the visible strip, and the map
     *    is shifted so the incident pin stays visible inside the strip
     */
    private fun setupMapHero() {
        val appBarParams = binding.appBarMap.layoutParams as CoordinatorLayout.LayoutParams
        val appBarBehavior = (appBarParams.behavior as? AppBarLayout.Behavior) ?: AppBarLayout.Behavior()
        appBarBehavior.setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
            override fun canDrag(appBarLayout: AppBarLayout): Boolean = false
        })
        appBarParams.behavior = appBarBehavior

        binding.appBarMap.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { appBar, verticalOffset ->
                val range = appBar.totalScrollRange
                val scrolled = -verticalOffset
                val fraction = if (range > 0) (scrolled.toFloat() / range).coerceIn(0f, 1f) else 0f

                binding.mapScrim.alpha = MAP_SCRIM_MAX_ALPHA * fraction

                // pinned: cancel the hero's own upward movement
                binding.mapControls.translationY = scrolled.toFloat()
                val scale = 1f - (1f - MAP_CONTROLS_MIN_SCALE) * fraction
                binding.llMapTypeToggle.apply {
                    pivotX = 0f; pivotY = 0f
                    scaleX = scale; scaleY = scale
                }
                binding.btnStreetView.apply {
                    pivotX = width.toFloat(); pivotY = 0f
                    scaleX = scale; scaleY = scale
                }

                // The hero slides up under the top bar, leaving its bottom
                // strip visible. Shift the map down inside it so the camera
                // target (the incident pin) ends up in that strip, low enough
                // that the pin (36dp tall, tip on the point) clears the pinned
                // controls above it.
                val heroHeight = binding.mapHero.height
                if (heroHeight > 0 && range > 0) {
                    val pinY = (heroHeight + mapPaddingTop() - mapPaddingBottom()) / 2f
                    val pinYCollapsed = heroHeight - dp(MAP_STRIP_PIN_INSET_DP)
                    val maxShift = (pinYCollapsed - pinY).coerceIn(0f, range.toFloat())
                    binding.mapParallax.translationY = maxShift * fraction
                }
            }
        )

        // 360 pill and map-type toggle share one row; if they would collide
        // (narrow screen / large font scale) the pill drops under the toggle.
        streetViewButtonTopMargin =
            (binding.btnStreetView.layoutParams as FrameLayout.LayoutParams).topMargin
        binding.mapControls.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            binding.mapControls.post { placeStreetViewButton() }
        }

        binding.btnStreetView.setOnClickListener { openStreetView() }

        // Short screens (landscape, split screen, small phones): never let the
        // hero push the cards off screen, since only the cards can collapse it.
        binding.coordinator.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (bottom - top != oldBottom - oldTop) binding.coordinator.post { fitMapHeroToScreen() }
        }
    }

    /**
     * The hero is fw_incident_map_height tall unless that would leave less
     * than fw_incident_min_cards_area for the cards; it never goes below the
     * collapsed strip plus a small scroll range.
     */
    private fun fitMapHeroToScreen() {
        val available = binding.coordinator.height
        if (available <= 0) return
        val designed = resources.getDimensionPixelSize(R.dimen.fw_incident_map_height)
        val strip = resources.getDimensionPixelSize(R.dimen.fw_incident_map_collapsed)
        val minCards = resources.getDimensionPixelSize(R.dimen.fw_incident_min_cards_area)
        val wanted = minOf(designed, maxOf(available - minCards, strip + dp(40f)))
        val params = binding.mapHero.layoutParams
        if (params.height != wanted) {
            params.height = wanted
            binding.mapHero.layoutParams = params
        }
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()

    /** Map padding: keeps the compass clear of the controls ... */
    private fun mapPaddingTop(): Int = dp(MAP_CONTROLS_HEIGHT_DP)

    /** ... and the Google logo / camera centre clear of the overlapping card. */
    private fun mapPaddingBottom(): Int =
        resources.getDimensionPixelSize(R.dimen.fw_incident_map_overlap) + dp(MAP_LOGO_CLEARANCE_DP)

    private fun placeStreetViewButton() {
        val toggle = binding.llMapTypeToggle
        val button = binding.btnStreetView
        if (button.visibility != View.VISIBLE || button.width == 0 || toggle.width == 0) return
        val params = button.layoutParams as FrameLayout.LayoutParams
        val gap = dp(8f)
        val buttonLeft = binding.mapControls.width - params.marginEnd - button.width
        val collides = toggle.right + gap > buttonLeft
        val wantedTop = if (collides) toggle.bottom + gap else streetViewButtonTopMargin
        if (params.topMargin != wantedTop) {
            params.topMargin = wantedTop
            button.layoutParams = params
        }
    }

    /**
     * The map sits in the app bar, outside the scroll view, so nothing should
     * take its gestures; this makes that explicit for every ancestor (the
     * CoordinatorLayout included) from the first touch, then lets the event
     * fall through to the map underneath.
     */
    private fun setupMapTouchHandler() {
        binding.mapOverlay.setOnTouchListener { v, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                v.parent?.requestDisallowInterceptTouchEvent(true)
            }
            false
        }
    }

    /**
     * Shows the 360 pill only when Google reports a panorama near the incident.
     * The check is a free metadata request (no panorama is created here; the
     * billable panorama is created only when the pill is tapped).
     *
     * If the key is not allowed to call the metadata endpoint (DENIED) coverage
     * is unknown, so the pill is shown and StreetViewActivity reports
     * "No street view here" itself when there is none.
     */
    private fun probeStreetView() {
        if (streetViewProbed) return
        val lat = wireDetails.latitude?.toDoubleOrNull() ?: return
        val lng = wireDetails.longitude?.toDoubleOrNull() ?: return
        if (lat == 0.0 && lng == 0.0) return
        streetViewProbed = true
        val appContext = applicationContext
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { StreetViewProbe.check(appContext, lat, lng) }
            if (isFinishing || isDestroyed) return@launch
            if (result == StreetViewProbe.Result.AVAILABLE || result == StreetViewProbe.Result.DENIED) {
                binding.btnStreetView.visible()
            } else {
                binding.btnStreetView.gone()
            }
        }
    }

    private fun openStreetView() {
        val lat = wireDetails.latitude?.toDoubleOrNull() ?: return
        val lng = wireDetails.longitude?.toDoubleOrNull() ?: return
        Firebase.analytics.logEvent(EVENT_360_OPENED, Bundle().apply {
            putString("incident_id", wireDetails._id.orEmpty())
        })
        startActivity(StreetViewActivity.intent(this, lat, lng, wireDetails.address))
    }

    /**
     * INCIDENT PROGRESSION card: one row per title change, newest first, title
     * and time only. Hidden until the incident has had two or more titles.
     */
    private fun bindProgression(progression: List<ProgressionEntry?>?) {
        val rows = IncidentPage.progressionRows(progression)
        val container = binding.llProgressionRows
        container.removeAllViews()
        if (rows.isEmpty()) {
            binding.cardProgression.gone()
            return
        }
        val red = ContextCompat.getColor(this, R.color.fw_red)
        val muted = ContextCompat.getColor(this, R.color.fw_muted)
        val olderBadge = ContextCompat.getColor(this, R.color.fw_surface2)
        val white = ContextCompat.getColor(this, R.color.white)
        rows.forEachIndexed { index, entry ->
            val latest = index == 0
            val row = ItemProgressionRowBinding.inflate(layoutInflater, container, false)
            row.tvProgressionTime.text = DateUtils.formatTime24(entry.at)
            row.tvProgressionTitle.text = entry.title?.trim()
            row.tvProgressionTitle.backgroundTintList =
                ColorStateList.valueOf(if (latest) red else olderBadge)
            row.tvProgressionTitle.setTextColor(if (latest) white else muted)
            row.vProgressionDot.backgroundTintList =
                ColorStateList.valueOf(if (latest) red else muted)
            // connector runs dot to dot: nothing above the first, nothing below the last
            row.vProgressionLineTop.visibility = if (index == 0) View.INVISIBLE else View.VISIBLE
            row.vProgressionLineBottom.visibility =
                if (index == rows.lastIndex) View.INVISIBLE else View.VISIBLE
            container.addView(row.root)
        }
        binding.cardProgression.visible()
    }

    /** Firehouse callout tapped: hand off to turn-by-turn, or any maps app. */
    private fun openDirections(point: Points) {
        val lat = point.latitude?.toDoubleOrNull() ?: return
        val lng = point.longitude?.toDoubleOrNull() ?: return
        val coords = String.format(Locale.US, "%.6f,%.6f", lat, lng)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$coords")))
        } catch (e: ActivityNotFoundException) {
            try {
                val label = Uri.encode(point.name.orEmpty())
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$coords?q=$coords($label)")))
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(this, getString(R.string.directions_unavailable), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logFirehouseTapped(point: Points, action: String) {
        Firebase.analytics.logEvent(EVENT_FIREHOUSE_TAPPED, Bundle().apply {
            putString("incident_id", wireDetails._id.orEmpty())
            putString("firehouse", point.name.orEmpty().take(100))
            putString("action", action)
            point.distanceMiles?.let { putDouble("distance_miles", it) }
        })
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
                     respondingUnitColors= dataItem.respondingUnitColors,
                     unitLegend= dataItem.unitLegend,
                     unitColorsConfigured= dataItem.unitColorsConfigured,
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

                    // Variation D: title history card + 360 availability
                    bindProgression(dataItem.progression)
                    probeStreetView()

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
        )?.tag = INCIDENT_MARKER_TAG

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
                        // "0.4 mi · 74 Middagh St" (distance from the server, nearest first)
                        .snippet(IncidentPage.firehouseSnippet(it.distanceMiles, it.address))
                        .icon(composed.descriptor)
                        // pin tip on the coordinate; the label hangs below it
                        .anchor(composed.anchorU, composed.anchorV))
                    // the marker carries its firehouse: info window + directions read it back
                    ?.tag = it
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
        binding.chipGroupUnits.visible()
        binding.tvNoUnits.gone()

        val padH = resources.getDimensionPixelSize(R.dimen.fw_unit_chip_pad_h)
        val padV = resources.getDimensionPixelSize(R.dimen.fw_unit_chip_pad_v)
        val dark = UnitColors.isDarkMode(this)
        val surface = ContextCompat.getColor(this, R.color.fw_surface)

        // Server-resolved colors (per-locality scheme, editable in the portal).
        // Only when the field is present; otherwise the hardcoded classifier.
        val serverColors = wireDetails.respondingUnitColors
        val byUnit = serverColors?.associateBy { it.unit?.trim()?.uppercase().orEmpty() }

        units.forEach { unit ->
            val server = byUnit?.get(unit)
            val (bgColor, fgColor) = if (serverColors != null) {
                UnitColors.chipColors(this, server, dark)
            } else {
                // Text follows the background (black/white by contrast) in light and dark.
                val bg = ContextCompat.getColor(this, UnitCategory.classify(unit).bgColorRes)
                Pair(bg, UnitColors.contrastingText(bg, surface))
            }
            val chip = TextView(this).apply {
                text = unit
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.fw_text_caption))
                typeface = ResourcesCompat.getFont(context, R.font.poppins_semibold)
                setTextColor(fgColor)
                background = ContextCompat.getDrawable(context, R.drawable.fw_chip_bg)
                backgroundTintList = android.content.res.ColorStateList.valueOf(bgColor)
                setPadding(padH, padV, padH, padV)
            }
            binding.chipGroupUnits.addView(chip)
        }

        bindUnitLegend(serverColors != null, dark)
    }

    /**
     * Legend row. With server colors the legend comes from the scheme (and is
     * hidden when the locality has none, i.e. everything is gray). Without
     * server colors the static XML legend (engine / truck / marine / chief /
     * SOC) is shown as before.
     */
    private fun bindUnitLegend(serverMode: Boolean, dark: Boolean) {
        val legend = binding.llUnitsLegend
        if (!serverMode) {
            if (legend.tag == "server") { legend.removeAllViews(); legend.tag = null; layoutInflater.inflate(R.layout.view_units_legend_static, legend, true) }
            legend.visible()
            return
        }
        val items = wireDetails.unitLegend.orEmpty()
        legend.removeAllViews()
        legend.tag = "server"
        if (items.isEmpty()) { legend.gone(); return }
        val dot = resources.getDimensionPixelSize(R.dimen.fw_legend_dot_size)
        val density = resources.displayMetrics.density
        // one wrapping item per category: dot + label kept together (ChipGroup is a FlowLayout)
        items.forEach { item ->
            val row = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = android.view.ViewGroup.MarginLayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            val swatch = android.view.View(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(dot, dot)
                background = ContextCompat.getDrawable(context, R.drawable.fw_legend_dot)
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    UnitColors.parseHex(if (dark) item.colorDark ?: item.color else item.color) ?: ContextCompat.getColor(context, R.color.fw_muted))
            }
            val label = TextView(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT).also { it.marginStart = (4 * density).toInt() }
                text = item.label.orEmpty()
                setTextColor(ContextCompat.getColor(context, R.color.fw_muted))
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.fw_text_legend))
                typeface = ResourcesCompat.getFont(context, R.font.poppins_semibold)
                maxLines = 1
            }
            row.addView(swatch); row.addView(label)
            legend.addView(row)
        }
        legend.visible()
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
                // Temporary override for this screen only; the persisted default
                // lives in My Account > Map Type.
                mMap.mapType = mapType
                highlightMapTypeSegment(mapType)
            }
        }
        // Initial highlight follows the user's saved preference (hybrid when unset).
        highlightMapTypeSegment(MapTypePrefs.googleMapType(GoogleMap.MAP_TYPE_HYBRID))
    }

    private fun highlightMapTypeSegment(mapType: Int) {
        val segments = mapOf(
            binding.tvMapHybrid to GoogleMap.MAP_TYPE_HYBRID,
            binding.tvMapStreet to GoogleMap.MAP_TYPE_NORMAL,
            binding.tvMapSatellite to GoogleMap.MAP_TYPE_SATELLITE
        )
        segments.forEach { (seg, type) ->
            val selected = type == mapType
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
        // Saved default from My Account; hybrid is what this screen always forced,
        // so it stays the fallback for users who never picked one.
        MapTypePrefs.apply(mMap, GoogleMap.MAP_TYPE_HYBRID)
        highlightMapTypeSegment(mMap.mapType)
        // Before any camera move: the camera target is centred in the padded
        // area, i.e. between the on-map controls and the overlapping card.
        mMap.setPadding(0, mapPaddingTop(), 0, mapPaddingBottom())

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
                val view = layoutInflater.inflate(R.layout.info_window_incident_map, null)
                val titleTextView = view.findViewById<TextView>(R.id.tv_info_title)
                val snippetTextView = view.findViewById<TextView>(R.id.tv_info_snippet)
                val directionsTextView = view.findViewById<TextView>(R.id.tv_info_directions)

                val firehouse = marker.tag as? Points
                if (firehouse != null) {
                    // name, "0.4 mi · address", and the hint that a tap opens directions
                    titleTextView.text = firehouse.name
                    val snippet = marker.snippet.orEmpty()
                    snippetTextView.text = snippet
                    snippetTextView.visibility = if (snippet.isEmpty()) View.GONE else View.VISIBLE
                    directionsTextView.visible()
                } else {
                    titleTextView.text = wireDetails.field1Value
                    snippetTextView.text = wireDetails.address
                    snippetTextView.visibility =
                        if (wireDetails.address.isNullOrBlank()) View.GONE else View.VISIBLE
                    directionsTextView.gone()
                }
                return view
            }
        })

        // Firehouse pin: default behaviour (centre + callout) plus the event.
        mMap.setOnMarkerClickListener { marker ->
            (marker.tag as? Points)?.let { logFirehouseTapped(it, "pin") }
            false
        }
        // Firehouse callout: open directions to it.
        mMap.setOnInfoWindowClickListener { marker ->
            (marker.tag as? Points)?.let {
                logFirehouseTapped(it, "directions")
                openDirections(it)
            }
        }
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