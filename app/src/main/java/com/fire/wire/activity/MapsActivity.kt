package com.fire.wire.activity

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import com.fire.wire.ReplaceCallback
import com.fire.wire.databinding.ActivityHomeBinding
import com.fire.wire.fragment.FilterFragment
import com.fire.wire.fragment.NewsFragment
import com.fire.wire.fragment.WireFragment
import com.fire.wire.model.incident.response.Incident
import com.fire.wire.model.incident.response.IncidentResponse
import com.fire.wire.resource.Resource
import com.fire.wire.resource.ResourceState
import com.fire.wire.utils.*
import com.fire.wire.viewModel.FireWireViewModel
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.Toast
import com.fire.wire.R
import com.fire.wire.callback.CallbackFunctions
import com.fire.wire.fragment.CommentsFragment
import com.fire.wire.utils.IntentUtils.FROM_WIRE
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.onesignal.OneSignal
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.ArrayList;


@AndroidEntryPoint
class MapsActivity : BaseActivity(), OnMapReadyCallback , ReplaceCallback,CallbackFunctions{

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityHomeBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private var isWireView =false
    private var isNewsView =false
    private val mTAG= MapsActivity::class.java.canonicalName
    private var incidentList= ArrayList<Incident>()
    private val locationArrayList= ArrayList<LatLng>()

    private lateinit var vm: FireWireViewModel
    private var isNewsFragment= false
    private var isWireFragment= false
    private var isCommentFragment= false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomConstraint)
//        bottomSheetBehavior.peekHeight=50
      // bottomSheetBehavior.isFitToContents=false
        bottomSheetBehavior.isDraggable=true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
