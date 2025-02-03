package com.fire.wire.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
        binding.tvTitle.text= wireDetails.field1Value
        binding.tvSubTitle.text= wireDetails.field2Value
        binding.tvAddress.text= wireDetails.address

        if(wireDetails.field2Value?.isNotEmpty() == true)binding.tvSubTitle.visible() else binding.tvSubTitle.gone()

        binding.tvLikeCount.text= getString(R.string.star,wireDetails.likeCount)
        val count= if(wireDetails.commentCount.isNullOrEmpty())"0" else wireDetails.commentCount

        if(count?.toInt()!! >1)
            binding.tvCommentCount.text= getString(R.string.comments,count)
        else binding.tvCommentCount.text= getString(R.string.comment,count)

        if(isLikeSingle)
            binding.ivRating.setImageResource(R.drawable.ic_rating_red)
        else binding.ivRating.setImageResource(R.drawable.ic_rating)


        if(wireDetails.field3Value.isNullOrEmpty())binding.tvDesc.gone() else binding.tvDesc.visible()
        binding.tvDesc.text= wireDetails.field3Value

        if(!wireDetails.createdAt.isNullOrEmpty())
            binding.tvDateTime.text= DateUtils.getFormattedDateOfFireWire(wireDetails.createdAt.toString())

        if(wireDetails.featuredImageUrl.isNullOrEmpty()){
            binding.tvNoImage.visible()
        }else{
            Glide.with(this)
                .load(wireDetails.featuredImageUrl)
                .into(binding.ivBanner)
            binding.tvNoImage.gone()
        }


    }

    private fun clickEvent() {
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

            mMap.animateCamera(CameraUpdateFactory.zoomTo(18.0f))

            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
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