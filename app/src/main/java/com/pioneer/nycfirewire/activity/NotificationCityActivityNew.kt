package com.pioneer.nycfirewire.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.ActivityNotificationCityBinding
import com.pioneer.nycfirewire.model.locality.*
import com.pioneer.nycfirewire.model.user.request.NotificationAreaData
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.*
import com.pioneer.nycfirewire.viewModel.FireWireViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext



@AndroidEntryPoint
class NotificationCityActivityNew : BaseActivity() {

    private lateinit var binding: ActivityNotificationCityBinding
    private val vm: FireWireViewModel by viewModels()

    private var locality = Locality()
    private var localityId = ""
    private var localityName = ""

    private var subLocalityList = mutableListOf<SubLocality>()
    private var unitList = mutableListOf<FireUnit>()
    private var incidentTypeList = mutableListOf<IncidentType>()
    private var allLocation = mutableListOf<Locality>()

    private var debounceJob: Job? = null

    // Declare adapters later
    private lateinit var subLocalityAdapter: SubNotificationAdapter
    private lateinit var unitAdapter: UnitNotificationAdapter
    private lateinit var incidentAdapter: IncidentNotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationCityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initExtra()
        setupToolbar()
        setupRecyclerViews() // Just animator setup
        setupClickListeners()

