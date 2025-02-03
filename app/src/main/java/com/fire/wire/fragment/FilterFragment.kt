package com.fire.wire.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.fire.wire.R
import com.fire.wire.ReplaceCallback
import com.fire.wire.adapter.setUpAdapter
import com.fire.wire.databinding.BottomSheetFilterBinding
import com.fire.wire.databinding.ItemSubFilterBinding
import com.fire.wire.databinding.ItemSubFilterItemBinding
import com.fire.wire.model.incident.response.FilterData
import com.fire.wire.model.locality.Locality
import com.fire.wire.model.locality.LocalityData
import com.fire.wire.model.locality.LocalityResponse
import com.fire.wire.prefs
import com.fire.wire.resource.Resource
import com.fire.wire.resource.ResourceState
import com.fire.wire.utils.*
import com.fire.wire.utils.IntentUtils.FILTER_DATA
import com.fire.wire.utils.IntentUtils.FROM_WIRE
import com.fire.wire.viewModel.FireWireViewModel
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class FilterFragment : Fragment() {

    private lateinit var binding: BottomSheetFilterBinding
    private var callback: ReplaceCallback? = null
    private lateinit var vm: FireWireViewModel
    private var localityListData = ArrayList<Locality>()
    private var filterLocalityList = ArrayList<Locality>()

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        clickEvent()
        getData()
    }

    private fun initViewModel() {
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)
        vm.getLocalityList()
        vm.localityLiveData.observe(viewLifecycleOwner) {
            updateFilterData(it)
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
        checkSelectedData()
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
                bindingItem.tvSelectAll.setOnClickListener {
                    localityListData[pos].subLocality?.map { it.isChecked  }
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

    private fun checkSelectedData() {
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
    }

    private fun clickEvent() {
        binding.tvDone.setOnClickListener {
            localityListData.forEach { loc ->
                if (loc.subLocality?.find { it.isChecked } != null) {
                    loc.isChecked = true
                }
            }
            val localityId = ArrayList<String>()
            localityId.addAll(localityListData.filter { it.isChecked }.map { it._id ?: "" })

            val subLocalityId = ArrayList<String>()
            localityListData.filter { it.isChecked }.forEach { subItem ->
                subLocalityId.addAll(subItem.subLocality?.filter { it.isChecked }
                    ?.map { it._id ?: "" }!!)
            }

            val bundle = Bundle()
            bundle.putParcelable(FILTER_DATA, FilterData(localityId, subLocalityId))
            bundle.putBoolean(FROM_WIRE, true)
            callback?.replaceFragment(NAV_WIRE_NEWS, bundle)

            saveData()
        }
    }

    private fun saveData() {
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
    }


    fun showAlert(message: String? = "") {
        AlertDialog.Builder(requireContext())
            .setTitle(resources.getString(R.string.app_name))
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
            }
            .show()
    }
}