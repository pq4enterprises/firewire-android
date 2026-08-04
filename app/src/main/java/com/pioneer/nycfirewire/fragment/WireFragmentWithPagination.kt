package com.pioneer.nycfirewire.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pioneer.nycfirewire.ReplaceCallback
import com.pioneer.nycfirewire.activity.LoginNewActivity
import com.pioneer.nycfirewire.activity.WireDetailActivity
import com.pioneer.nycfirewire.adapter.setUpAdapter
import com.pioneer.nycfirewire.callback.CallbackFunctions
import com.pioneer.nycfirewire.model.incident.request.AddCommentRequest
import com.pioneer.nycfirewire.model.incident.response.FilterData
import com.pioneer.nycfirewire.model.incident.response.Incident
import com.pioneer.nycfirewire.model.incident.response.IncidentResponse
import com.pioneer.nycfirewire.model.user.response.CommonResponse
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.IntentUtils.BUN_WIRE_DETAILS
import com.pioneer.nycfirewire.utils.IntentUtils.FILTER_DATA
import com.pioneer.nycfirewire.viewModel.FireWireViewModel
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.activity.FeedFilterOrCommentsActivity
import com.pioneer.nycfirewire.databinding.BottomSheetWireBinding
import com.pioneer.nycfirewire.databinding.ItemWireBinding
import com.pioneer.nycfirewire.listener.RecyclerViewLoadMoreScroll
import com.pioneer.nycfirewire.utils.DateUtils
import com.pioneer.nycfirewire.utils.NAV_COMMENT_LIST
import com.pioneer.nycfirewire.utils.NAV_FILTER
import com.pioneer.nycfirewire.utils.NAV_WIRE_list
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.putArgs
import com.pioneer.nycfirewire.utils.startNewActivity
import com.pioneer.nycfirewire.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import androidx.recyclerview.widget.LinearLayoutManager;
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.getkeepsafe.taptargetview.TapTargetView
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.pioneer.nycfirewire.adapter.IncidentClickListener
import com.pioneer.nycfirewire.adapter.PagingAdapter
import com.pioneer.nycfirewire.listener.OnLoadMoreListener
import com.pioneer.nycfirewire.utils.Constants
import com.pioneer.nycfirewire.utils.Constants.NEWS_FRAGMENT
import com.pioneer.nycfirewire.utils.Constants.WIRE_FRAGMENT
import com.pioneer.nycfirewire.utils.IntentUtils.TOTAL_COUNT
import com.pioneer.nycfirewire.utils.NetworkUtils.isOnline
import com.pioneer.nycfirewire.utils.inVisible
import com.pioneer.nycfirewire.utils.showToast
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.pioneer.nycfirewire.BuildConfig
import com.pioneer.nycfirewire.utils.introsEnabled


@AndroidEntryPoint
class WireFragmentWithPagination: Fragment(), IncidentClickListener {
    private lateinit var binding: BottomSheetWireBinding
    private var callback: ReplaceCallback?=null
    private var callbackFunctions: CallbackFunctions?=null
    private lateinit var vm: FireWireViewModel
    private var incidentList= ArrayList<Incident>()
    private var localityList= ArrayList<String>()
    private var subLocalityList= ArrayList<String>()
    private var posLike=-1
    private var isLikeSingle=false

    var totalCount=""
    var isAttach= false
    var isDetach= false


    private val pagingAdapter = PagingAdapter(this)



