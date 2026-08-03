package com.pioneer.nycfirewire.activity


import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.adapter.setUpAdapter
import com.pioneer.nycfirewire.databinding.BottomSheetFilterBinding
import com.pioneer.nycfirewire.databinding.ItemSubFilterBinding
import com.pioneer.nycfirewire.databinding.ItemSubFilterItemBinding
import com.pioneer.nycfirewire.model.locality.Locality
import com.pioneer.nycfirewire.model.locality.LocalityResponse
import com.pioneer.nycfirewire.model.user.request.PostAreaData
import com.pioneer.nycfirewire.model.user.response.CommonResponse
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.Constants
import com.pioneer.nycfirewire.utils.Constants.CODE_UPDATED
import com.pioneer.nycfirewire.utils.Constants.FROM_START
import com.pioneer.nycfirewire.utils.Constants.LOCALITY_DATA
import com.pioneer.nycfirewire.utils.Constants.SELECT_AREA
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.showToast
import com.pioneer.nycfirewire.utils.visible
import com.pioneer.nycfirewire.viewModel.FireWireViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectAreaActivity : BaseActivity() {


    private lateinit var binding: BottomSheetFilterBinding
    private lateinit var vm: FireWireViewModel
    private var localityListData = ArrayList<Locality>()
    var isFromStart = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BottomSheetFilterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)
        initExtra()
        initApiCall()
        clickEvent()
    }

    override fun onResume() {
        super.onResume()
        analyticMethod(SELECT_AREA,"SelectAreaActivity")
    }

    private fun initExtra() {
        isFromStart = intent.getBooleanExtra(FROM_START, false)
    }

    private fun initApiCall() {
        vm.getLocalityList(Constants.TYPE_AREA)
        vm.localityLiveData.observe(this) {
            updateFilterData(it)
        }

        vm.postSelectAreaLiveData.observe(this) {
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
                    // clear first: this observer can fire again (re-entry, config
                    // change) and addAll alone duplicated every locality on screen
                    localityListData.clear()
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
        // The saved selection arrives on the response itself: the server sets
        // isChecked on every locality and subLocality from the caller's stored
        // UserLocality rows. The old checkSelectedData() cross-referenced a local
        // JSON cache in prefs.filterData instead — but the code that populated that
        // cache (saveData/getData) was commented out, so it looped over a
        // permanently empty list and did nothing at all.
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
            { it, pos, bindingItem ->
                bindingItem.tvCountryName.text = it.name

                var list= it.subLocality?.filter { it1-> !it1.isChecked }
                it.isSelectAll=  if(list?.isEmpty() == true) true else false

                bindingItem.tvSelectAll.text= if(it.isSelectAll){
                    getString(R.string.un_select_all)
                }else getString(R.string.select_all)

                bindingItem.tvSelectAll.setOnClickListener { view->
                    localityListData[pos].subLocality?.forEach { it2->
                        it2.isChecked= if(it.isSelectAll) false else true
                    }
                    binding.rvMainFilter.adapter?.notifyItemChanged(pos)
                }

                val subLocalityList = ArrayList(it.subLocality ?: ArrayList())
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

    private fun clickEvent() {
        binding.tvDone.setOnClickListener {
            var postList = ArrayList<PostAreaData>()

            localityListData.forEach { loc ->
                loc.subLocality?.forEach {
                    if (it.isChecked) {
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
            }else showToast(this,"Please select a area before proceeding")

        }
    }


    private fun updateSelectAreaData(response: Resource<CommonResponse>) {
        when (response.state) {
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                if (response.data?.code == CODE_UPDATED) {
                    if (isFromStart) {
                        prefs.isAreaSelected = true
                        val intent = Intent(this, MapsActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        intent.putParcelableArrayListExtra(LOCALITY_DATA, localityListData)
                        startActivity(intent)
                        finish()
                    } else finish()

                } else showSnack(response.data?.message.toString())
            }

            ResourceState.ERROR -> {
                binding.progress.gone()
                showAlert(response.message)
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if(isFromStart) exitApp()

    }

    fun exitApp() {
        finishAffinity()
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}