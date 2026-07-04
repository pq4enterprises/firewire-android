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
                     subLocalityDetails= arrayListOf(subLocalityData))
                      //response.data.data
                    isLikeSingle= wireDetails.isLiked
                    bindItems()


                    if(dataItem.respondingUnits?.isNotEmpty() == true){
                        var respondingString =dataItem.respondingUnits.joinToString(", ").filter { it.toString().isNotEmpty() }
                        binding.tvUnitValue.text=  respondingString
                        if(respondingString.isNotEmpty()) binding.tvUnitLabel.visible() else binding.tvUnitLabel.gone()
                    }else{
                        binding.tvUnitLabel.gone()
                    }

                    if (!::mMap.isInitialized) {
                        pendingIncidentResponse = response
                        return
                    }else addingMarkerInMap(response.data.data)

                }
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showAlert(response.message)
                if(response.message==getString(R.string.token_expired)) {
                    startNewActivity(LoginNewActivity::class.java)
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
            fireStationlist.forEach {
                val latLng = LatLng(
                    it.latitude?.toDouble() ?: 0.0,
                    it.longitude?.toDouble() ?: 0.0
                )

                mMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(it.name.toString())
                        .icon(
                            BitmapFromVector(
                                applicationContext,
                                R.drawable.ic_map_fire_station
                            )
                        ))
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
                binding.tvRating.visible()
                binding.pbSmall.gone()

                if(isLikeSingle) {
                    likeCount = likeCount + 1
                    binding.tvRating.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_rating_red,
                        0,
                        0,
                        0
                    )
                }else{
                    likeCount= likeCount-1
                    binding.tvRating.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_rating,0,0,0)
                }

                if(incidentList.isNotEmpty()){
                    incidentId= wireDetails._id.toString()
                }else {
                    var position = incidentList.indexOfFirst { it._id == wireDetails._id }
                    if (position != -1) updatedPosition = position
                }

               // binding.tvLikeCount.text= getString(R.string.star,likeCount.toString())

                binding.tvRating.text= if(likeCount<=1)
                    getString(R.string.star,likeCount.toString())
                else getString(R.string.stars,likeCount.toString())
            }
            ResourceState.ERROR -> {
                binding.tvRating.visible()
                binding.pbSmall.gone()
                binding.progress.gone()
                showAlert(response.message)
                if(response.message==getString(R.string.token_expired)) {
                    startNewActivity(LoginNewActivity::class.java)
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
        binding.tvTitle.text= wireDetails.field1Value

        var subLocalityName= if(wireDetails.subLocalityDetails?.isNotEmpty()==true) ", ".plus(wireDetails.subLocalityDetails?.get(0)?.name) else ""

        binding.tvSubTitle.text= wireDetails.field3Value.plus(subLocalityName)

       //binding.tvSubTitle.text= wireDetails.field2Value
        binding.tvAddress.text= wireDetails.address
        binding.tvDesc.text= wireDetails.field2Value

        //if(wireDetails.field2Value?.isNotEmpty() == true)binding.tvSubTitle.visible() else binding.tvSubTitle.gone()

        if(!wireDetails.likeCount.isNullOrEmpty()) {
            likeCount = wireDetails.likeCount?.toInt() ?: 0
            //binding.tvLikeCount.text = getString(R.string.star, wireDetails.likeCount)

            binding.tvRating.text= if(likeCount<=1)
                getString(R.string.star,wireDetails.likeCount.toString())
            else getString(R.string.stars,wireDetails.likeCount.toString())
        }
        val count= if(wireDetails.commentCount.isNullOrEmpty())"0" else wireDetails.commentCount

        if(count?.toInt()!! >1)
            binding.tvCommand.text= getString(R.string.comments,count)
        else binding.tvCommand.text= getString(R.string.comment,count)


        if(isLikeSingle) {
            binding.tvRating.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_rating_red,
                0,
                0,
                0
            )
        }else{
            binding.tvRating.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_rating,0,0,0)
        }

//        if(wireDetails.field3Value.isNullOrEmpty())binding.tvDesc.gone() else binding.tvDesc.visible()
//        binding.tvDesc.text= wireDetails.field3Value

        if(!wireDetails.createdAt.isNullOrEmpty())
            binding.tvDateTime.text= DateUtils.formatDateTime(wireDetails.createdAt.toString())

        if(wireDetails.featuredImageUrl.isNullOrEmpty()){
            binding.clBannerImage.gone()
          //  binding.tvNoImage.visible()
        }else{
            binding.clBannerImage.visible()
            Glide.with(this)
                .load(wireDetails.featuredImageUrl)
                .into(binding.ivBanner)
            binding.tvNoImage.gone()
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

        binding.flRating.setOnClickListener {
            type="like"
            isLikeSingle= !isLikeSingle
            binding.tvRating.gone()
            binding.pbSmall.visible()
            vm.postComment(AddCommentRequest(prefs.userId,wireDetails._id,if(isLikeSingle)"like" else "unlike"))
        }

        binding.tvCommand.setOnClickListener { view->
            val intent= Intent(this, FeedFilterOrCommentsActivity::class.java)
            intent.putExtra(NAV_COMMENT_LIST, wireDetails._id)
            startActivity(intent)
            //callback?.replaceFragment(NAV_COMMENT_LIST,it._id.toString())
        }

        binding.imgNormal.setOnClickListener{
            mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            binding.imgNormal.gone()
            binding.imgHybrid.gone()
            binding.imgSatellite.visible()
        }
        binding.imgSatellite.setOnClickListener{
            mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            binding.imgNormal.gone()
            binding.imgHybrid.visible()
            binding.imgSatellite.gone()
        }
        binding.imgHybrid.setOnClickListener{
            mMap.mapType= GoogleMap.MAP_TYPE_HYBRID
            binding.imgNormal.visible()
            binding.imgHybrid.gone()
            binding.imgSatellite.gone()
        }
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
                binding.tvCommand.text= getString(R.string.comments,count.toString())
            else binding.tvCommand.text= getString(R.string.comment,count.toString())
            prefs.commentCount=""
        }
    }


}