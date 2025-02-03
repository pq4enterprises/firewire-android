package com.fire.wire.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.fire.wire.R
import com.fire.wire.ReplaceCallback
import com.fire.wire.activity.FeedsActivity
import com.fire.wire.adapter.setUpAdapter
import com.fire.wire.databinding.BottomSheetFeedFilterBinding
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
import java.util.*


@AndroidEntryPoint
class FeedFilterFragment : Fragment() {

    private lateinit var binding: BottomSheetFeedFilterBinding
    private var callback: ReplaceCallback? = null
    private lateinit var vm: FireWireViewModel
    private var localityListData = ArrayList<Locality>()
    private var filterLocalityList = ArrayList<Locality>()

    companion object {
        fun newInstance() = FeedFilterFragment().putArgs { }
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
        binding = BottomSheetFeedFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        clickEvent()
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
            binding.rvSubFilter.visible()
        } else {
            binding.tvNoData.visible()
            binding.rvSubFilter.gone()
        }


        binding.rvSubFilter.setUpAdapter(
            localityListData,
            R.layout.item_sub_filter_item,
            ItemSubFilterItemBinding::inflate,
            { it, pos, bindingItem ->

                bindingItem.cbCountry.isChecked = it.isChecked
                bindingItem.tvSubCountry.text = it.name
                bindingItem.llCb.setOnClickListener { view ->
                    it.isChecked = !it.isChecked
                    binding.rvSubFilter.adapter?.notifyItemChanged(pos)
                }

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
        binding.tvSelectAll.setOnClickListener {
            localityListData.map { it.isChecked  }
            binding.rvSubFilter.adapter?.notifyDataSetChanged()
        }

        binding.tvDone.setOnClickListener {

            val localityId = ArrayList<String>()
            localityId.addAll(localityListData.filter { it.isChecked }.map { it._id ?: "" })

            val intent = Intent(requireContext(), FeedsActivity::class.java )
            intent.putStringArrayListExtra("LocalityId",localityId)
            startActivity(intent)
            activity?.finish()
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