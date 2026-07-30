package com.pioneer.nycfirewire.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.pioneer.nycfirewire.ReplaceCallback
import com.pioneer.nycfirewire.adapter.setUpAdapter
import com.pioneer.nycfirewire.model.incident.response.FilterData
import com.pioneer.nycfirewire.model.locality.Locality
import com.pioneer.nycfirewire.model.locality.LocalityResponse
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.IntentUtils.FILTER_DATA
import com.pioneer.nycfirewire.utils.IntentUtils.FROM_WIRE
import com.pioneer.nycfirewire.viewModel.FireWireViewModel
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.BottomSheetFilterBinding
import com.pioneer.nycfirewire.databinding.ItemSubFilterBinding
import com.pioneer.nycfirewire.databinding.ItemSubFilterItemBinding
import com.pioneer.nycfirewire.model.user.request.PostAreaData
import com.pioneer.nycfirewire.model.user.response.CommonResponse
import com.pioneer.nycfirewire.utils.Constants
import com.pioneer.nycfirewire.utils.Constants.CODE_UPDATED
import com.pioneer.nycfirewire.utils.Constants.COMMENTS_FRAGMENT
import com.pioneer.nycfirewire.utils.Constants.FILTER_FRAGMENT
import com.pioneer.nycfirewire.utils.Constants.LOCALITY_DATA
import com.pioneer.nycfirewire.utils.IntentUtils.TOTAL_COUNT
import com.pioneer.nycfirewire.utils.NAV_WIRE_NEWS
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.putArgs
import com.pioneer.nycfirewire.utils.showToast
import com.pioneer.nycfirewire.utils.visible
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class FilterFragment : Fragment() {

    private lateinit var binding: BottomSheetFilterBinding
    private var callback: ReplaceCallback? = null
    private lateinit var vm: FireWireViewModel
    private var localityListData = ArrayList<Locality>()
   // private var filterLocalityList = ArrayList<Locality>()
    private var isSelectAll= false

    companion object {
        fun newInstance() = FilterFragment().putArgs { }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as? ReplaceCallback
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = BottomSheetFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, FILTER_FRAGMENT)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "FilterFragment")
        }

        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        clickEvent()
        //getData()

        val itemTouchListener = object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                // Detect touch events here, for example, detect a click event
                rv.getParent().requestDisallowInterceptTouchEvent(true);

                return false // Return false to let other touch events be handled by the RecyclerView
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                // Handle the touch event (e.g., on a long press)
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                // Optional: Control whether RecyclerView should disallow intercepting touch events
            }
        }

        binding.rvMainFilter.addOnItemTouchListener(itemTouchListener)

    }

    private fun initViewModel() {
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)
        vm.getLocalityList(Constants.TYPE_AREA)
        vm.localityLiveData.observe(viewLifecycleOwner) {
            updateFilterData(it)
        }

        vm.postSelectAreaLiveData.observe(viewLifecycleOwner){
            updateSelectAreaData(it)
        }

    }

    private fun updateFilterData(response: Resource<LocalityResponse>?) {
        when (response?.state) {
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                if (response.data?.code == Constants.CODE_SUCCESS) {
                    val localityList = response.data.data
                    val list = ArrayList(localityList?.data ?: ArrayList())
                    localityListData.addAll(list)
                    setupAdapter()

                } else {
                    showAlert(response.data?.message.toString())
                }


            }
            ResourceState.ERROR -> {
                binding.progress.gone()

            }
            else -> {}
        }
    }

    private fun setupAdapter() {
       // checkSelectedData()
        if (localityListData.isNotEmpty()) {
            binding.tvNoData.gone()
            binding.rvMainFilter.visible()
        } else {
            binding.tvNoData.visible()
            binding.rvMainFilter.gone()
        }


        binding.rvMainFilter.setUpAdapter(
            localityListData,
            R.layout.item_sub_filter,
            ItemSubFilterBinding::inflate,
            { data, pos, bindingItem ->
                bindingItem.tvCountryName.text = data.name

                var list= data.subLocality?.filter { !it.isChecked }
                data.isSelectAll=  if(list?.isEmpty() == true) true else false

                bindingItem.tvSelectAll.text= if(data.isSelectAll){
                    getString(R.string.un_select_all)
                }else getString(R.string.select_all)

                bindingItem.tvSelectAll.setOnClickListener {
                    localityListData[pos].subLocality?.forEach {
                        it.isChecked= if(data.isSelectAll) false else true
                    }
                   // binding.rvMainFilter.adapter?.notifyItemChanged(pos)
                    binding.rvMainFilter.adapter?.notifyItemChanged(pos)
                }

                val subLocalityList = ArrayList(data.subLocality ?: ArrayList())
                bindingItem.rvSubFilter.setUpAdapter(
                    subLocalityList,
                    R.layout.item_sub_filter_item,
                    ItemSubFilterItemBinding::inflate,
                    { it1, pos1, subBindItem ->
                        subBindItem.cbCountry.isChecked = it1.isChecked
                        subBindItem.tvSubCountry.text = it1.name
                        subBindItem.llCb.setOnClickListener { view ->
                            it1.isChecked = !it1.isChecked
                            bindingItem.rvSubFilter.adapter?.notifyItemChanged(pos1)

                        }
                    }

                )


            }
        )
    }

  /*  private fun checkSelectedData() {
        filterLocalityList.forEach {
            val selectedSubLoc = it.subLocality?.filter { it.isChecked }

            localityListData.forEach {
               it.subLocality?.forEach { subLoc->
                   if(selectedSubLoc?.map { it._id }?.contains(subLoc._id) == true){
                       subLoc.isChecked= true
                   }
               }
            }
        }
    }*/

    private fun clickEvent() {
        binding.tvDone.setOnClickListener {
            var postList= ArrayList<PostAreaData>()

            localityListData.forEach { loc ->
                loc.subLocality?.forEach {
                    if(it.isChecked) {
                        loc.isChecked = true

                        val data = PostAreaData(
                            localityId = loc._id.toString(),
                            subLocalityId = it._id.toString(),
                            userId = prefs.userId.toString()
                        )
                        postList.add(data)
                    }
                }
            }
            if(postList.isNotEmpty()) {
                vm.postSelectArea(postList)
            }else showToast(requireContext(),"Please select a area before proceeding")


            //saveData()
        }
    }

   /* private fun saveData() {
        val gson = Gson()
        val jsonString = gson.toJson(localityListData)
        prefs.filterData = jsonString
    }

    private fun getData() {
        val jsonString = prefs.filterData
        if (!jsonString.isNullOrEmpty()) {
            // Convert JSON string back to ArrayList
            val gson = Gson()
            val arrayListType =
                object : com.google.gson.reflect.TypeToken<ArrayList<Locality>>() {}.type
            filterLocalityList.addAll(gson.fromJson(jsonString, arrayListType))
        }
    }*/


    fun showAlert(message: String? = "") {
        AlertDialog.Builder(requireContext())
            .setTitle(resources.getString(R.string.app_name))
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
            }
            .show()
    }


    private fun updateSelectAreaData(response: Resource<CommonResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                if(response.data?.code== CODE_UPDATED) {

                    val localityId = ArrayList<String>()
                    localityId.addAll(localityListData.filter { it.isChecked }.map { it._id ?: "" })

                    val subLocalityId = ArrayList<String>()
                    localityListData.filter { it.isChecked }.forEach { subItem ->
                        subLocalityId.addAll(subItem.subLocality?.filter { it.isChecked }
                            ?.map { it._id ?: "" }!!)
                    }
                    prefs.localityIds= localityId
                    val bundle = Bundle()
                    bundle.putParcelable(FILTER_DATA, FilterData(localityId, subLocalityId))
                    bundle.putBoolean(FROM_WIRE, true)
                    callback?.replaceFragment(NAV_WIRE_NEWS, bundle)

                }else showSnackbar(binding.tvNoData,response.data?.message.toString())
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showAlert(response.message)
            }
        }
    }

    private fun showSnackbar(view: View, message: String) {
        hideKeyboard()

        // Create the Snackbar
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE)

        // Optional: Set the anchor view to avoid blocking UI elements like BottomNavigationView or BottomSheet
        val bottomNavigationView = view.findViewById<View>(R.id.bottom_constraint)  // Replace with your bottom view ID
        if (bottomNavigationView != null) {
            snackbar.setAnchorView(bottomNavigationView)
        }

        // Optional: Adjust Snackbar's bottom margin to avoid overlap with the system navigation gestures
        val snackbarView = snackbar.view
        val params = snackbarView.layoutParams as FrameLayout.LayoutParams
        params.bottomMargin = 100  // Adjust this as necessary for your layout
        snackbar.setAction("OK",{snackbar.dismiss()})
        snackbar.setActionTextColor(requireContext().getColor(R.color.app_red))

        snackbarView.layoutParams = params
        snackbar.show()
    }
    fun Fragment.hideKeyboard() {
        // Get the current focused view (usually an EditText)
        val view = activity?.currentFocus

        // If the view is not null, hide the keyboard
        view?.let {
            val inputMethodManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }
}