//        bottomSheetBehavior.halfExpandedRatio=0.5f

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        initViews()
        initBinding()

        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)
        callApi()
    }



    private fun callApi() {
        var locality= ArrayList<String>()
        var subLocality= ArrayList<String>()
        vm.getIncidentList(locality,subLocality)
        vm.incidentLiveData.observe(this, Observer {
           updateIncidentList(it)
        })
    }

    private fun updateIncidentList(response: Resource<IncidentResponse>?) {
        when(response?.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                response.data?.let { it1 ->
                    val list= it1.data?.data?: emptyList()
                    incidentList= ArrayList(list)
                    displayFragment(WireFragment.newInstance(incidentList,  Bundle()),false)
                    formLatLng()
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

    private fun formLatLng() {
        incidentList.forEach {
            val latLng = LatLng(it.latitude?.toDouble()?:0.0, it.longitude?.toDouble()?:0.0)
            locationArrayList.add(latLng)
        }
        mMap.clear()

        locationArrayList.forEachIndexed { i, latLng ->

            mMap.addMarker(
                MarkerOptions()
                    .position(locationArrayList[i])
                    .title(incidentList[i].address)
                    .icon(
                        BitmapFromVector(
                            applicationContext,
                            com.fire.wire.R.drawable.ic_fire
                        )
                    )
            )

            mMap.animateCamera(CameraUpdateFactory.zoomTo(18.0f))

            mMap.moveCamera(CameraUpdateFactory.newLatLng(locationArrayList[i]))
        }

    }

    private fun initViews() {
        onChangeOption()
        clickEvent()

        CoroutineScope(Dispatchers.IO).launch {
            OneSignal.Notifications.requestPermission(false)
        }

        OneSignal.Notifications.addClickListener(clickListener)
        OneSignal.Notifications.removeClickListener(clickListener)
    }

    private fun clickEvent() {
        binding.toolbarLayout.ivMenu.setOnClickListener {
            val intent = Intent(this,NavigationMenuActivity::class.java )
            startActivity(intent)
        }

        binding.toolbarLayout.ivFeed.setOnClickListener {
            val intent = Intent(this,FeedsActivity::class.java )
            startActivity(intent)
        }
    }


    private fun initBinding() {

        bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(view: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        binding.appBarLayout.setBackgroundColor(resources.getColor(R.color.fw_surface))
                        binding.cvList.visible()
                        binding.cvMap.gone()
                        transparentToolbar()
                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                       whiteToolbar()
                        binding.llTopView.visible()
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        binding.appBarLayout.setBackgroundColor(resources.getColor(R.color.fw_surface))
                        binding.appBarLayout.visible()
                        binding.cvList.gone()
                        binding.cvMap.visible()
                        transparentToolbar()
                    }
                    BottomSheetBehavior.STATE_SETTLING -> {
                        binding.appBarLayout.setBackgroundColor(resources.getColor(R.color.fw_surface))
                        transparentToolbar()
                        binding.cvMap.visible()
                    }
                    BottomSheetBehavior.STATE_DRAGGING -> {
                        binding.cvMap.visible()
                        binding.llTopView.gone()

                    }

                }
            }

            override fun onSlide(view: View, v: Float) {}
        })

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_in_night));
    }

    private fun onChangeOption(){
        val changeView= binding.toolbarLayout.itemChange

        changeView.tvWire.setOnClickListener {
            changeView.cvWire.visible()
            changeView.cvNews.gone()
            changeView.tvNews.visible()
            changeView.tvWire.gone()
            isWireView= true
            isNewsView=false
            changeFragment()
        }

        changeView.tvNews.setOnClickListener {
            changeView.cvNews.visible()
            changeView.cvWire.gone()
            changeView.tvWire.visible()
            changeView.tvNews.gone()
            isNewsView= true
            isWireView=false
            changeFragment()
        }

        binding.cvList.setOnClickListener {
            bottomSheetBehavior.state= BottomSheetBehavior.STATE_EXPANDED
            binding.cvList.gone()
            binding.bottomCl.visible()
            changeFragment()

        }

        binding.cvMap.setOnClickListener {
            bottomSheetBehavior.state= BottomSheetBehavior.STATE_HIDDEN
        }

    }

    private fun displayFragment(fragment: Fragment, flag:Boolean){
        replaceFragment(
            fragment,
            mTAG,
            allowStateLoss = true,
            containerViewId = R.id.fl_main,
            allowBackStack = flag
        )
        if(fragment is CommentsFragment) binding.cvMap.gone()
    }

    override fun replaceFragment(receivingType: String, data: Any) {
        when(receivingType){
            NAV_FILTER ->{
                displayFragment(FilterFragment.newInstance(),true)
            }
            NAV_WIRE->{
                displayFragment(WireFragment.newInstance(incidentList, data as Bundle),false)
            }
            NAV_NEWS->{
                displayFragment(NewsFragment.newInstance(),true)
            }
            NAV_COMMENT_LIST->{
                displayFragment(CommentsFragment.newInstance(data as String),true)
            }
            NAV_WIRE_NEWS->{
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                val bundle = data as Bundle
                if(bundle.containsKey(FROM_WIRE)){
                    isWireView= bundle.getBoolean(FROM_WIRE)
                }

                if(isWireView){
                    displayFragment(WireFragment.newInstance(incidentList,data as Bundle),false)
                }else  displayFragment(NewsFragment.newInstance(),true)
            }
        }
    }

    private fun changeFragment(){
        if(isWireView){
            displayFragment(WireFragment.newInstance(incidentList, Bundle()),false)
        }else if(isNewsView){
            displayFragment(NewsFragment.newInstance(),true)
        }
        if(bottomSheetBehavior.state== STATE_EXPANDED)
        binding.cvMap.visible()
    }

    fun ImageView.setTint(@ColorRes colorRes: Int) {
        ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(ContextCompat.getColor(context, colorRes)))
        }

    // 2026 design system: header is always a solid surface bar with text-tinted
    // icons (both light and dark mode); only the sheet-state side effects differ.
    private fun whiteToolbar(){
        binding.appBarLayout.setBackgroundColor(resources.getColor(R.color.fw_surface))
        binding.toolbarLayout.ivFeed.setTint(R.color.fw_text)
        binding.toolbarLayout.ivMenu.setTint(R.color.fw_text)
    }

    private fun transparentToolbar(){
        binding.appBarLayout.setBackgroundColor(resources.getColor(R.color.fw_surface))
        binding.toolbarLayout.ivFeed.setTint(R.color.fw_text)
        binding.toolbarLayout.ivMenu.setTint(R.color.fw_text)
        binding.llTopView.gone()
    }


    private fun BitmapFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        // below line is use to generate a drawable.
        val vectorDrawable = ContextCompat.getDrawable(
            context, vectorResId
        )

        // below line is use to set bounds to our vector
        // drawable.
        vectorDrawable!!.setBounds(
            0, 0, vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight
        )

        // below line is use to create a bitmap for our
        // drawable which we have added.
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

        // below line is use to add bitmap in our canvas.
        val canvas = Canvas(bitmap)

        // below line is use to draw our
        // vector drawable in canvas.
        vectorDrawable.draw(canvas)

        // after generating our bitmap we are returning our
        // bitmap.
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val f: Fragment = supportFragmentManager.findFragmentById(R.id.fl_main)!!

        when{
            f is WireFragment-> binding.cvMap.visible()
            f is NewsFragment-> binding.cvMap.visible()
        }

    }

    override fun filterApply(incidentList: ArrayList<Incident>) {
        this.incidentList= incidentList
        locationArrayList.clear()
        formLatLng()
    }

    val clickListener = object : INotificationClickListener {
        override fun onClick(event: INotificationClickEvent) {
            // respond to click
            Toast.makeText(this@MapsActivity, "Notification clicked", Toast.LENGTH_SHORT).show()
        }
    }

}