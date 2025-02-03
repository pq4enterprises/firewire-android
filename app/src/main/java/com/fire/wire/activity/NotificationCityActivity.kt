package com.fire.wire.activity

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.fire.wire.R
import com.fire.wire.adapter.setUpAdapter
import com.fire.wire.databinding.ActivityNotificationCityBinding
import com.fire.wire.databinding.ItemSubNotificationCbBinding
import com.fire.wire.model.locality.Locality
import com.fire.wire.model.user.request.LocalityUpdate
import com.fire.wire.model.user.response.CommonResponse
import com.fire.wire.resource.ResourceState
import com.fire.wire.utils.Constants
import com.fire.wire.utils.Constants.LOCALITY_NAME
import com.fire.wire.utils.gone
import com.fire.wire.utils.visible
import com.fire.wire.viewModel.FireWireViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class NotificationCityActivity :BaseActivity(){

    private lateinit var binding: ActivityNotificationCityBinding
    private lateinit var vm: FireWireViewModel
    private var locality= Locality()
    private var localityName=""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityNotificationCityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)
        binding.toolbarLayout.tvTitle.text= getString(R.string.notification_setting)

        initExtra()
        clickEvent()

        vm.updateLocalityLiveData.observe(this, Observer {
            updateList(it)
        })
    }

    private fun updateList(response: com.fire.wire.resource.Resource<CommonResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                if(response.data?.code== Constants.CODE_SUCCESS) {
                    showSnack("Settings update successfully!")
                }else{
                    showAlert(response.data?.message.toString())
                }
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showAlert(response.message)
            }
        }
    }

    private fun initExtra() {
         locality= intent.getParcelableExtra(Constants.LOCALITY_DATA)?:Locality()
        localityName= intent.getStringExtra(LOCALITY_NAME)?:""
        binding.tvTitleLocality.text= locality.name
        binding.tvUnits.text= getString(R.string.units)
        setupAdapter()
    }



    private fun setupAdapter() {
        if(locality.subLocality?.isNotEmpty() == true){
            binding.tvNoData.gone()
            binding.rvSubLocality.visible()
        }else{
            binding.tvNoData.visible()
            binding.rvSubLocality.gone()
        }

        val subLocality= ArrayList(locality.subLocality?:ArrayList())
        val units= ArrayList(locality.unit?:ArrayList())

        binding.tvCitySelectAll.setOnClickListener {
            subLocality.map { it.isChecked=true }
            units.map { it.isChecked=true }

            binding.rvSubLocality.adapter?.notifyDataSetChanged()
            binding.rvUnits.adapter?.notifyDataSetChanged()
        }

        binding.tvLocalitySelectAll.setOnClickListener {
            subLocality.map { it.isChecked=true }
            binding.rvSubLocality.adapter?.notifyDataSetChanged()
        }
        binding.tvUnitSelectAll.setOnClickListener {
            units.map { it.isChecked=true }
            binding.rvUnits.adapter?.notifyDataSetChanged()
        }


        binding.rvSubLocality.setUpAdapter(
            subLocality,
            R.layout.item_sub_notification_cb,
            ItemSubNotificationCbBinding::inflate,
            { it,pos,bindingItem->
                bindingItem.tvSubName.text= it.name
                bindingItem.tvTitle.isChecked= it.isChecked
                bindingItem.tvTitle.setOnClickListener { view->
                    it.isChecked= !it.isChecked
                    binding.rvSubLocality.adapter?.notifyItemChanged(pos)
                }

            }
        )

        binding.rvUnits.setUpAdapter(
            units,
            R.layout.item_sub_notification_cb,
            ItemSubNotificationCbBinding::inflate,
            { it1,pos1,subBindItem->
                subBindItem.tvSubName.text= it1.unitName
                subBindItem.tvTitle.isChecked= it1.isChecked
                subBindItem.tvTitle.setOnClickListener {
                    it1.isChecked= !it1.isChecked
                    binding.rvUnits.adapter?.notifyItemChanged(pos1)
                }
            }

        )

    }

    private fun clickEvent() {
        binding.toolbarLayout.tvBack.setOnClickListener {
            finish()
        }
        binding.btnProceed.setOnClickListener {
            val localityIdList= ArrayList<String>()
            val subLocalityIdList= ArrayList<String>()
            val unitIdList= ArrayList<String>()

            localityIdList.add(locality._id.toString())
            locality.subLocality?.filter { it.isChecked }?.map { it._id.toString() }
                ?.let { it1 -> subLocalityIdList.addAll(it1) }
            locality.unit?.filter { it.isChecked }?.map { it._id.toString() }
                ?.let { it1 -> unitIdList.addAll(it1) }

            val request= LocalityUpdate(
                localityIdList, subLocalityIdList, unitIdList
            )

            vm.updateLocalityProfile(request)
        }
    }
}