        observeData()
        vm.getLocalityList(Constants.TYPE_NOTIFICATION)
    }

    private fun initExtra() {
        localityId = intent.getStringExtra(Constants.LOCALITY_DATA).orEmpty()
        localityName = intent.getStringExtra(Constants.LOCALITY_NAME).orEmpty()
    }

    private fun setupToolbar() {
        binding.toolbarLayout.tvTitle.text = getString(R.string.notification_setting)
        binding.toolbarLayout.tvBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerViews() {
        (binding.rvSubLocality.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        (binding.rvUnits.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        (binding.rvIncidentType.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

    }

    private fun observeData() {
        vm.localityLiveData.observe(this) {
            when (it.state) {
                ResourceState.LOADING -> binding.progressRl.visible()
                ResourceState.SUCCESS -> {
                    binding.progressRl.gone()
                    it.data?.data?.data?.let { list ->
                        allLocation = list.toMutableList()
                        locality = allLocation.find { loc -> loc._id == localityId } ?: Locality()
                        binding.tvTitleLocality.text = locality.name
                        updateLocalityData()
                    }
                }
                ResourceState.ERROR -> binding.progressRl.gone()
            }
        }

        vm.postNotificationAreaLiveData.observe(this) {
            when (it.state) {
                ResourceState.LOADING -> binding.progressRl.visible()
                ResourceState.SUCCESS -> {
                    binding.progressRl.gone()
                    if (it.data?.code == Constants.CODE_UPDATED) showSnack("Settings updated successfully!")
                    else showAlert(it.data?.message ?: "Unknown error")
                }
                ResourceState.ERROR -> {
                    binding.progressRl.gone()
                    showAlert(it.message)
                }
            }
        }
    }

    private fun setupAdapters() {
        subLocalityAdapter = SubNotificationAdapter { pos ->
            if (pos in subLocalityList.indices) {
                subLocalityList[pos].isChecked = !subLocalityList[pos].isChecked
                subLocalityAdapter.submitList(subLocalityList.toList())
                updateSelectAllFunction()
            }
        }
        unitAdapter = UnitNotificationAdapter { pos ->
            if (pos in unitList.indices) {
                unitList[pos].isChecked = !unitList[pos].isChecked
                unitAdapter.submitList(unitList.toList())
                updateSelectAllFunction()
            }
        }

        incidentAdapter = IncidentNotificationAdapter { pos ->
            if (pos in incidentTypeList.indices) {
                incidentTypeList[pos].isChecked = !incidentTypeList[pos].isChecked
                incidentAdapter.submitList(incidentTypeList.toList())
                updateSelectAllFunction()
            }
        }

        binding.rvSubLocality.layoutManager = LinearLayoutManager(this)
        binding.rvUnits.layoutManager = LinearLayoutManager(this)
        binding.rvIncidentType.layoutManager = LinearLayoutManager(this)

        bindAdapterWithOptionalDelay(
            listSize = subLocalityList.size,
            adapterSetter = {
                binding.rvSubLocality.adapter = subLocalityAdapter
            },
            listSubmitter = {
                subLocalityAdapter.submitList(subLocalityList.toList())
            },
            onStartLoading = {
                binding.proSubLoc.visible()
            },
            onStopLoading = {
                binding.proSubLoc.gone()
            }
        )

        bindAdapterWithOptionalDelay(
            listSize = unitList.size,
            adapterSetter = {
                binding.rvUnits.adapter = unitAdapter
            },
            listSubmitter = {
                unitAdapter.submitList(unitList.toList())
            },
            onStartLoading = {
                binding.proUnits.visible()
            },
            onStopLoading = {
                binding.proUnits.gone()
            }
        )

        bindAdapterWithOptionalDelay(
            listSize = incidentTypeList.size,
            adapterSetter = {
                binding.rvIncidentType.adapter = incidentAdapter
            },
            listSubmitter = {
                incidentAdapter.submitList(incidentTypeList.toList())
            },
            onStartLoading = {
                binding.proIncident.visible()
            },
            onStopLoading = {
                binding.proIncident.gone()
            }
        )


        /* if (subLocalityList.size > 100 || unitList.size > 100 || incidentTypeList.size > 100) {
             // Large data, delay adapter setup to prevent UI block
             Handler(Looper.getMainLooper()).postDelayed({
                 binding.rvSubLocality.adapter = subLocalityAdapter
                 binding.rvUnits.adapter = unitAdapter
                 binding.rvIncidentType.adapter = incidentAdapter
             }, 200)
         } else {
             // Small data, safe to bind immediately
             binding.rvSubLocality.adapter = subLocalityAdapter
             binding.rvUnits.adapter = unitAdapter
             binding.rvIncidentType.adapter = incidentAdapter
         }


         subLocalityAdapter.submitList(subLocalityList.toList())
         unitAdapter.submitList(unitList.toList())
         incidentAdapter.submitList(incidentTypeList.toList())*/

        updateSelectAllFunction()

        binding.tvNoData.isVisible = subLocalityList.isEmpty()
        binding.rvSubLocality.isVisible = subLocalityList.isNotEmpty()
        binding.tvTitleIncidentType.isVisible = incidentTypeList.isNotEmpty()
    }

    private fun updateLocalityData() {


        lifecycleScope.launch(Dispatchers.Default) {
            val subList = locality.subLocality?.toMutableList() ?: mutableListOf()
            val unitListLocal = locality.unit?.toMutableList() ?: mutableListOf()
            val incidentList = locality.incidentType?.toMutableList() ?: mutableListOf()

            withContext(Dispatchers.Main) {
                subLocalityList = subList
                unitList = unitListLocal
                incidentTypeList = incidentList

                setupAdapters()
            }
        }



    }


    private fun setupClickListeners() {
        binding.btnProceed.setOnClickListener { postNotificationSettings() }

        binding.tvCitySelectAll.setOnClickListener { toggleSelectAll() }

        binding.tvLocalitySelectAll.setOnClickListener {
            val newState = !isSelectAllSubLocality()
            subLocalityList.forEach { it.isChecked = newState }
            subLocalityAdapter.submitList(subLocalityList.toList())
            updateSelectAllFunction()
        }

        binding.tvUnitSelectAll.setOnClickListener {
            val newState = !isSelectAllUnit()
            unitList.forEach { it.isChecked = newState }
            unitAdapter.submitList(unitList.toList())
            updateSelectAllFunction()
        }

        binding.tvIncidentSelectAll.setOnClickListener {
            val newState = !isSelectAllIncident()
            incidentTypeList.forEach { it.isChecked = newState }
            incidentAdapter.submitList(incidentTypeList.toList())
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
                    it.isChecked=if(isSelectAllUnit()) false else true
                    binding.rvUnits.adapter?.notifyItemChanged(index)
                }
                binding.tvUnitSelectAll.visible()
                updateSelectAllFunction()
            }
            /*if (unitList.size > 100) {
                Toast.makeText(this, "Kindly wait until selection completes", Toast.LENGTH_LONG).show()
            }
            debounceJob?.cancel()
            debounceJob = lifecycleScope.launch(Dispatchers.Default) {
                val newState = !isSelectAllUnit()
                unitList.forEach { it.isChecked = newState }
                withContext(Dispatchers.Main) {
                    unitAdapter.submitList(unitList.toList())
                    updateSelectAllFunction()
                }
            }*/
        }
    }

    private fun updateSelectAllFunction() {
        binding.tvLocalitySelectAll.text = if (isSelectAllSubLocality()) getString(R.string.un_select_all) else getString(R.string.select_all)
        binding.tvUnitSelectAll.text = if (isSelectAllUnit()) getString(R.string.un_select_all) else getString(R.string.select_all)
        binding.tvIncidentSelectAll.text = if (isSelectAllIncident()) getString(R.string.un_select_all) else getString(R.string.select_all)
        binding.tvCitySelectAll.text = if (isSelectAllSubLocality() && isSelectAllUnit() && isSelectAllIncident()) getString(R.string.un_select_all) else getString(R.string.select_all)
    }

    private fun isSelectAllSubLocality() = subLocalityList.all { it.isChecked }
    private fun isSelectAllUnit() = unitList.all { it.isChecked }
    private fun isSelectAllIncident() = incidentTypeList.all { it.isChecked }

    private fun toggleSelectAll() {
        val newState = !(isSelectAllSubLocality() && isSelectAllUnit() && isSelectAllIncident())
        subLocalityList.forEach { it.isChecked = newState }
        unitList.forEach { it.isChecked = newState }
        incidentTypeList.forEach { it.isChecked = newState }

        subLocalityAdapter.submitList(subLocalityList.toList())
        unitAdapter.submitList(unitList.toList())
        incidentAdapter.submitList(incidentTypeList.toList())

        updateSelectAllFunction()
    }

    private fun postNotificationSettings() {
        val postList = mutableListOf<NotificationAreaData>()

        allLocation.forEach { loc ->
            if (loc._id != locality._id) {
                loc.subLocality?.filter { it.isChecked }?.forEach {
                    postList.add(NotificationAreaData(prefs.userId.toString(), it._id.toString(), "subLocality"))
                }
                loc.unit?.filter { it.isChecked }?.forEach {
                    postList.add(NotificationAreaData(prefs.userId.toString(), it._id.toString(), "unit"))
                }
                loc.incidentType?.filter { it.isChecked }?.forEach {
                    postList.add(NotificationAreaData(prefs.userId.toString(), it._id.toString(), "incidentType"))
                }
                if (
                    loc.subLocality?.any { it.isChecked } == true ||
                    loc.unit?.any { it.isChecked } == true ||
                    loc.incidentType?.any { it.isChecked } == true
                ) {
                    postList.add(NotificationAreaData(prefs.userId.toString(), loc._id.toString(), "locality"))
                }
            }
        }

        postList.add(NotificationAreaData(prefs.userId.toString(), localityId, "locality"))

        locality.subLocality?.filter { it.isChecked }?.forEach {
            postList.add(NotificationAreaData(prefs.userId.toString(), it._id.toString(), "subLocality"))
        }
        locality.unit?.filter { it.isChecked }?.forEach {
            postList.add(NotificationAreaData(prefs.userId.toString(), it._id.toString(), "unit"))
        }
        locality.incidentType?.filter { it.isChecked }?.forEach {
            postList.add(NotificationAreaData(prefs.userId.toString(), it._id.toString(), "incidentType"))
        }

        vm.postNotificationArea(postList as ArrayList<NotificationAreaData>)
    }


    private fun bindAdapterWithOptionalDelay(
        listSize: Int,
        threshold: Int = 100,
        delay: Long = 300,
        adapterSetter: () -> Unit,
        listSubmitter: () -> Unit,
        onStartLoading: (() -> Unit)? = null,
        onStopLoading: (() -> Unit)? = null
    ) {
        if (listSize > threshold) {
            onStartLoading?.invoke()
            Handler(Looper.getMainLooper()).postDelayed({
                adapterSetter()
                listSubmitter()
                onStopLoading?.invoke()
            }, delay)
        } else {
            adapterSetter()
            listSubmitter()
            onStopLoading?.invoke()
        }
    }
}
