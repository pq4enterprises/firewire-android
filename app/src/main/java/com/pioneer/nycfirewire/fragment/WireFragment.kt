package com.pioneer.nycfirewire.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.paging.LoadState
import androidx.paging.PagingData
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
import com.pioneer.nycfirewire.listener.OnIncidentListLoadedListener
import com.pioneer.nycfirewire.listener.OnLoadMoreListener
import com.pioneer.nycfirewire.utils.Constants
import com.pioneer.nycfirewire.utils.Constants.NEWS_FRAGMENT
import com.pioneer.nycfirewire.utils.Constants.WIRE_FRAGMENT
import com.pioneer.nycfirewire.utils.IntentUtils.BUN_WIRE_LIST_DETAIL
import com.pioneer.nycfirewire.utils.IntentUtils.TOTAL_COUNT
import com.pioneer.nycfirewire.utils.NetworkUtils.isOnline
import com.pioneer.nycfirewire.utils.inVisible
import com.pioneer.nycfirewire.utils.showToast
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import com.pioneer.nycfirewire.BuildConfig
import com.pioneer.nycfirewire.utils.introsEnabled


@AndroidEntryPoint
class WireFragment: Fragment(),IncidentClickListener {
    private lateinit var binding: BottomSheetWireBinding
    private var callback: ReplaceCallback?=null
    private var callbackFunctions: CallbackFunctions?=null
    private lateinit var vm: FireWireViewModel
    private var incidentList= ArrayList<Incident>()
    private var localityList= ArrayList<String>()
    private var subLocalityList= ArrayList<String>()
    private var posLike=-1
    private var isLikeSingle=false
    var updatedIncident= Incident()

    var totalCount=""
    var isAttach= false
    var isDetach= false

    private lateinit var pagingAdapter: PagingAdapter
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>



