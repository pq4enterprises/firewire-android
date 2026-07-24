package com.fire.wire.activity

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.fire.wire.R
import com.fire.wire.adapter.setUpAdapter
import com.fire.wire.databinding.ActivityAreasAlertsBinding
import com.fire.wire.databinding.ItemAreaGroupBinding
import com.fire.wire.databinding.ItemAreaRowBinding
import com.fire.wire.model.locality.Locality
import com.fire.wire.model.user.request.UserAreaItem
import com.fire.wire.model.user.request.UserNotificationItem
import com.fire.wire.model.user.response.CommonResponse
import com.fire.wire.prefs
import com.fire.wire.resource.Resource
import com.fire.wire.resource.ResourceState
import android.content.Intent
import com.fire.wire.utils.AlertSounds
import com.fire.wire.utils.Constants
import com.fire.wire.utils.gone
import com.fire.wire.utils.visible
import com.fire.wire.viewModel.FireWireViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * AREAS & ALERTS (design screen 13) — replaces the old Personalization hub
 * (feed areas + notification settings) with a single toggle list.
 *
 * FEED column  -> POST api/app/user/area          (UserLocality rows; drives the wire feed)
 * ALERT column -> POST api/app/user/notification  (Notification rows; drives push alerts)
 * Both selections are read back from GET api/app/locality with
 * query {"type":"area"} / {"type":"notification"} respectively.
 */
@AndroidEntryPoint
class AreasAlertsActivity : BaseActivity() {

    private data class AreaRow(
        val localityId: String,
        val subLocalityId: String,
        val name: String,
        var feedOn: Boolean,
        var alertOn: Boolean // kept even while feed is off so re-enabling feed restores the bell
    )

    private data class AreaGroup(
        val localityId: String,
        val name: String,
        val rows: MutableList<AreaRow>
    )

    private lateinit var binding: ActivityAreasAlertsBinding
    private lateinit var vm: FireWireViewModel

    private val groups = ArrayList<AreaGroup>()

    // collapsible region headers (scanner-screen pattern); default COLLAPSED —
    // SELECT ALL and SAVE operate on the data model, so they keep working for
    // groups whose rows aren't currently on screen
    private val expandedGroups = HashMap<String, Boolean>()

    // server state at load time, for change detection on SAVE
    private var initialFeedIds = setOf<String>()
    private var initialAlertIds = setOf<String>()

    // existing unit / incident-type alert selections; not editable on this
    // screen but re-sent on save so the server's diff keeps them
    private val preservedUnitRows = ArrayList<Pair<String, String>>()          // localityId to unitId
    private val preservedIncidentTypeRows = ArrayList<Pair<String, String>>()  // localityId to typeId

    private var areaResponse: List<Locality>? = null
    private var alertResponse: List<Locality>? = null
    private var pendingSaves = 0

