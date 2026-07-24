package com.fire.wire.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.fire.wire.R
import com.fire.wire.databinding.ActivityWireDetailBinding
import com.fire.wire.model.incident.request.AddCommentRequest
import com.fire.wire.model.incident.response.CommentsResponse
import com.fire.wire.model.incident.response.Incident
import com.fire.wire.model.user.response.CommonResponse
import com.fire.wire.prefs
import com.fire.wire.resource.Resource
import com.fire.wire.resource.ResourceState
import com.fire.wire.utils.*
import com.fire.wire.utils.IntentUtils.BUN_WIRE_DETAILS
import com.fire.wire.viewModel.FireWireViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WireDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityWireDetailBinding
    private var wireDetails= Incident()
    private lateinit var mMap: GoogleMap
    private lateinit var vm: FireWireViewModel
    private var isLikeSingle=false
    var type="view"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityWireDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initMap()

        clickEvent()
        initExtra()
        initViewModel()
    }

    private fun initViewModel() {
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)
        val commentRequest= AddCommentRequest(
            prefs.userId.toString(), wireDetails._id,type)
        vm.postComment(commentRequest)

        vm.commentsLiveData.observe(this, Observer {
            updateView(it)
        })

        vm.addCommentLiveData.observe(this, Observer {
            if(type=="like")
            updateLikeData(it)
        })

        // the list API omits respondingUnits, so fetch the full incident by id
        // (same as iOS) to populate the units card and signal chip
        vm.incidentByIdLiveData.observe(this, Observer { response ->
            if (response.state == ResourceState.SUCCESS) {
                val detail = response.data?.data?.firstOrNull() ?: return@Observer
                wireDetails = wireDetails.copy(
                    field4Value = detail.field4Value ?: wireDetails.field4Value,
                    respondingUnits = detail.respondingUnits ?: wireDetails.respondingUnits
                )
                val signal = wireDetails.field4Value?.trim().orEmpty()
                if (signal.isNotEmpty()) {
                    binding.tvSignalChip.text = signal
                    binding.tvSignalChip.visible()
                }
                val subLocality = detail.subLocalityName?.trim().orEmpty()
                if (subLocality.isNotEmpty()) {
                    binding.tvSubLocality.text = subLocality
                    binding.tvSubLocality.visible()
                }
                bindUnits()
            }
        })
        if (!wireDetails._id.isNullOrEmpty()) {
            vm.getIncidentById(wireDetails._id.toString())
        }
    }

    private fun updateLikeData(response: Resource<CommonResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                isLikeSingle= !isLikeSingle

                if(isLikeSingle)
                    binding.ivRating.setImageResource(R.drawable.ic_rating_red)
                else binding.ivRating.setImageResource(R.drawable.ic_rating)
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

    private fun initMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun initExtra() {
        val intent= intent.getBundleExtra(IntentUtils.BUN_WIRE_DETAILS)
         wireDetails= intent?.getParcelable(BUN_WIRE_DETAILS)?:Incident()
        isLikeSingle= wireDetails.isLiked
        bindItems()
    }

    private fun bindItems() {
        // summary card: incident type badge + optional signal chip
        binding.tvLevelBadge.text= wireDetails.field1Value
        val signal= wireDetails.field4Value?.trim().orEmpty()
        if(signal.isNotEmpty()){
            binding.tvSignalChip.text= signal
            binding.tvSignalChip.visible()
        } else binding.tvSignalChip.gone()

        // address is the headline; field3 (city/town) is the subtitle; field2 is the description
        binding.tvTitle.text= wireDetails.address
        binding.tvAddress.text= wireDetails.address

        // sub-locality (e.g. borough) comes populated in the list response;
        // the deployed by-ID API only returns the raw id
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
        binding.tvDesc.setOnClickListener {
            binding.tvDesc.maxLines = if (binding.tvDesc.maxLines == 3) Integer.MAX_VALUE else 3
        }

        binding.tvLikeCount.text= if(wireDetails.likeCount.isNullOrEmpty()) "0" else wireDetails.likeCount
        binding.tvCommentCount.text= if(wireDetails.commentCount.isNullOrEmpty()) "0" else wireDetails.commentCount

        if(isLikeSingle)
            binding.ivRating.setImageResource(R.drawable.ic_rating_red)
        else binding.ivRating.setImageResource(R.drawable.ic_rating)

        if(!wireDetails.createdAt.isNullOrEmpty())
            binding.tvDateTime.text= DateUtils.getFormattedDateOfFireWire(wireDetails.createdAt.toString()).uppercase()

        if(wireDetails.featuredImageUrl.isNullOrEmpty()){
            binding.cardBanner.gone()
        }else{
            binding.cardBanner.visible()
            Glide.with(this)
                .load(wireDetails.featuredImageUrl)
                .into(binding.ivBanner)
        }

        bindUnits()
    }

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
        binding.toolbar.tvToolbarTitle.text = getString(R.string.incident_title)
        binding.toolbar.tvToolbarTitle.visible()
        setupMapTypeToggle()
        binding.toolbar.ivFeed.setOnClickListener {
            val intent = Intent(this,FeedsActivity::class.java)
            startActivity(intent)
        }
        binding.toolbar.ivMenu.setOnClickListener {
            finish()
        }
        binding.ivShare.setOnClickListener {
            val shareContent= wireDetails.field1Value.plus("\n").plus(wireDetails.address)
            shareText(this,shareContent)
        }

        binding.ivRating.setOnClickListener {
            type="like"
            isLikeSingle= !wireDetails.isLiked
            vm.postComment(AddCommentRequest(prefs.userId,wireDetails._id,if(isLikeSingle)"like" else "unlike"))
        }

        binding.ivCommand.setOnClickListener { view->
            val intent= Intent(this,FeedFilterActivity::class.java)
            intent.putExtra(NAV_COMMENT_LIST, wireDetails._id)
            startActivity(intent)
            //callback?.replaceFragment(NAV_COMMENT_LIST,it._id.toString())
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
        if(!wireDetails.latitude.isNullOrEmpty() && !wireDetails.longitude.isNullOrEmpty()) {

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
                            com.fire.wire.R.drawable.ic_fire
                        )
                    )
            )

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.0f))
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
                showAlert(response.message)
            }
        }
    }

    fun showAlert(message: String? = "") {
        AlertDialog.Builder(this)
            .setTitle(resources.getString(R.string.app_name))
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
            }
            .show()
    }


}