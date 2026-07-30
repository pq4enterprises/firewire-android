package com.pioneer.nycfirewire.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
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
import com.pioneer.nycfirewire.ReplaceCallback
import com.pioneer.nycfirewire.fragment.FilterFragment
import com.pioneer.nycfirewire.fragment.NewsFragment
import com.pioneer.nycfirewire.fragment.WireFragment
import com.pioneer.nycfirewire.model.incident.response.Incident
import com.pioneer.nycfirewire.model.incident.response.IncidentResponse
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.viewModel.FireWireViewModel
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.getkeepsafe.taptargetview.TapTargetView
import com.pioneer.nycfirewire.callback.CallbackFunctions
import com.pioneer.nycfirewire.fragment.CommentsFragment
import com.pioneer.nycfirewire.utils.IntentUtils.FROM_WIRE
import com.google.android.gms.maps.model.BitmapDescriptor
import com.onesignal.OneSignal
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.ActivityHomeBinding
import com.pioneer.nycfirewire.model.locality.Locality
import com.pioneer.nycfirewire.model.locality.LocalityResponse
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.utils.Constants
import com.pioneer.nycfirewire.utils.Constants.LOCALITY_DATA
import com.pioneer.nycfirewire.utils.IntentUtils.BUN_WIRE_DETAILS
import com.pioneer.nycfirewire.utils.NAV_COMMENT_LIST
import com.pioneer.nycfirewire.utils.NAV_FILTER
import com.pioneer.nycfirewire.utils.NAV_NEWS
import com.pioneer.nycfirewire.utils.NAV_WIRE
import com.pioneer.nycfirewire.utils.NAV_WIRE_NEWS
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.replaceFragment
import com.pioneer.nycfirewire.utils.startNewActivity
import com.pioneer.nycfirewire.utils.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.ArrayList;
import com.google.android.gms.maps.model.Marker
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.pioneer.nycfirewire.fragment.WireFragmentWithPagination
import com.pioneer.nycfirewire.listener.OnIncidentListLoadedListener
import com.pioneer.nycfirewire.model.incident.response.FilterData
import com.pioneer.nycfirewire.model.user.request.ProfileUpdateRequest
import com.pioneer.nycfirewire.model.user.response.CommonResponse
import com.pioneer.nycfirewire.model.user.response.UserDetails
import com.pioneer.nycfirewire.model.user.response.UserResponse
import com.pioneer.nycfirewire.utils.AppUtils.isNetworkConnected
import com.pioneer.nycfirewire.utils.Constants.FROM_START
import com.pioneer.nycfirewire.utils.Constants.USER_BASIC_USER
import com.pioneer.nycfirewire.utils.Constants.USER_PREMIUM_FREE
import com.pioneer.nycfirewire.utils.DateUtils.getExpiryTime
import com.pioneer.nycfirewire.utils.IntentUtils.BUN_WIRE_LIST_DETAIL
import com.pioneer.nycfirewire.utils.IntentUtils.EXTRA_WIRE_DETAILS
import com.pioneer.nycfirewire.utils.IntentUtils.FILTER_DATA
import com.pioneer.nycfirewire.utils.showToast


