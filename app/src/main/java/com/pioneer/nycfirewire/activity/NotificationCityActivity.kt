package com.pioneer.nycfirewire.activity

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.SimpleItemAnimator
import com.pioneer.nycfirewire.adapter.setUpAdapter
import com.pioneer.nycfirewire.model.locality.Locality
import com.pioneer.nycfirewire.model.user.response.CommonResponse
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.Constants
import com.pioneer.nycfirewire.utils.Constants.LOCALITY_NAME
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.visible
import com.pioneer.nycfirewire.viewModel.FireWireViewModel
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.ActivityNotificationCityBinding
import com.pioneer.nycfirewire.databinding.ItemSubNotificationCbBinding
import com.pioneer.nycfirewire.model.locality.IncidentType
import com.pioneer.nycfirewire.model.locality.LocalityResponse
import com.pioneer.nycfirewire.model.locality.SubLocality
import com.pioneer.nycfirewire.model.locality.FireUnit
import com.pioneer.nycfirewire.model.user.request.NotificationAreaData
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.resource.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@AndroidEntryPoint
class NotificationCityActivity : BaseActivity(){

    private lateinit var binding: ActivityNotificationCityBinding
    private lateinit var vm: FireWireViewModel
    private var locality= Locality()
    private var localityName=""
    private var localityId=""

    private var subLocalityList= ArrayList<SubLocality>()
    private var unitList= ArrayList<FireUnit>()
    private var incidentTypeList= ArrayList<IncidentType>()
    private var allLocation= ArrayList<Locality>()

