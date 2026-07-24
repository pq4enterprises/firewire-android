package com.fire.wire.fragment

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.fire.wire.R
import com.fire.wire.ReplaceCallback
import com.fire.wire.activity.LoginNewActivity
import com.fire.wire.activity.WireDetailActivity
import com.fire.wire.adapter.setUpAdapter
import com.fire.wire.callback.CallbackFunctions
import com.fire.wire.databinding.BottomSheetWireBinding
import com.fire.wire.databinding.ItemWireBinding
import com.fire.wire.model.incident.request.AddCommentRequest
import com.fire.wire.model.incident.response.FilterData
import com.fire.wire.model.incident.response.Incident
import com.fire.wire.model.incident.response.IncidentResponse
import com.fire.wire.model.user.response.CommonResponse
import com.fire.wire.prefs
import com.fire.wire.resource.Resource
import com.fire.wire.resource.ResourceState
import com.fire.wire.utils.*
import com.fire.wire.utils.IntentUtils.BUN_WIRE_DETAILS
import com.fire.wire.utils.IntentUtils.FILTER_DATA
import com.fire.wire.viewModel.FireWireViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class WireFragment: Fragment() {

    private lateinit var binding: BottomSheetWireBinding
    private var callback: ReplaceCallback?=null
    private var callbackFunctions: CallbackFunctions?=null
    private lateinit var vm: FireWireViewModel
    private var incidentList= ArrayList<Incident>()
    private var localityList= ArrayList<String>()
    private var subLocalityList= ArrayList<String>()
    private var posLike=-1
    private var isLikeSingle=false


    companion object{
        fun newInstance(incident: ArrayList<Incident>, bundle: Bundle)= WireFragment().putArgs {
            putBundle(FILTER_DATA,bundle)
            putParcelableArrayList(NAV_WIRE_list,incident)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetWireBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        initExtra()
        clickEvents()
        setupAdapter()
        //binding.rvMainFilter.isNestedScrollingEnabled=false
    }

    private fun initViewModel() {
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)
        vm.addCommentLiveData.observe(viewLifecycleOwner, Observer {
           updateLikeData(it)
        })
    }

    private fun updateLikeData(response: Resource<CommonResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                updateLikePosition()
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

    private fun updateIncidentList(response: Resource<IncidentResponse>?) {
        when(response?.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                response.data?.let { it1 ->
                    val list= it1.data?.data?: emptyList()
                    incidentList= ArrayList(list)

                    setupAdapter()
                     callbackFunctions?.filterApply(incidentList)

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

    private fun updateLikePosition(){
      incidentList[posLike].isLiked= isLikeSingle
        if(isLikeSingle)
        incidentList[posLike].likeCount= (incidentList[posLike].likeCount?:"0").toInt().plus(1).toString()
        else incidentList[posLike].likeCount= (incidentList[posLike].likeCount?:"0").toInt().minus(1).toString()

        binding.rvMainFilter.adapter?.notifyItemChanged(posLike)
    }


    private fun setupAdapter() {
        if(incidentList.isNotEmpty()){
          binding.tvNoData.gone()
          binding.rvMainFilter.visible()
        }else{
            binding.tvNoData.visible()
            binding.rvMainFilter.gone()
        }
        binding.tvWireTotal.text= getString(R.string.posts_listed, incidentList.size.toString())
        binding.rvMainFilter.setUpAdapter(
            incidentList,
            R.layout.item_wire,
            ItemWireBinding::inflate,
            { it,pos,bindingItem->
                bindingItem.tvTitle.text= it.field1Value
                bindingItem.tvDesc.text= it.field2Value
                if(it.field2Value?.isNotEmpty() == true)bindingItem.tvDesc.visible() else bindingItem.tvDesc.gone()
                Glide.with(this)
                    .load(it.featuredImageUrl)
                    .into(bindingItem.ivBanner)
                if(it.isLiked)
                bindingItem.ivRating.setImageResource(R.drawable.ic_rating_red)
                else bindingItem.ivRating.setImageResource(R.drawable.ic_rating)
                // design-system tint: red when starred, muted otherwise (dark-mode safe)
                ImageViewCompat.setImageTintList(bindingItem.ivRating, ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), if(it.isLiked) R.color.fw_red else R.color.fw_muted)))

                bindingItem.ivRating.setOnClickListener { view->
                    isLikeSingle= !it.isLiked
                    posLike= pos
                    vm.postComment(AddCommentRequest(prefs.userId,it._id,if(isLikeSingle)"like" else "unlike"))
                }

                if(!it.createdAt.isNullOrEmpty())
                bindingItem.tvDateTime.text= DateUtils.getFormattedDateOfFireWire(it.createdAt.toString())

                if(it.featuredImageUrl.isNullOrEmpty()){
                    bindingItem.cardBanner.gone()
                }else{
                    Glide.with(this)
                        .load(it.featuredImageUrl)
                        .into(bindingItem.ivBanner)
                    bindingItem.cardBanner.visible()
                }

                bindingItem.tvAddress.text= it.address
                // design-system card shows bare counts inside the star/comment chips
                bindingItem.tvRateCount.text= if(it.likeCount.isNullOrEmpty()) "0" else it.likeCount
                val count= if(it.commentCount.isNullOrEmpty())"0" else it.commentCount
                bindingItem.tvCommentCount.text= count

                bindingItem.ivCommand.setOnClickListener { view->
                    callback?.replaceFragment(NAV_COMMENT_LIST,it._id.toString())
                }


                bindingItem.clContainer.setOnClickListener { view->
                    val bundle= Bundle()
                    bundle.putParcelable(BUN_WIRE_DETAILS,it)
                    val intent = Intent(requireContext(),WireDetailActivity::class.java)
                    intent.putExtra(BUN_WIRE_DETAILS, bundle)
                    startActivity(intent)

                }
                bindingItem.ivShare.setOnClickListener {view->
                    val shareContent= it.field1Value.plus("\n").plus(it.address)
                    shareText(requireContext(),shareContent)
                }

            }
        )
    }

    private fun initExtra() {
        incidentList= arguments?.getParcelableArrayList<Incident>(NAV_WIRE_list) as ArrayList<Incident>
        val bundle= arguments?.getBundle(FILTER_DATA)
        if(bundle?.containsKey(FILTER_DATA) == true){
            val filterData= bundle.getParcelable<FilterData>(FILTER_DATA)
            localityList.addAll(filterData?.locality?:ArrayList())
            subLocalityList.addAll(filterData?.subLocality?:ArrayList())

            vm.getIncidentList(localityList,subLocalityList)
            vm.incidentLiveData.observe(viewLifecycleOwner, Observer {
                updateIncidentList(it)
            })
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback= context as? ReplaceCallback
        callbackFunctions= context as? CallbackFunctions
    }

    private fun clickEvents() {
        binding.tvFilter.setOnClickListener {
            callback?.replaceFragment(NAV_FILTER,"")
        }


      /*  binding.rvMainFilter.setOnClickListener {
           val intent = Intent(requireContext(), WireDetailActivity::class.java)
           startActivity(intent)
        }*/

    }

    fun showAlert(message: String? = "") {
        AlertDialog.Builder(requireContext())
            .setTitle(resources.getString(R.string.app_name))
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
            }
            .show()
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


}
