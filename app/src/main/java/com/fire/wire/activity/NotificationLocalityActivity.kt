package com.fire.wire.activity

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.fire.wire.R
import com.fire.wire.adapter.setUpAdapter
import com.fire.wire.databinding.ActivityNotificationLocalityBinding
import com.fire.wire.databinding.ItemLocalityBinding
import com.fire.wire.model.locality.Locality
import com.fire.wire.model.locality.LocalityResponse
import com.fire.wire.resource.Resource
import com.fire.wire.resource.ResourceState
import com.fire.wire.utils.Constants
import com.fire.wire.utils.Constants.LOCALITY_DATA
import com.fire.wire.utils.Constants.LOCALITY_NAME
import com.fire.wire.utils.gone
import com.fire.wire.utils.visible
import com.fire.wire.viewModel.FireWireViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationLocalityActivity : BaseActivity() {

    private lateinit var binding: ActivityNotificationLocalityBinding
    private lateinit var vm: FireWireViewModel
    private var localityListData= ArrayList<Locality>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityNotificationLocalityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)
        binding.toolbarLayout.tvTitle.text= getString(R.string.notification_setting)

        clickEvent()

    }

    private fun initApiCall() {
        vm.getLocalityList()
        vm.localityLiveData.observe(this, Observer {
            updateLocalityData(it)
        })
    }

    private fun updateLocalityData(response: Resource<LocalityResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                if(response.data?.code== Constants.CODE_SUCCESS) {
                    val localityList= response.data.data
                    val list= ArrayList(localityList?.data?:ArrayList())
                    localityListData.clear()
                    localityListData.addAll(list)
                    setupAdapter()

                }else{
                    showAlert(response.data?.message.toString())
                }


            }
            ResourceState.ERROR -> {
                binding.progress.gone()

            }
        }
    }

    private fun setupAdapter() {
        if(localityListData.isNotEmpty()){
            binding.tvNoData.gone()
            binding.rvLocality.visible()
        }else{
            binding.tvNoData.visible()
            binding.rvLocality.gone()
        }

        binding.rvLocality.setUpAdapter(
            localityListData,
            R.layout.item_locality,
            ItemLocalityBinding::inflate,
            { it,pos,bindingItem->
                bindingItem.tvCountryName.text= it.name
                bindingItem.tvCountryName.setOnClickListener { view->
                    moveToActivity(it,it.name.toString())
                }
            }
        )
    }

    private fun clickEvent() {
        binding.toolbarLayout.tvBack.setOnClickListener {
            finish()
        }
    }
    private fun moveToActivity(locality: Locality, name: String) {
        val intent= Intent(this,NotificationCityActivity::class.java)
        intent.putExtra(LOCALITY_DATA, locality)
        intent.putExtra(LOCALITY_NAME,name)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        initApiCall()
    }
}