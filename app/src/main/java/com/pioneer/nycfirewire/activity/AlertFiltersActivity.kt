package com.pioneer.nycfirewire.activity

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.adapter.AlertFilterRow
import com.pioneer.nycfirewire.adapter.AlertFiltersAdapter
import com.pioneer.nycfirewire.adapter.AlertFiltersAdapter.Kind
import com.pioneer.nycfirewire.databinding.ActivityAlertFiltersBinding
import com.pioneer.nycfirewire.model.alertfilters.AlertFilterOption
import com.pioneer.nycfirewire.model.alertfilters.AlertFiltersData
import com.pioneer.nycfirewire.model.alertfilters.AlertFiltersUpdateRequest
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.Constants
import com.pioneer.nycfirewire.utils.IntentUtils
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.visible
import com.pioneer.nycfirewire.viewModel.FireWireViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * ALERT FILTERS (alert picker, iteration 1 item #2) — for ONE locality, which
 * incident types and which units may alert the user inside their alert areas.
 *
 * Reads  GET api/app/alert-filters/{localityId}
 * Writes PUT api/app/alert-filters/{localityId}  { typeIds, unitIds }
 *        null = ALL (default; the server stores no rows), else the checked ids.
 *
 * Extras: Constants.LOCALITY_ID, Constants.LOCALITY_NAME.
 * Sets RESULT_OK after a successful save so Areas & Alerts can refresh the
 * unit / incident-type rows it re-sends on its own save.
 */
@AndroidEntryPoint
class AlertFiltersActivity : BaseActivity(), AlertFiltersAdapter.Listener {

    private lateinit var binding: ActivityAlertFiltersBinding
    private lateinit var vm: FireWireViewModel
    private lateinit var adapter: AlertFiltersAdapter

    private var localityId = ""
    private var localityName = ""

    // screen state
    private var loaded = false
    private var typesAll = true
    private var unitsAll = true
    private val types = ArrayList<AlertFilterOption>()
    private val units = ArrayList<AlertFilterOption>()
    private val checkedTypeIds = LinkedHashSet<String>()
    private val checkedUnitIds = LinkedHashSet<String>()
    private var unitQuery = ""

    // what the server had at load time, for change detection on SAVE
    private var initialSignature = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertFiltersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)

        localityId = intent.getStringExtra(Constants.LOCALITY_ID) ?: ""
        localityName = intent.getStringExtra(Constants.LOCALITY_NAME) ?: ""

        // same premium gate as the alert switches on Areas & Alerts
        if ((prefs.userRole ?: "") == Constants.USER_BASIC_USER) {
            val paywall = Intent(this, MyAccountActivity::class.java)
            paywall.putExtra(IntentUtils.FROM_ACCOUNT, IntentUtils.OTHER)
            startActivity(paywall)
            finish()
            return
        }
        if (localityId.isEmpty()) {
            finish()
            return
        }

        initToolbar()
        initList()
        initApiCall()
        binding.btnSave.setOnClickListener { saveChanges() }
        setSaveEnabled(false)
    }

    private fun initToolbar() {
        setToolbarTitle(localityName)
        binding.toolbar.tvToolbarTitle.visible()
        binding.toolbar.ivFeed.gone()
        binding.toolbar.ivMenu.setOnClickListener { finish() }
    }

    /**
     * The shared toolbar centers the title between BACK and the right edge with
     * no side constraints, so a long one ("NEW YORK CITY ALERTS") runs under
     * BACK. Keep it to one line, cap its width, and drop the "ALERTS" suffix
     * when the full title would not fit.
     */
    private fun setToolbarTitle(name: String?) {
        val full = getString(R.string.alert_filters_title, name.orEmpty()).uppercase()
        val title = binding.toolbar.tvToolbarTitle
        val density = resources.displayMetrics.density
        val maxWidth = (resources.displayMetrics.widthPixels - 2 * 96 * density).toInt()
        title.maxLines = 1
        title.ellipsize = android.text.TextUtils.TruncateAt.END
        title.maxWidth = maxWidth
        title.text = if (title.paint.measureText(full) <= maxWidth) full else name.orEmpty().uppercase()
    }

    private fun initList() {
        adapter = AlertFiltersAdapter(this)
        // Without a layout manager RecyclerView skips layout entirely and the
        // screen stays blank even though the rows were submitted.
        binding.rvFilters.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.rvFilters.adapter = adapter
        // checkbox rows flip in place; no fade/crossfade churn on long lists
        binding.rvFilters.itemAnimator = null
    }

    override fun onResume() {
        super.onResume()
        analyticMethod(Constants.ALERT_FILTERS, "AlertFiltersActivity")
    }

    private fun initApiCall() {
        vm.alertFiltersLiveData.observe(this, Observer { response ->
            when (response.state) {
                ResourceState.LOADING -> binding.progress.visible()
                ResourceState.SUCCESS -> {
                    binding.progress.gone()
                    val data = response.data?.data
                    if (data == null) {
                        showAlert(response.data?.message ?: getString(R.string.no_data_found))
                        return@Observer
                    }
                    onLoaded(data)
                }
                ResourceState.ERROR -> {
                    binding.progress.gone()
                    showAlert(response.message)
                }
            }
        })

        vm.saveAlertFiltersLiveData.observe(this, Observer { response ->
            when (response.state) {
                ResourceState.LOADING -> binding.progress.visible()
                ResourceState.SUCCESS -> {
                    binding.progress.gone()
                    val code = response.data?.code
                    if (code == "updated" || code == Constants.CODE_SUCCESS) {
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        showAlert(response.data?.message.toString())
                    }
                }
                ResourceState.ERROR -> {
                    binding.progress.gone()
                    showAlert(response.message)
                }
            }
        })

        vm.getAlertFilters(localityId)
    }

    private fun onLoaded(data: AlertFiltersData) {
        if (loaded) return // duplicate LiveData delivery: keep the user's edits
        loaded = true
        if (localityName.isEmpty()) {
            localityName = data.locality?.name ?: ""
            setToolbarTitle(localityName)
        }

        typesAll = data.typesAll
        unitsAll = data.unitsAll
        types.clear(); types.addAll(data.types.orEmpty().filter { !it._id.isNullOrEmpty() })
        units.clear(); units.addAll(data.units.orEmpty().filter { !it._id.isNullOrEmpty() })

        // when ALL is on the server marks every option checked, which is exactly
        // the starting point we want if the user switches ALL off
        checkedTypeIds.clear(); checkedTypeIds.addAll(types.filter { it.checked }.map { it._id!! })
        checkedUnitIds.clear(); checkedUnitIds.addAll(units.filter { it.checked }.map { it._id!! })
        initialSignature = signature()

        if (types.isEmpty() && units.isEmpty()) {
            binding.tvNoData.visible()
            binding.rvFilters.gone()
        } else {
            binding.tvNoData.gone()
            binding.rvFilters.visible()
        }
        setSaveEnabled(true)
        render()
    }

    private fun setSaveEnabled(enabled: Boolean) {
        binding.btnSave.isEnabled = enabled
        binding.btnSave.alpha = if (enabled) 1f else 0.5f
    }

    // ---- rows -------------------------------------------------------------

    private fun render() {
        val rows = ArrayList<AlertFilterRow>(types.size + units.size + 8)
        rows.addAll(sectionRows(Kind.TYPES))
        rows.addAll(sectionRows(Kind.UNITS))
        adapter.submitList(rows)
    }

    private fun sectionRows(kind: Kind): List<AlertFilterRow> {
        val isTypes = kind == Kind.TYPES
        val all = if (isTypes) typesAll else unitsAll
        val options = if (isTypes) types else units
        val checked = if (isTypes) checkedTypeIds else checkedUnitIds

        val summary = when {
            all && isTypes -> getString(R.string.alert_filters_summary_all_types)
            all -> getString(R.string.alert_filters_summary_all_units)
            checked.isEmpty() -> getString(R.string.alert_filters_summary_none)
            else -> getString(R.string.alert_filters_summary_selected, checked.size, options.size)
        }
        val rows = ArrayList<AlertFilterRow>()
        rows.add(
            AlertFilterRow.Section(
                kind = kind,
                title = getString(if (isTypes) R.string.alert_filters_types_section else R.string.alert_filters_units_section),
                allLabel = getString(if (isTypes) R.string.alert_filters_all_types else R.string.alert_filters_all_units),
                summary = summary,
                allOn = all
            )
        )
        if (all) return rows

        rows.add(
            AlertFilterRow.Tools(
                kind = kind,
                showSearch = !isTypes,
                query = unitQuery,
                countLabel = getString(R.string.alert_filters_summary_selected, checked.size, options.size).uppercase()
            )
        )

        val visible = if (isTypes || unitQuery.isBlank()) options
        else {
            val q = unitQuery.trim()
            options.filter { (it.name ?: "").contains(q, ignoreCase = true) }
        }
        if (visible.isEmpty()) {
            rows.add(
                AlertFilterRow.Empty(
                    kind,
                    getString(if (options.isEmpty()) R.string.alert_filters_none_configured else R.string.alert_filters_no_match)
                )
            )
            return rows
        }
        val lastIndex = visible.size - 1
        visible.forEachIndexed { i, opt ->
            val id = opt._id!!
            rows.add(
                AlertFilterRow.Option(
                    kind = kind,
                    id = id,
                    name = opt.name ?: "",
                    checked = checked.contains(id),
                    last = i == lastIndex
                )
            )
        }
        return rows
    }

    // ---- adapter callbacks ------------------------------------------------

    override fun onAllToggled(kind: Kind, allOn: Boolean) {
        if (kind == Kind.TYPES) typesAll = allOn else unitsAll = allOn
        if (!allOn) {
            // switching ALL off with nothing remembered: start from everything
            // checked so the user removes what they don't want
            val checked = if (kind == Kind.TYPES) checkedTypeIds else checkedUnitIds
            val options = if (kind == Kind.TYPES) types else units
            if (checked.isEmpty()) checked.addAll(options.map { it._id!! })
        } else {
            hideKeyboard()
        }
        render()
    }

    override fun onSelectAll(kind: Kind, select: Boolean) {
        val checked = if (kind == Kind.TYPES) checkedTypeIds else checkedUnitIds
        val options = if (kind == Kind.TYPES) types else units
        // with a search active, SELECT ALL / NONE act on the visible matches only
        val scope = if (kind == Kind.UNITS && unitQuery.isNotBlank()) {
            val q = unitQuery.trim()
            options.filter { (it.name ?: "").contains(q, ignoreCase = true) }
        } else options
        val ids = scope.map { it._id!! }
        if (select) checked.addAll(ids) else checked.removeAll(ids.toSet())
        render()
    }

    override fun onOptionToggled(kind: Kind, id: String, checked: Boolean) {
        val set = if (kind == Kind.TYPES) checkedTypeIds else checkedUnitIds
        if (checked) set.add(id) else set.remove(id)
        render()
    }

    override fun onUnitQueryChanged(query: String) {
        if (query == unitQuery) return
        unitQuery = query
        render()
    }

    // ---- save -------------------------------------------------------------

    /** Compact form of what SAVE would send; equal strings = nothing to send. */
    private fun signature(): String {
        val t = if (typesAll) "ALL" else checkedTypeIds.sorted().joinToString(",")
        val u = if (unitsAll) "ALL" else checkedUnitIds.sorted().joinToString(",")
        return "$t|$u"
    }

    private fun saveChanges() {
        if (!loaded) return
        // an OFF section with nothing checked would mute the user; the server
        // would quietly treat it as ALL, so refuse it here instead
        if ((!typesAll && checkedTypeIds.isEmpty()) || (!unitsAll && checkedUnitIds.isEmpty())) {
            showSnack(getString(R.string.alert_filters_pick_one))
            return
        }
        if (signature() == initialSignature) {
            finish()
            return
        }
        hideKeyboard()
        val request = AlertFiltersUpdateRequest(
            typeIds = if (typesAll) null else checkedTypeIds.toList(),
            unitIds = if (unitsAll) null else checkedUnitIds.toList()
        )
        vm.saveAlertFilters(localityId, request)
    }
}