    // selection sets being committed; promoted to initial* once the save lands
    private var savingFeedIds = setOf<String>()
    private var savingAlertIds = setOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAreasAlertsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)

        initToolbar()
        initApiCall()
        clickEvent()
    }

    private fun initToolbar() {
        binding.toolbar.tvToolbarTitle.text = getString(R.string.areas_alerts)
        binding.toolbar.tvToolbarTitle.visible()
        binding.toolbar.ivFeed.gone()
        binding.toolbar.ivMenu.setOnClickListener {
            finish()
        }
    }

    private fun initApiCall() {
        vm.getAreaLocalityList()
        vm.getAlertLocalityList()

        vm.areaLocalityLiveData.observe(this, Observer { response ->
            when (response.state) {
                ResourceState.LOADING -> binding.progress.visible()
                ResourceState.SUCCESS -> {
                    areaResponse = response.data?.data?.data
                    onListsLoaded()
                }
                ResourceState.ERROR -> {
                    binding.progress.gone()
                    showAlert(response.message)
                }
            }
        })

        vm.alertLocalityLiveData.observe(this, Observer { response ->
            when (response.state) {
                ResourceState.LOADING -> binding.progress.visible()
                ResourceState.SUCCESS -> {
                    alertResponse = response.data?.data?.data
                    onListsLoaded()
                }
                ResourceState.ERROR -> {
                    binding.progress.gone()
                    showAlert(response.message)
                }
            }
        })

        vm.saveAreasLiveData.observe(this, Observer { onSaveResult(it) })
        vm.saveAlertsLiveData.observe(this, Observer { onSaveResult(it) })
    }

    /** Builds the toggle model once BOTH the feed and alert selections are in. */
    private fun onListsLoaded() {
        val areas = areaResponse ?: return
        val alerts = alertResponse ?: return
        binding.progress.gone()

        val alertCheckedSubIds = HashSet<String>()
        preservedUnitRows.clear()
        preservedIncidentTypeRows.clear()
        alerts.forEach { locality ->
            val localityId = locality._id ?: return@forEach
            locality.subLocality?.forEach { sub ->
                if (sub.isChecked) sub._id?.let { alertCheckedSubIds.add(it) }
            }
            locality.unit?.forEach { unit ->
                if (unit.isChecked) unit._id?.let { preservedUnitRows.add(localityId to it) }
            }
            locality.incidentType?.forEach { type ->
                if (type.isChecked) type._id?.let { preservedIncidentTypeRows.add(localityId to it) }
            }
        }

        groups.clear()
        val feedIds = HashSet<String>()
        areas.forEach { locality ->
            val localityId = locality._id ?: return@forEach
            val rows = ArrayList<AreaRow>()
            locality.subLocality?.forEach { sub ->
                val subId = sub._id ?: return@forEach
                if (sub.isChecked) feedIds.add(subId)
                rows.add(
                    AreaRow(
                        localityId = localityId,
                        subLocalityId = subId,
                        name = sub.name ?: "",
                        feedOn = sub.isChecked,
                        alertOn = alertCheckedSubIds.contains(subId)
                    )
                )
            }
            if (rows.isNotEmpty()) {
                groups.add(AreaGroup(localityId, locality.name ?: "", rows))
            }
        }
        groups.sortWith(compareBy({ regionRank(it.name) }, { it.name.uppercase() }))
        initialFeedIds = feedIds
        initialAlertIds = alertCheckedSubIds.toSet()

        setupAdapter()
    }

    // Region display priority — first match wins; unlisted regions follow alphabetically.
    // Adjust this list to change the on-screen order. Future: drive from a
    // portal-managed Locality.sort field once the backend adds one.
    private val regionOrder = listOf(
        "NEW YORK CITY",
        "LONG ISLAND",
        "USA",
        "UNITED STATES",
        "CANADA",
        "EUROPE",
        "UNITED KINGDOM"
    )

    private fun regionRank(name: String): Int {
        val i = regionOrder.indexOf(name.trim().uppercase())
        return if (i >= 0) i else regionOrder.size
    }

    private fun setupAdapter() {
        if (groups.isNotEmpty()) {
            binding.tvNoData.gone()
            binding.rvGroups.visible()
        } else {
            binding.tvNoData.visible()
            binding.rvGroups.gone()
        }

        binding.rvGroups.setUpAdapter(
            groups,
            R.layout.item_area_group,
            ItemAreaGroupBinding::inflate,
            { group, pos, groupBinding ->
                groupBinding.tvRegion.text = group.name
                updateSelectAllLabel(group, groupBinding)

                // collapsible dropdown, mirroring the scanner region headers:
                // rotating chevron, tap the header to toggle, default collapsed
                val expanded = expandedGroups[group.localityId] ?: false
                groupBinding.ivChevron.rotation = if (expanded) 90f else 0f
                groupBinding.llRowsCard.visibility =
                    if (expanded) android.view.View.VISIBLE else android.view.View.GONE
                groupBinding.llGroupHeader.setOnClickListener {
                    expandedGroups[group.localityId] = !expanded
                    binding.rvGroups.adapter?.notifyItemChanged(pos)
                }

                // SELECT ALL consumes its own tap — it never collapses the group
                groupBinding.tvSelectAll.setOnClickListener {
                    val allOn = group.rows.all { it.feedOn }
                    group.rows.forEach { it.feedOn = !allOn }
                    groupBinding.rvRows.adapter?.notifyDataSetChanged()
                    updateSelectAllLabel(group, groupBinding)
                }

                groupBinding.rvRows.setUpAdapter(
                    group.rows,
                    R.layout.item_area_row,
                    ItemAreaRowBinding::inflate,
                    { row, pos, rowBinding ->
                        rowBinding.tvAreaName.text = row.name
                        rowBinding.divider.visibility =
                            if (pos == group.rows.size - 1) android.view.View.GONE
                            else android.view.View.VISIBLE

                        rowBinding.swFeed.setOnCheckedChangeListener(null)
                        rowBinding.swAlert.setOnCheckedChangeListener(null)

                        rowBinding.swFeed.isChecked = row.feedOn
                        bindAlertSwitch(row, rowBinding)

                        rowBinding.swFeed.setOnCheckedChangeListener { _, checked ->
                            row.feedOn = checked
                            rowBinding.swAlert.setOnCheckedChangeListener(null)
                            bindAlertSwitch(row, rowBinding)
                            updateSelectAllLabel(group, groupBinding)
                        }
                    }
                )
            }
        )
    }

    /** ALERT switch is only live while FEED is on; shown off + dimmed otherwise. */
    private fun bindAlertSwitch(row: AreaRow, rowBinding: ItemAreaRowBinding) {
        rowBinding.swAlert.isChecked = row.feedOn && row.alertOn
        rowBinding.swAlert.isEnabled = row.feedOn
        rowBinding.swAlert.alpha = if (row.feedOn) 1f else 0.4f
        if (row.feedOn) {
            rowBinding.swAlert.setOnCheckedChangeListener { _, checked ->
                row.alertOn = checked
            }
        }
    }

    private fun updateSelectAllLabel(group: AreaGroup, groupBinding: ItemAreaGroupBinding) {
        val allOn = group.rows.isNotEmpty() && group.rows.all { it.feedOn }
        groupBinding.tvSelectAll.text =
            if (allOn) getString(R.string.areas_alerts_unselect_all)
            else getString(R.string.areas_alerts_select_all)
    }

    private fun clickEvent() {
        binding.btnSave.setOnClickListener {
            saveChanges()
        }
        binding.cardAlertSound.setOnClickListener {
            startActivity(Intent(this, AlertSoundActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // refresh after returning from the ALERT SOUND screen
        binding.tvCurrentSound.text = AlertSounds.current(prefs).displayName
    }

    /** Commits only the changed selection sets, then finishes. */
    private fun saveChanges() {
        if (groups.isEmpty()) {
            finish()
            return
        }
        val userId = prefs.userId ?: ""

        val allRows = groups.flatMap { it.rows }
        val feedRows = allRows.filter { it.feedOn }
        val feedIds = feedRows.map { it.subLocalityId }.toSet()
        // an alert only counts while its area is in the feed
        val alertRows = allRows.filter { it.feedOn && it.alertOn }
        val alertIds = alertRows.map { it.subLocalityId }.toSet()

        pendingSaves = 0
        savingFeedIds = feedIds
        savingAlertIds = alertIds

        if (feedIds != initialFeedIds) {
            pendingSaves++
            vm.saveUserAreas(feedRows.map {
                UserAreaItem(userId, it.localityId, it.subLocalityId)
            })
        }

        if (alertIds != initialAlertIds) {
            pendingSaves++
            val payload = ArrayList<UserNotificationItem>()
            alertRows.forEach {
                payload.add(UserNotificationItem(userId, it.subLocalityId, "subLocality"))
            }
            // keep unit / incident-type alert rows the server already has —
            // this endpoint replaces the full set, so omitting them deletes them
            preservedUnitRows.forEach { (_, unitId) ->
                payload.add(UserNotificationItem(userId, unitId, "unit"))
            }
            preservedIncidentTypeRows.forEach { (_, typeId) ->
                payload.add(UserNotificationItem(userId, typeId, "incidentType"))
            }
            // parent locality rows, mirroring iOS: one per locality with any selection
            val localityIds = LinkedHashSet<String>()
            alertRows.forEach { localityIds.add(it.localityId) }
            preservedUnitRows.forEach { localityIds.add(it.first) }
            preservedIncidentTypeRows.forEach { localityIds.add(it.first) }
            localityIds.forEach {
                payload.add(UserNotificationItem(userId, it, "locality"))
            }
            vm.saveUserNotifications(payload)
        }

        if (pendingSaves == 0) {
            // nothing changed
            showSnack(getString(R.string.settings_updated))
        }
    }

    private fun onSaveResult(response: Resource<CommonResponse>) {
        when (response.state) {
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                val code = response.data?.code
                if (code == "updated" || code == Constants.CODE_SUCCESS) {
                    pendingSaves--
                    if (pendingSaves <= 0) {
                        binding.progress.gone()
                        initialFeedIds = savingFeedIds
                        initialAlertIds = savingAlertIds
                        showSnack(getString(R.string.settings_updated))
                    }
                } else {
                    binding.progress.gone()
                    showAlert(response.data?.message.toString())
                }
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showAlert(response.message)
            }
        }
    }
}