    companion object{
        fun newInstance(incident: ArrayList<Incident>, total: String, bundle: Bundle)= WireFragment().putArgs {
            putBundle(FILTER_DATA,bundle)
            putString(TOTAL_COUNT,total)
            putParcelableArrayList(NAV_WIRE_list,incident)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data?.getIntExtra("wire_detail",-1)?:0
                val incidentId = result.data?.getStringExtra("wire_id")
                if(data!=-1){
                    // Call activity method or update fragment viewp
                    pagingAdapter.notifyItemChanged(data)
                }else{
                   if(incidentId?.isNotEmpty() == true) {
                       val position = incidentList.indexOfFirst { it._id == incidentId }
                       pagingAdapter.notifyItemChanged(position)
                   }

                }
            }
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
         pagingAdapter = PagingAdapter(this)

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

            lifecycleScope.launch {
                vm.posts.collectLatest { pagingData ->
                    pagingAdapter.submitData(pagingData)
                    val currentList = pagingAdapter.snapshot().items
                    incidentList= currentList as java.util.ArrayList<Incident>
                    (activity as? OnIncidentListLoadedListener)?.onListLoaded(currentList)
                }

                binding.rvMainFilter.post {
                    binding.rvMainFilter.scrollBy(0, 1) // forces scroll event
                }
            }

            vm.totalCount.observe(viewLifecycleOwner, Observer{
                binding.tvWireTotal.text= getString(R.string.posts_listed, it)
            })
        }
        val recycler = binding.rvMainFilter.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pagingAdapter
        }


       /* pagingAdapter.loadStateFlow
            .distinctUntilChangedBy { it.refresh }
            .collectLatest { loadStates ->
                val isRefreshing = loadStates.refresh is LoadState.Loading
                binding.progressBar.isVisible = isRefreshing && pagingAdapter.itemCount == 0
            }*/


        lifecycleScope.launch {
            pagingAdapter.loadStateFlow .distinctUntilChangedBy { it.refresh }.collectLatest { loadStates ->
                val isRefreshing = loadStates.refresh is LoadState.Loading
                binding.progress.isVisible = isRefreshing && pagingAdapter.itemCount == 0
                loadStates.refresh.let {
                        if (it is LoadState.Error || pagingAdapter.itemCount == 0) {
                          if(binding.progress.isVisible) binding.tvNoData.gone() else binding.tvNoData.visible()
                        }else  binding.tvNoData.gone()
                    }
            }

        }



        pagingAdapter.addLoadStateListener { loadStates ->
            val isLoaded = loadStates.refresh is LoadState.NotLoading
            if (isLoaded) {
                val currentList = pagingAdapter.snapshot().items
                Log.d("LoadedList", "Loaded ${currentList.size} items")
                // You can also pass this list to Activity here if needed

                (activity as? OnIncidentListLoadedListener)?.onListLoaded(currentList)

            }
        }
    }

    private fun updateLikeData(response: Resource<CommonResponse>) {
        when(response.state){
            ResourceState.LOADING ->{
                binding.progress.visible()
            }
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


    @SuppressLint("SuspiciousIndentation")
    private fun updateLikePosition(){
        if(posLike!=-1) {
            /*incidentList[posLike].isLiked = isLikeSingle
            if (isLikeSingle)
                incidentList[posLike].likeCount =
                    (incidentList[posLike].likeCount ?: "0").toInt().plus(1).toString()
            else incidentList[posLike].likeCount =
                (incidentList[posLike].likeCount ?: "0").toInt().minus(1).toString()*/

            // Update the item locally
            val updatedIncident = updatedIncident.copy()
            updatedIncident.isLiked= isLikeSingle

            if (isLikeSingle)
                updatedIncident.likeCount =
                    (updatedIncident.likeCount ?: "0").toInt().plus(1).toString()
            else updatedIncident.likeCount =
                (updatedIncident.likeCount ?: "0").toInt().minus(1).toString()


            // Convert current PagingData to a Snapshot
            val currentItems = pagingAdapter.snapshot().items.toMutableList()

            // Replace the item at that position
            if (posLike != RecyclerView.NO_POSITION && posLike < currentItems.size) {
                currentItems[posLike] = updatedIncident

                // Submit updated PagingData
                val updatedPagingData = PagingData.from(currentItems)
                pagingAdapter.submitData(lifecycle, updatedPagingData)
            }

        }
    }


    private fun initExtra() {
        incidentList= arguments?.getParcelableArrayList<Incident>(NAV_WIRE_list) as ArrayList<Incident>
        val bundle= arguments?.getBundle(FILTER_DATA)
        if(bundle?.containsKey(FILTER_DATA) == true){
            val filterData= bundle.getParcelable<FilterData>(FILTER_DATA)
            localityList.addAll(filterData?.locality?:ArrayList())
            subLocalityList.addAll(filterData?.subLocality?:ArrayList())

            incidentList.clear()

           /* if(isOnline(requireContext())) {
                vm.getIncidentList(localityList, subLocalityList, "1", "10")
            }else{
                showToast(requireContext(),getString(R.string.check_network_connection))
            }*/

        }


       // totalCount= arguments?.getString(TOTAL_COUNT).toString()
       // binding.tvWireTotal.text= getString(R.string.posts_listed, totalCount)
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

       // pagingAdapter.refresh()

    }

    fun refreshData(){
        pagingAdapter.refresh()
        (activity as? OnIncidentListLoadedListener)?.onListLoaded(pagingAdapter.snapshot().items)
    }

    override fun onDetach() {
        super.onDetach()
        isDetach= true
    }

    override fun onRatingClicked(it: Incident,pos:Int) {
        updatedIncident=it
        isLikeSingle= !it.isLiked
        posLike= pos
        if(isOnline(requireContext())) {
            vm.postComment(AddCommentRequest(prefs.userId,it._id,if(isLikeSingle)"like" else "unlike"))
        }else showToast(requireContext(), getString(R.string.check_network_connection))

    }

    override fun appIntroTour(
        tvTitle: View,
        ivRating: View,
        ivCommand: View,
        ivShare: View
    ) {
        if(introsEnabled && prefs.showAppIntro && prefs.showFeedIntro ) {

            TapTargetSequence(requireActivity())
                .targets(
                    TapTarget.forView(
                        tvTitle,
                        "INCIDENT","Click to view incident details"
                    ).id(1),
                    TapTarget.forView(
                        ivRating,"LIKE",
                        "Tap to like incidents"
                    ),
                    TapTarget.forView(
                        ivCommand,"COMMENT",
                        "Comment and share photos"
                    ),
                    TapTarget.forView(ivShare, "SHARE","Share incidents with friends")
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
    }

    override fun onItemClicked(incident: Incident) {
        val bundle= Bundle()
        bundle.putParcelable(BUN_WIRE_DETAILS,incident)
        bundle.putParcelableArrayList(BUN_WIRE_LIST_DETAIL, incidentList)
        val intent = Intent(requireContext(), WireDetailActivity::class.java)
        intent.putExtra(BUN_WIRE_DETAILS, bundle)
        resultLauncher.launch(intent)
    }


}