    companion object{
        fun newInstance(incident: ArrayList<Incident>, total: String, bundle: Bundle)= WireFragmentWithPagination().putArgs {
            putBundle(FILTER_DATA,bundle)
            putString(TOTAL_COUNT,total)
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

        if(introsEnabled && prefs.showAppIntro && prefs.showFeedIntro){
            introApp()
        }else{
            initiateAllAction()
        }
    }

    private fun initiateAllAction(){
        initViewModel()
        initExtra()
        clickEvents()
       // setupAdapter()
    }

    private fun introApp() {
        if(introsEnabled && prefs.showAppIntro && prefs.showFeedIntro) {
            TapTargetView.showFor(requireActivity(),                 // `this` is an Activity
                TapTarget.forView(
                    binding.tvFilter,
                    "FEED AREAS",
                    "Select areas to appear on your feed"
                ).dimColor(R.color.black)
                    .outerCircleColor(R.color.app_red)
                    .targetCircleColor(R.color.white)
                    .textColor(R.color.black)
                    .targetRadius(20),
                object : TapTargetView.Listener() {
                    override fun onTargetClick(view: TapTargetView) {
                        super.onTargetClick(view)
                        initiateAllAction()
                    }

                    override fun onTargetCancel(view: TapTargetView?) {
                        super.onTargetCancel(view)
                        initiateAllAction()
                    }
                })
        }
    }


    private fun initViewModel() {
        if(isAttach && !isDetach) {
            vm = ViewModelProvider(this).get(FireWireViewModel::class.java)
            vm.addCommentLiveData.observe(viewLifecycleOwner, Observer {
                updateLikeData(it)
            })
        }


        val recycler = binding.rvMainFilter.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pagingAdapter
        }

        lifecycleScope.launch {
            vm.posts.collectLatest { pagingData ->
                pagingAdapter.submitData(pagingData)
            }

            binding.rvMainFilter.post {
                binding.rvMainFilter.scrollBy(0, 1) // forces scroll event
            }
        }
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



    @SuppressLint("SuspiciousIndentation")
    private fun updateLikePosition(){
        if(posLike!=-1) {
            incidentList[posLike].isLiked = isLikeSingle
            if (isLikeSingle)
                incidentList[posLike].likeCount =
                    (incidentList[posLike].likeCount ?: "0").toInt().plus(1).toString()
            else incidentList[posLike].likeCount =
                (incidentList[posLike].likeCount ?: "0").toInt().minus(1).toString()

            binding.rvMainFilter.adapter?.notifyItemChanged(posLike)
        }
    }



    private fun setupAdapter() {
        val linearLayoutManager = LinearLayoutManager(requireContext()).apply { orientation = LinearLayoutManager.VERTICAL }
        binding.rvMainFilter.setHasFixedSize(true)
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
                // ivRating is a plain ImageView in the redesigned row, not a
                // TextView with a compound drawable
                bindingItem.ivRating.setImageResource(
                    if(it.isLiked) R.drawable.ic_rating_red else R.drawable.ic_rating)

                bindingItem.ivRating.setOnClickListener { view->
                    isLikeSingle= !it.isLiked
                    posLike= pos
                    if(isOnline(requireContext())) {
                        vm.postComment(AddCommentRequest(prefs.userId,it._id,if(isLikeSingle)"like" else "unlike"))
                    }else showToast(requireContext(), getString(R.string.check_network_connection))

                }

                if(!it.createdAt.isNullOrEmpty()) {
                    bindingItem.tvDateTime.text =
                        DateUtils.formatDateTime(it.createdAt.toString())
                }

                // the redesigned row wraps the banner in a rounded card, so
                // visibility is toggled on the card rather than the ImageView
                if(it.featuredImageUrl.isNullOrEmpty()){
                    bindingItem.cardBanner.gone()
                }else{
                    Glide.with(this)
                        .load(it.featuredImageUrl)
                        .into(bindingItem.ivBanner)
                    bindingItem.cardBanner.visible()
                }

                // The redesigned row splits what used to be one line: the street
                // address is the headline, and the city/town + sub-locality move
                // to a muted line beneath it (iOS parity).
                bindingItem.tvAddress.text= it.address

                val localityParts= ArrayList<String>()
                if(!it.field3Value.isNullOrEmpty()) localityParts.add(it.field3Value!!)
                val subName= it.subLocalityDetails?.firstOrNull()?.name
                if(!subName.isNullOrEmpty()) localityParts.add(subName)
                if(localityParts.isNotEmpty()){
                    bindingItem.tvLocality.text= localityParts.joinToString(", ")
                    bindingItem.tvLocality.visible()
                }else{
                    bindingItem.tvLocality.gone()
                }
                bindingItem.tvRateCount.text= getString(R.string.star,it.likeCount)
                val count= if(it.commentCount.isNullOrEmpty())"0" else it.commentCount

                if(count.toInt()>1)
                bindingItem.tvCommentCount.text= getString(R.string.comments,count)
                else bindingItem.tvCommentCount.text= getString(R.string.comment,count)

                bindingItem.ivCommand.setOnClickListener { view->
                   // callback?.replaceFragment(NAV_COMMENT_LIST,it._id.toString())
                    val intent= Intent(requireContext(), FeedFilterOrCommentsActivity::class.java)
                    intent.putExtra(NAV_COMMENT_LIST,it._id.toString())
                    startActivity(intent)
                }


                bindingItem.clContainer.setOnClickListener { view->
                    val bundle= Bundle()
                    bundle.putParcelable(BUN_WIRE_DETAILS,it)
                    val intent = Intent(requireContext(), WireDetailActivity::class.java)
                    intent.putExtra(BUN_WIRE_DETAILS, bundle)
                    startActivity(intent)

                }
                bindingItem.ivShare.setOnClickListener {view->
                    val shareContent= it.field1Value.plus("\n").plus(it.address).plus("\n").plus(
                        Constants.PLAY_STORE_URL)
                }

                if(introsEnabled && prefs.showAppIntro && prefs.showFeedIntro && pos==0) {

                    TapTargetSequence(requireActivity())
                        .targets(
                            TapTarget.forView(
                                bindingItem.tvTitle,
                                "INCIDENT","Click to view incident details"
                            ).id(1),
                            TapTarget.forView(
                                bindingItem.ivRating,"LIKE",
                                "Tap to like incidents"
                            ),
                            TapTarget.forView(
                                bindingItem.ivCommand,"COMMENT",
                                "Comment and share photos"
                            ),
                            TapTarget.forView(bindingItem.ivShare, "SHARE","Share incidents with friends")
                                .dimColor(R.color.black)
                                .outerCircleColor(R.color.app_red)
                                .targetCircleColor(R.color.white)
                                .textColor(R.color.black)
                                .targetRadius(20)
                        )
                        .listener(object : TapTargetSequence.Listener {
                            override fun onSequenceFinish() {
                                // Yay
                                prefs.showFeedIntro=false
                            }

                            override fun onSequenceStep(
                                lastTarget: TapTarget,
                                targetClicked: Boolean
                            ) {
                                if(targetClicked && lastTarget.id()==1){
                                    callbackFunctions?.updateBottomSheet()
                                }

                            }

                            override fun onSequenceCanceled(lastTarget: TapTarget) {
                                // Boo
                               prefs.showFeedIntro=false
                            }
                        })
                        .start()
                }



            },{},linearLayoutManager
        )


    }

    private fun initExtra() {
        incidentList= arguments?.getParcelableArrayList<Incident>(NAV_WIRE_list) as ArrayList<Incident>
        val bundle= arguments?.getBundle(FILTER_DATA)
        if(bundle?.containsKey(FILTER_DATA) == true){
            val filterData= bundle.getParcelable<FilterData>(FILTER_DATA)
            localityList.addAll(filterData?.locality?:ArrayList())
            subLocalityList.addAll(filterData?.subLocality?:ArrayList())

            incidentList.clear()

            if(isOnline(requireContext())) {
                vm.getIncidentList(localityList, subLocalityList, "1", "10")
            }else{
                showToast(requireContext(),getString(R.string.check_network_connection))
            }

        }

        totalCount= arguments?.getString(TOTAL_COUNT).toString()
        binding.tvWireTotal.text= getString(R.string.feed_updated_placeholder)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        isAttach=true
        callback= context as? ReplaceCallback
        callbackFunctions= context as? CallbackFunctions
    }

    private fun clickEvents() {
        binding.tvFilter.setOnClickListener {
            callback?.replaceFragment(NAV_FILTER,"")
        }

       /* binding.rvMainFilter.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            // on scroll change we are checking when users scroll as bottom.
            if (scrollY == binding.rvMainFilter.getChildAt(0).measuredHeight - v.measuredHeight) {
                // in this method we are incrementing page number,
                // making progress bar visible and calling get data method.
                offset++

                binding.rvProgress.visible()
                vm.getIncidentList(localityList,subLocalityList,offset.toString(),limit.toString())

            }
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


    override fun onResume() {
        super.onResume()
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, WIRE_FRAGMENT)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "WireFragment")
        }

        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    override fun onDetach() {
        super.onDetach()
        isDetach= true
    }

    override fun onRatingClicked(
        incident: Incident,
        pos: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun appIntroTour(
        tvTitle: View,
        ivRating: View,
        ivCommand: View,
        ivShare: View
    ) {
        TODO("Not yet implemented")
    }

    override fun onItemClicked(incident: Incident) {
        TODO("Not yet implemented")
    }


}
