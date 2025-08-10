package com.pioneer.nycfirewire.activity

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.pioneer.nycfirewire.adapter.setUpAdapter
import com.pioneer.nycfirewire.model.locality.Locality
import com.pioneer.nycfirewire.model.locality.LocalityResponse
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.Constants
import com.pioneer.nycfirewire.utils.Constants.LOCALITY_DATA
import com.pioneer.nycfirewire.utils.Constants.LOCALITY_NAME
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.visible
import com.pioneer.nycfirewire.viewModel.FireWireViewModel
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.ActivityNotificationLocalityBinding
import com.pioneer.nycfirewire.databinding.ItemLocalityBinding
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.utils.Constants.NOTIFICATION_AREAS
import com.pioneer.nycfirewire.utils.Constants.USER_BASIC_USER
import com.pioneer.nycfirewire.utils.IntentUtils.FROM_ACCOUNT
import com.pioneer.nycfirewire.utils.IntentUtils.OTHER
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
        vm.getLocalityList(Constants.TYPE_NOTIFICATION)
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
                    if(prefs.userRole==USER_BASIC_USER){
                        moveToPaymentPage()
                    }else moveToActivity(it,it.name.toString())
                }
            }
        )
    }

    private fun moveToPaymentPage(){
        val intent = Intent(this, MyAccountActivity::class.java)
        intent.putExtra(FROM_ACCOUNT, OTHER)
        startActivity(intent)
    }

    private fun clickEvent() {
        binding.toolbarLayout.tvBack.setOnClickListener {
            finish()
        }
    }
    private fun moveToActivity(locality: Locality, name: String) {
        val intent= Intent(this, NotificationCityActivity::class.java)
        intent.putExtra(LOCALITY_DATA, locality._id)
        intent.putExtra(LOCALITY_NAME,name)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
       // analyticMethod(NOTIFICATION_AREAS,"NotificationLocalityActivity")
        initApiCall()
    }
}