@AndroidEntryPoint
class MapsActivity : BaseActivity(), OnMapReadyCallback , ReplaceCallback, CallbackFunctions,PurchasesUpdatedListener,OnIncidentListLoadedListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityHomeBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private var isWireView =false
    private var isNewsView =false
    private val mTAG= MapsActivity::class.java.canonicalName
    private var incidentList= ArrayList<Incident>()
    private val locationArrayList= ArrayList<LatLng>()
    private lateinit var billingClient: BillingClient
    private var profileDetails= UserDetails()
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val UPDATE_REQUEST_CODE = 123
    private lateinit var appUpdateManager: AppUpdateManager



    private lateinit var vm: FireWireViewModel
    private var totalCount= ""
    private var isNeedBsButton= true
    private var userRole= prefs.userRole.toString()

    private lateinit var mapFragment : SupportMapFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        FirebaseApp.initializeApp(this)
        firebaseAnalytics = Firebase.analytics

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(!prefs.isLogin)
            startNewActivity(LoginNewActivity::class.java)
        else {

            createCall()

            appUpdateManager = AppUpdateManagerFactory.create(this)
            val appUpdateInfoTask = appUpdateManager.appUpdateInfo

            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    appUpdateInfo.isUpdateTypeAllowed(IMMEDIATE)) {

                    // Start the update
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            IMMEDIATE,
                            this,
                            UPDATE_REQUEST_CODE
                        )
                    } catch (e: IntentSender.SendIntentException) {
                        e.printStackTrace()
                    }
                }
            }

        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UPDATE_REQUEST_CODE && resultCode != RESULT_OK) {
            // User denied the update, exit or block access
            Toast.makeText(this, "Update required to use the app", Toast.LENGTH_SHORT).show()
            finish()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    fun createCall(){
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if(prefs.showHomeIntro && prefs.showAppIntro){
            introducingApp()
        }else callAllInitiateAction()
    }

    private fun callAllInitiateAction(){
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)

        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomConstraint)