    var isSelectAllSubLocality= false
    var isSelectAllIncident= false
    var isSelectAllUnit= false
    var isAllSelect=false
    private var debounceJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityNotificationCityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)
        binding.toolbarLayout.tvTitle.text= getString(R.string.notification_setting)

        initExtra()
        clickEvent()

        vm.postNotificationAreaLiveData.observe(this, Observer {
            updateList(it)
        })

        initApiCall()
    }




    private fun initApiCall() {
        vm.getLocalityList(Constants.TYPE_NOTIFICATION)
        vm.localityLiveData.observe(this, Observer {
            updateLocalityData(it)
        })
    }


    private fun updateLocalityData(response: Resource<LocalityResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progressRl.visible()
            ResourceState.SUCCESS -> {
                binding.progressRl.gone()
                if(response.data?.code== Constants.CODE_SUCCESS) {
                    binding.btnProceed.visible()
                    val localityList= response.data.data
                    allLocation= ArrayList(localityList?.data?:ArrayList())

                     locality= allLocation.find { it._id==localityId }!!

                    binding.tvTitleLocality.text= locality.name
                    binding.tvUnits.text= getString(R.string.units)
                    setupAdapter()


                }else{
                    showAlert(response.data?.message.toString())
                }


            }
            ResourceState.ERROR -> {
                binding.progressRl.gone()

            }
        }
    }

    private fun updateList(response: Resource<CommonResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progressRl.visible()
            ResourceState.SUCCESS -> {
                binding.progressRl.gone()
                if(response.data?.code== Constants.CODE_UPDATED) {
                    showSnack("Settings updated successfully!")
                }else{
                    showAlert(response.data?.message.toString())
                }
            }
            ResourceState.ERROR -> {
                binding.progressRl.gone()
                showAlert(response.message)
            }
        }
    }

    private fun initExtra() {
        localityId= intent.getStringExtra(Constants.LOCALITY_DATA).toString()
        localityName= intent.getStringExtra(LOCALITY_NAME)?:""

    }


    private fun updateSelectAllFunction(){
        isSelectAllSubLocality= subLocalityList.filter { !it.isChecked }.isEmpty()
        isSelectAllUnit= unitList.filter { !it.isChecked }.isEmpty()
        isSelectAllIncident= incidentTypeList.filter { !it.isChecked }.isEmpty()

        isAllSelect =  if(isSelectAllUnit && isSelectAllSubLocality && isSelectAllIncident) true else false

       updateNamesSelect()
    }

    private fun updateNamesSelect(){
        binding.tvCitySelectAll.text= if(isAllSelect) getString(R.string.un_select_all) else getString(R.string.select_all)
        binding.tvLocalitySelectAll.text= if(isSelectAllSubLocality) getString(R.string.un_select_all) else getString(R.string.select_all)
        binding.tvUnitSelectAll.text= if(isSelectAllUnit) getString(R.string.un_select_all) else getString(R.string.select_all)
        binding.tvIncidentSelectAll.text= if(isSelectAllIncident) getString(R.string.un_select_all) else getString(R.string.select_all)

    }



    private fun setupAdapter() {
        if(locality.subLocality?.isNotEmpty() == true){
            binding.tvNoData.gone()
            binding.rvSubLocality.visible()
        }else{
            binding.tvNoData.visible()
            binding.rvSubLocality.gone()
        }

        subLocalityList= ArrayList(locality.subLocality?:ArrayList())
        unitList= ArrayList(locality.unit?:ArrayList())
        incidentTypeList= ArrayList(locality.incidentType?: ArrayList())

        if(incidentTypeList.isNotEmpty())
            binding.tvTitleIncidentType.visible()
        else binding.tvTitleIncidentType.gone()


        updateSelectAllFunction()

        binding.tvIncidentSelectAll.setOnClickListener{
            incidentTypeList.forEach { it.isChecked= if(isSelectAllIncident) false else true }
            // subLocalityList.map { it.isChecked=if(isSelectAllSubLocality) false else true }
            binding.rvIncidentType.adapter?.notifyDataSetChanged()
            updateSelectAllFunction()
        }

        binding.tvCitySelectAll.setOnClickListener {
            subLocalityList.map { it.isChecked=if(isAllSelect) false else true }
            unitList.map { it.isChecked= if(isAllSelect) false else true }
            incidentTypeList.map { it.isChecked= if(isAllSelect) false else true }

            binding.rvSubLocality.adapter?.notifyDataSetChanged()
            binding.rvUnits.adapter?.notifyDataSetChanged()
            binding.rvIncidentType.adapter?.notifyDataSetChanged()
            updateSelectAllFunction()
        }

        binding.tvLocalitySelectAll.setOnClickListener {
            subLocalityList.forEach { it.isChecked= if(isSelectAllSubLocality) false else true }
           // subLocalityList.map { it.isChecked=if(isSelectAllSubLocality) false else true }
            binding.rvSubLocality.adapter?.notifyDataSetChanged()
            updateSelectAllFunction()
        }
        binding.flUnit.setOnClickListener {
            ( binding.rvUnits.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
            if(unitList.size>100){
                Toast.makeText(this, "Kindly wait! until selection complete", Toast.LENGTH_LONG).show()
                binding.tvUnitSelectAll.gone()
            }

            debounceJob?.cancel()
            debounceJob = CoroutineScope(Dispatchers.Main).launch {
                delay(100) // Small delay to avoid multiple rapid calls
                unitList.forEachIndexed { index,it->
                    // delay(10) // Small delay to avoid multiple rapid calls
                    it.isChecked=if(isSelectAllUnit) false else true
                    binding.rvUnits.adapter?.notifyItemChanged(index)
                }
                binding.tvUnitSelectAll.visible()
                updateSelectAllFunction()
            }



        }

        binding.rvIncidentType.setUpAdapter(
            incidentTypeList,
            R.layout.item_sub_notification_cb,
            ItemSubNotificationCbBinding::inflate,
            { it,pos,bindingItem->
                bindingItem.tvSubName.text= it.optionName
                bindingItem.tvTitle.isChecked= it.isChecked
                bindingItem.tvTitle.setOnClickListener { view->
                    it.isChecked= !it.isChecked
                    binding.rvIncidentType.adapter?.notifyItemChanged(pos)
                }

            }
        )


        binding.rvSubLocality.setUpAdapter(
            subLocalityList,
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

        binding.tvUnits.text= if(unitList.isNullOrEmpty()) "" else getString(R.string.units)
        binding.tvUnitSelectAll.text= if(unitList.isNullOrEmpty()) "" else getString(R.string.select_all)

        binding.rvUnits.setUpAdapter(
            unitList,
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
            var postList= ArrayList<NotificationAreaData>()

            allLocation.forEach { allLoc->
                if(allLoc._id!= locality._id){

                    var subLocality= allLoc.subLocality?.filter { it.isChecked }
                    var unit= allLoc.unit?.filter { it.isChecked }
                    var incidentType= allLoc.incidentType?.filter { it.isChecked }

                    if(subLocality?.isNotEmpty() == true){
                    subLocality.forEach {
                        val data = NotificationAreaData(
                            userId = prefs.userId.toString(),
                            notificationId = it._id.toString(),
                            type = "subLocality"
                        )
                        postList.add(data)
                    }}

                    if(unit?.isNotEmpty() == true){
                    unit.forEach {
                        val data = NotificationAreaData(
                            userId = prefs.userId.toString(),
                            notificationId = it._id.toString(),
                            type = "unit"
                        )
                        postList.add(data)
                    }}

                    if(incidentType?.isNotEmpty() == true){
                        incidentType.forEach {
                            val data = NotificationAreaData(
                                userId = prefs.userId.toString(),
                                notificationId = it._id.toString(),
                                type = "incidentType"
                            )
                            postList.add(data)
                        }

                    }

                    if(subLocality?.isNotEmpty() == true || unit?.isNotEmpty() == true || incidentType?.isNotEmpty() == true){
                        val data = NotificationAreaData(
                            userId = prefs.userId.toString(),
                            notificationId = allLoc._id.toString(),
                            type = "locality"
                        )
                        postList.add(data)
                    }

                }
            }



            locality.subLocality?.filter { it.isChecked }?.forEach {
                    val data = NotificationAreaData(
                        userId = prefs.userId.toString(),
                        notificationId = it._id.toString(),
                        type = "subLocality"
                    )
                postList.add(data)
            }
            locality.unit?.filter { it.isChecked }?.forEach {
                    val data = NotificationAreaData(
                        userId = prefs.userId.toString(),
                        notificationId = it._id.toString(),
                        type = "unit"
                    )
                postList.add(data)
            }

            locality.incidentType?.filter { it.isChecked }?.forEach {
                val data = NotificationAreaData(
                    userId = prefs.userId.toString(),
                    notificationId = it._id.toString(),
                    type = "incidentType"
                )
                postList.add(data)
            }

            var currentSubLoc= locality.subLocality?.filter { it.isChecked }
            var currentUnit= locality.unit?.filter { it.isChecked }
            var currentIncidentType= locality.incidentType?.filter { it.isChecked }

          if(currentSubLoc?.isEmpty() == true && currentUnit?.isEmpty() == true && currentIncidentType?.isEmpty() == true){

          }else{
              val data = NotificationAreaData(
                  userId = prefs.userId.toString(),
                  notificationId = localityId.toString(),
                  type = "locality"
              )
              postList.add(data)
          }

            vm.postNotificationArea(postList)

            /*if(currentSubLoc?.isNotEmpty() == true && currentUnit?.isNotEmpty() == true && currentIncidentType?.isNotEmpty() == true) {
                vm.postNotificationArea(postList)
            }else{
                showSnack(getString(R.string.select_sub_unit_type))
            }*/
        }
    }
}