//        bottomSheetBehavior.peekHeight=50
        // bottomSheetBehavior.isFitToContents=false
        bottomSheetBehavior.isDraggable = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED


        initViews()
        initBinding()

        initExtra()

        prefs.isRecreate = false
    }

    override fun onResume() {
        super.onResume()
        //mapFragment.onResume()
        val currentFragment = supportFragmentManager.fragments.lastOrNull { it.isVisible }

        if(currentFragment is WireFragment){
            val bundle = Bundle()
            bundle.putBoolean(FROM_WIRE, true)
            replaceFragment(NAV_WIRE_NEWS, bundle)
            //currentFragment.refreshData()
        }
    }

    override fun onPause() {
      //  mapFragment.onPause()
        super.onPause()
    }



    private fun introducingApp() {
       if(prefs.showAppIntro && prefs.showHomeIntro) {
           TapTargetSequence(this)
               .targets(
                   TapTarget.forView(binding.toolbarLayout.ivFeed, "RADIO","Listen to scanner feeds"),
                   TapTarget.forView(
                       binding.toolbarLayout.itemChange.tvNews,"NEWS", "News, Updates, Articles from FIRE WIRE"
                   ),
                   TapTarget.forView(
                       binding.toolbarLayout.ivMenu,
                       "Edit Profile",
                       "My Account -> Update Profile"
                   ),
                   TapTarget.forView(
                       binding.toolbarLayout.ivMenu,
                       "Notification",
                       "Personalization -> Notification"
                   ),
                   TapTarget.forView(
                       binding.toolbarLayout.ivMenu,
                       "Submit a tip",
                       "Click menu option to submit a tip"
                   )
                       .dimColor(android.R.color.black)
                       .outerCircleColor(R.color.app_red)
                       .targetCircleColor(R.color.white)
                       .textColor(android.R.color.black)
                       .targetRadius(20)
               )
               .listener(object : TapTargetSequence.Listener {
                   override fun onSequenceFinish() {
                       // Yay
                      // Toast.makeText(this@MapsActivity, "App tour completed!", Toast.LENGTH_SHORT).show()
                       prefs.showHomeIntro=false
                       callAllInitiateAction()
                   }

                   override fun onSequenceStep(lastTarget: TapTarget, targetClicked: Boolean) {
                       // Action for each step
                       //Toast.makeText(this@MapsActivity, "action next", Toast.LENGTH_SHORT).show()
                   }

                   override fun onSequenceCanceled(lastTarget: TapTarget) {
                       // Boo
                     prefs.showHomeIntro=false
                       callAllInitiateAction()
                   }
               })
               .start()
       }
    }

    var cameraIdleListener = GoogleMap.OnCameraIdleListener {
        // Check if the map zoom is at a certain level
        val zoomLevel = mMap.cameraPosition.zoom

        // If zoomed in, you can show the map at the top of the screen
        if (zoomLevel > 10) {
            // Adjust the bottom sheet's visibility or height
            bottomSheetBehavior.setPeekHeight((binding.flFragmentContainer.height * 0.4).toInt())

        }
    }




    private fun initExtra() {
        if(intent!=null && intent.hasExtra(LOCALITY_DATA)){
            var localityList= intent.getParcelableArrayListExtra<Locality>(LOCALITY_DATA)
            if(localityList.isNullOrEmpty()) callApi() else callIncidentList(localityList!!)
        }else   callApi()



        billingClient = BillingClient.newBuilder(this)
            .enablePendingPurchases()
            .setListener(this)
            .build()


        billingClient.startConnection(object : BillingClientStateListener {
            override
            fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    vm.getUserDetails()
                   // checkIfUserAlreadyPurchased()
                }
            }

            override fun onBillingServiceDisconnected() {
            }
        }
        )


        vm.userLiveData.observe(this, Observer {
            updateUserDetails(it)
        })

        vm.updateProfileLiveData.observe(this, Observer {
            showUpdateResponse(it)
        })


    }

    private fun checkIfUserAlreadyPurchased() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS) // or SUBS if subscription
                .build()
        ) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (purchasesList.isNullOrEmpty()) {
                    // No active subscriptions
                    Log.d("SubscriptionCheck", "No active subscriptions")
                } else {
                    for (purchase in purchasesList) {
                       //if (purchase.products.contains(SUB_PRODUCT_ID)) {
                            val isAutoRenewing = purchase.isAutoRenewing
                            val expiryTime = getExpiryTime(purchase.purchaseTime)
                            val currentTime = System.currentTimeMillis()
                            val isExpired = !isAutoRenewing && (expiryTime < currentTime)
                            if (isExpired && prefs.userRole == USER_PREMIUM_FREE) {
                                Log.d("SubscriptionCheck", "Subscription is expired")
                                    userApiCall(USER_BASIC_USER)
                            } else if(prefs.userRole == USER_BASIC_USER){
                                    userApiCall(USER_PREMIUM_FREE)
                                Log.d("SubscriptionCheck", "Subscription is active")
                            }
                       // }
                    }


                }} else {
                //showSnack("Error checking purchases: ${billingResult.debugMessage}")
            }
        }
    }

    private fun userApiCall(role: String){
        userRole= role

        val postRequest= ProfileUpdateRequest(
            firstName=profileDetails.firstName,
            lastName=profileDetails.lastName,
            email= profileDetails.email,
            mobile= profileDetails.mobile,
            title=profileDetails.title,
            img=profileDetails.img,
            role = userRole
        )

        vm.updateProfileData(postRequest)

      //  vm.getUserDetails()

    }

    private fun showUpdateResponse(response: Resource<CommonResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                prefs.userRole= userRole
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showAlert(response.message)
            }
        }
    }


    private fun updateUserDetails(response: Resource<UserResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                response.data?.let { it1 ->
                    profileDetails= it1.data?: UserDetails()
                    checkIfUserAlreadyPurchased()
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

    private fun callIncidentList(localityList : ArrayList<Locality>){
        var localityId= ArrayList<String>()
        var subLocalityId= ArrayList<String>()
        var list= localityList
        list.forEach {
            it.subLocality?.forEach { sub->
                if(sub.isChecked){
                    localityId.add(it._id.toString())
                    subLocalityId.add(sub._id.toString())
                }
            }
        }
        prefs.localityIds= localityId

        if(localityId.isNotEmpty() || subLocalityId.isNotEmpty()){

            displayFragment(WireFragment.newInstance(incidentList,totalCount,  Bundle()),false)
            /*if(isNetworkConnected(this)) {
                vm.getIncidentList(ArrayList(localityId.distinct()), subLocalityId, "1", "10")
            }else showSnack(getString(R.string.check_network_connection))
            vm.incidentLiveData.observe(this, Observer {
                updateIncidentList(it)
            })*/
        }else{
            val intent= Intent(this, SelectAreaActivity::class.java)
            intent.putExtra(FROM_START,true)
            startActivity(intent)
        }

    }


    private fun callApi() {
        if(isNetworkConnected(this)) {
            vm.getLocalityList(Constants.TYPE_AREA)
        }else getString(R.string.check_network_connection)
        vm.localityLiveData.observe(this) {
            updateFilterData(it)
        }
    }

    private fun updateFilterData(response: Resource<LocalityResponse>) {
        when (response.state) {
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                if (response.data?.code == Constants.CODE_SUCCESS) {
                    val localityList = response.data.data
                    var list= ArrayList(localityList?.data?: ArrayList())
                    callIncidentList(list)
                } else {
                    showAlert(response.data?.message.toString())
                }


            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showSnack("Error occured")

            }
            else -> {}
        }
    }

    private fun updateIncidentList(response: Resource<IncidentResponse>?) {
        when(response?.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                response.data?.let { it1 ->
                    val list= it1.data?.data?: emptyList()
                    incidentList= ArrayList(list)
                    totalCount=  it1.data?.pageInfo?.totalCount.toString()
                    if(incidentList.isNotEmpty()) formLatLng()
                    binding.toolbarLayout.ivRefresh.visible()
                    displayFragment(WireFragment.newInstance(incidentList,totalCount,  Bundle()),false)

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
                    //.title(incidentList[i].address)
                    .icon(
                        BitmapFromVector(
                            applicationContext,
                            R.drawable.frame
                        )
                    )
            )!!
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

                if(incidentList.isNotEmpty()) {
                    var field1Value = incidentList[0].field1Value
                    var address = incidentList[0].address

                    incidentList.forEach {
                        if (it.latitude == marker.position.latitude.toString() && it.longitude == marker.position.longitude.toString()) {
                            field1Value = it.field1Value
                            address = it.address
                        }
                    }

                    titleTextView.text = field1Value
                    snippetTextView.text = address
                }

                return view
            }
        })


        mMap.setOnInfoWindowClickListener { marker ->
            // Handle the info window click here
            var wireDetail= incidentList.find { it.latitude==marker.position.latitude.toString() && it.longitude==marker.position.longitude.toString() }
            val bundle= Bundle()
            bundle.putParcelable(BUN_WIRE_DETAILS,wireDetail)
            bundle.putParcelableArrayList(BUN_WIRE_LIST_DETAIL, incidentList)
            val intent = Intent(this, WireDetailActivity::class.java)
            intent.putExtra(BUN_WIRE_DETAILS, bundle)
            startActivity(intent)

        }


        if(locationArrayList.isNotEmpty()) {
            val latLng = locationArrayList[0]
            mMap.setOnCameraIdleListener(cameraIdleListener)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            mMap.animateCamera(CameraUpdateFactory.scrollBy(0f, 400f))
        }

    }

    private fun initViews() {
        onChangeOption()
        clickEvent()


        CoroutineScope(Dispatchers.IO).launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this@MapsActivity, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                    OneSignal.Notifications.requestPermission(false); // 'false' respects previous denials
                }
            }

        }

        OneSignal.Notifications.addClickListener(clickListener)
        OneSignal.Notifications.removeClickListener(clickListener)
    }

    private fun clickEvent() {
        binding.toolbarLayout.ivMenu.setOnClickListener {
            val currentFragment = supportFragmentManager.fragments.lastOrNull { it.isVisible }
            if(currentFragment is FilterFragment){
                val fragmentManager = supportFragmentManager
                fragmentManager.beginTransaction()
                    .remove(currentFragment)  // removes current fragment
                    .commitAllowingStateLoss()
            }
            val intent = Intent(this, NavigationMenuActivity::class.java )
            startActivity(intent)
        }

        binding.toolbarLayout.ivFeed.setOnClickListener {
            val intent = Intent(this, FeedsActivity::class.java )
            startActivity(intent)
        }

        binding.toolbarLayout.ivRefresh.setOnClickListener {
            val bundle = Bundle()
            bundle.putBoolean(FROM_WIRE, true)
            replaceFragment(NAV_WIRE_NEWS, bundle)
            /*val currentFragment = supportFragmentManager.fragments.lastOrNull { it.isVisible }

            if(currentFragment is WireFragment){
                currentFragment.refreshData()
                showToast(this,"Incident refreshed")
            }*/
        }
    }


    private fun initBinding() {

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(view: View, newState: Int) {
                if(isNeedBsButton){
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        binding.appBarLayout.setBackgroundColor(resources.getColor(R.color.transparent_color))
                        binding.cvList.visible()
                        binding.cvMap.gone()
                        whiteToolbar()
                        //transparentToolbar()
                    }

                    BottomSheetBehavior.STATE_EXPANDED -> {
                        whiteToolbar()
                        binding.llTopView.visible()
                    }

                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        binding.appBarLayout.setBackgroundColor(resources.getColor(R.color.transparent_color))
                        binding.appBarLayout.visible()
                        binding.cvList.gone()
                        binding.cvMap.visible()
                        whiteToolbar()
                        // transparentToolbar()
                    }

                    BottomSheetBehavior.STATE_SETTLING -> {
                        binding.appBarLayout.setBackgroundColor(resources.getColor(R.color.transparent_color))
                        whiteToolbar()
                        // transparentToolbar()
                        binding.cvMap.visible()
                    }

                    BottomSheetBehavior.STATE_DRAGGING -> {
                        binding.cvMap.visible()
                        binding.llTopView.gone()
                    }
                }
                    }
            }

            override fun onSlide(view: View, v: Float) {}
        }

        )

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

       // if(prefs.isDarkMode) {
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_in_night));
       // }
        mMap.setOnMarkerClickListener { marker ->
            if(marker.isInfoWindowShown){
                marker.hideInfoWindow()
            }else marker.showInfoWindow()

            true  // Return true to prevent the default behavior of showing the info window
        }

    }

    private fun onChangeOption(){
        val changeView= binding.toolbarLayout.itemChange

        changeView.tvWire.setOnClickListener {
            changeView.cvWire.visible()
            changeView.cvNews.gone()
            changeView.tvNews.visible()
            changeView.tvWire.gone()
            isNeedBsButton=true
            isWireView= true
            isNewsView=false
            changeFragment()
            binding.cvList.gone()
            binding.cvMap.visible()
            bottomSheetBehavior.state= BottomSheetBehavior.STATE_EXPANDED
        }

        changeView.tvNews.setOnClickListener {
            changeView.cvNews.visible()
            changeView.cvWire.gone()
            changeView.tvWire.visible()
            changeView.tvNews.gone()
            isNeedBsButton= false
            isWireView=false
            isNewsView= true
            changeFragment()
            binding.cvList.gone()
            binding.cvMap.gone()
            bottomSheetBehavior.state= BottomSheetBehavior.STATE_EXPANDED
        }

        binding.cvList.setOnClickListener {
            bottomSheetBehavior.state= BottomSheetBehavior.STATE_EXPANDED
            binding.cvList.gone()
            binding.cvMap.visible()
            binding.bottomCl.visible()
            changeFragment()

        }

        binding.cvMap.setOnClickListener {
            bottomSheetBehavior.state= BottomSheetBehavior.STATE_HIDDEN
            binding.cvList.visible()
            binding.cvMap.gone()
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
            NAV_WIRE ->{
                displayFragment(WireFragment.newInstance(incidentList,totalCount, data as Bundle),false)
            }
            NAV_NEWS ->{
                displayFragment(NewsFragment.newInstance(),true)
            }
            NAV_COMMENT_LIST ->{
               // displayFragment(CommentsFragment.newInstance(data as String),true)

                val intent= Intent(this, FeedFilterOrCommentsActivity::class.java)
                intent.putExtra(NAV_COMMENT_LIST,data as String)
                startActivity(intent)
            }
            NAV_WIRE_NEWS ->{
           //     bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                val bundle = data as Bundle
                if(bundle.containsKey(FROM_WIRE)){
                    isWireView= bundle.getBoolean(FROM_WIRE)
                }

                if(isWireView){
                    displayFragment(WireFragment.newInstance(incidentList,totalCount,data as Bundle),false)
                }else  displayFragment(NewsFragment.newInstance(),true)
            }
        }
    }

    private fun changeFragment(){
        if(isWireView){
            binding.toolbarLayout.ivRefresh.visible()
            displayFragment(WireFragment.newInstance(incidentList,totalCount, Bundle()),false)
        }else if(isNewsView){
            binding.toolbarLayout.ivRefresh.gone()
            displayFragment(NewsFragment.newInstance(),false)
        }

    }

    fun ImageView.setTint(@ColorRes colorRes: Int) {
        ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(ContextCompat.getColor(context, colorRes)))
        }

    private fun whiteToolbar(){
        binding.appBarLayout.setBackgroundColor(resources.getColor(R.color.white))
        binding.toolbarLayout.ivFeed.setTint(R.color.black)
        binding.toolbarLayout.ivMenu.setTint(R.color.black)
    }

   /* private fun transparentToolbar(){
        binding.appBarLayout.setBackgroundColor(resources.getColor(R.color.white))
        binding.toolbarLayout.ivFeed.setTint(R.color.white)
        binding.toolbarLayout.ivMenu.setTint(R.color.white)
        binding.llTopView.gone()
    }*/


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
        val f = supportFragmentManager.findFragmentById(R.id.fl_main)

        if (f == null) {
            Log.d("BackPressed", "No fragment found in R.id.fl_main")
        } else if (f is WireFragment) {
            binding.cvMap.visible()
        } else {
            Log.d("BackPressed", "Fragment is not WireFragment: ${f::class.java.simpleName}")
        }

    }

    override fun filterApply(incidentList: ArrayList<Incident>, totalCount: String) {
        this.incidentList= incidentList
        this.totalCount= totalCount
        locationArrayList.clear()
        formLatLng()
    }

    override fun updateBottomSheet() {
        bottomSheetBehavior.state= BottomSheetBehavior.STATE_EXPANDED
    }

    val clickListener = object : INotificationClickListener {
        override fun onClick(event: INotificationClickEvent) {
            // respond to click
            Toast.makeText(this@MapsActivity, "Notification clicked", Toast.LENGTH_SHORT).show()
        }
    }



   /* override fun onLowMemory() {
        mapFragment.onLowMemory()
        super.onLowMemory()
    }*/

    override fun onPurchasesUpdated(billingResult: BillingResult, purchaseList: MutableList<Purchase>?) {
        if (billingResult?.responseCode == BillingClient.BillingResponseCode.OK && purchaseList != null) {
            for (purchase in purchaseList!!) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                    acknowledgePurchase(purchase.purchaseToken,purchase)
                }
            }
        } else if (billingResult?.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            println("You've cancelled the Google play billing process...")
        } else {
            println("Item not found or Google play billing error...")
        }
    }

    private fun acknowledgePurchase(purchaseToken: String, purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            println("billingResult:"+ billingResult.responseCode )
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
               // callPaymentBackendApi(purchase)
                println("acknowledge Purchase completed")
            }else{
                println("Exit in acknowledge Purchase")
            }
        }
    }

    override fun onListLoaded(list: List<Incident>) {
        incidentList.clear()
        incidentList.addAll(list)
        locationArrayList.clear()
        formLatLng()
    }


}