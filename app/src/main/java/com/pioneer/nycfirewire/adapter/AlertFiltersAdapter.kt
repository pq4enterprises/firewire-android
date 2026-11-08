package com.pioneer.nycfirewire.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.ItemAlertFilterEmptyBinding
import com.pioneer.nycfirewire.databinding.ItemAlertFilterOptionBinding
import com.pioneer.nycfirewire.databinding.ItemAlertFilterSectionBinding
import com.pioneer.nycfirewire.databinding.ItemAlertFilterToolsBinding

/**
 * Alert picker list. One flat RecyclerView carries both sections so the units
 * list (hundreds to ~2,000 rows) recycles; a wrap_content list inside a scroll
 * view would inflate every row up front.
 *
 * Row order per section: Section (label + "All" switch) and, only while the
 * switch is OFF, Tools (search / select all / none) then Option rows, or an
 * Empty row when nothing matches. The activity owns the state and rebuilds the
 * row list on every change; stable ids keep the search box focused across
 * rebuilds.
 */
class AlertFiltersAdapter(
    private val listener: Listener
) : ListAdapter<AlertFilterRow, RecyclerView.ViewHolder>(DIFF) {

    enum class Kind { TYPES, UNITS }

    interface Listener {
        fun onAllToggled(kind: Kind, allOn: Boolean)
        fun onSelectAll(kind: Kind, select: Boolean)
        fun onOptionToggled(kind: Kind, id: String, checked: Boolean)
        fun onUnitQueryChanged(query: String)
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).stableId

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is AlertFilterRow.Section -> TYPE_SECTION
        is AlertFilterRow.Tools -> TYPE_TOOLS
        is AlertFilterRow.Option -> TYPE_OPTION
        is AlertFilterRow.Empty -> TYPE_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SECTION -> SectionHolder(ItemAlertFilterSectionBinding.inflate(inflater, parent, false))
            TYPE_TOOLS -> ToolsHolder(ItemAlertFilterToolsBinding.inflate(inflater, parent, false))
            TYPE_OPTION -> OptionHolder(ItemAlertFilterOptionBinding.inflate(inflater, parent, false))
            else -> EmptyHolder(ItemAlertFilterEmptyBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is AlertFilterRow.Section -> (holder as SectionHolder).bind(row)
            is AlertFilterRow.Tools -> (holder as ToolsHolder).bind(row)
            is AlertFilterRow.Option -> (holder as OptionHolder).bind(row)
            is AlertFilterRow.Empty -> (holder as EmptyHolder).bind(row)
        }
    }

    inner class SectionHolder(private val b: ItemAlertFilterSectionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(row: AlertFilterRow.Section) {
            b.tvSectionTitle.text = row.title
            b.tvAllLabel.text = row.allLabel
            b.tvSummary.text = row.summary
            // the card keeps its rounded bottom only while it stands alone
            b.llSectionCard.setBackgroundResource(
                if (row.allOn) R.drawable.fw_card_bg else R.drawable.fw_card_bg_top
            )
            b.swAll.setOnCheckedChangeListener(null)
            b.swAll.isChecked = row.allOn
            b.swAll.setOnCheckedChangeListener { _, checked -> listener.onAllToggled(row.kind, checked) }
        }
    }

    inner class ToolsHolder(private val b: ItemAlertFilterToolsBinding) : RecyclerView.ViewHolder(b.root) {
        private var kind: Kind = Kind.TYPES
        private var suppressWatcher = false

        init {
            b.etSearch.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (!suppressWatcher) listener.onUnitQueryChanged(s?.toString() ?: "")
                }
            })
            b.tvSelectAll.setOnClickListener { listener.onSelectAll(kind, true) }
            b.tvSelectNone.setOnClickListener { listener.onSelectAll(kind, false) }
        }

        fun bind(row: AlertFilterRow.Tools) {
            kind = row.kind
            b.tvCount.text = row.countLabel
            b.etSearch.visibility = if (row.showSearch) View.VISIBLE else View.GONE
            // only push the query into the field when it actually differs, so
            // a rebind after a keystroke never moves the cursor
            if (row.showSearch && b.etSearch.text.toString() != row.query) {
                suppressWatcher = true
                b.etSearch.setText(row.query)
                b.etSearch.setSelection(row.query.length)
                suppressWatcher = false
            }
        }
    }

    inner class OptionHolder(private val b: ItemAlertFilterOptionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(row: AlertFilterRow.Option) {
            b.tvOptionName.text = row.name
            b.cbOption.isChecked = row.checked
            b.divider.visibility = if (row.last) View.GONE else View.VISIBLE
            b.llOption.setBackgroundResource(
                if (row.last) R.drawable.fw_card_bg_bottom else R.drawable.fw_card_bg_mid
            )
            b.llOption.setOnClickListener {
                listener.onOptionToggled(row.kind, row.id, !b.cbOption.isChecked)
            }
        }
    }

    inner class EmptyHolder(private val b: ItemAlertFilterEmptyBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(row: AlertFilterRow.Empty) {
            b.tvEmpty.text = row.text
        }
    }

    companion object {
        private const val TYPE_SECTION = 1
        private const val TYPE_TOOLS = 2
        private const val TYPE_OPTION = 3
        private const val TYPE_EMPTY = 4

        private val DIFF = object : DiffUtil.ItemCallback<AlertFilterRow>() {
            override fun areItemsTheSame(a: AlertFilterRow, b: AlertFilterRow) = a.stableId == b.stableId
            override fun areContentsTheSame(a: AlertFilterRow, b: AlertFilterRow) = a == b
        }
    }
}

sealed class AlertFilterRow {
    abstract val stableId: Long

    data class Section(
        val kind: AlertFiltersAdapter.Kind,
        val title: String,
        val allLabel: String,
        val summary: String,
        val allOn: Boolean
    ) : AlertFilterRow() {
        override val stableId: Long = -(10L + kind.ordinal)
    }

    data class Tools(
        val kind: AlertFiltersAdapter.Kind,
        val showSearch: Boolean,
        val query: String,
        val countLabel: String
    ) : AlertFilterRow() {
        override val stableId: Long = -(20L + kind.ordinal)
    }

    data class Option(
        val kind: AlertFiltersAdapter.Kind,
        val id: String,
        val name: String,
        val checked: Boolean,
        val last: Boolean
    ) : AlertFilterRow() {
        // 64-bit id from two independent 32-bit hashes of the Mongo ObjectId;
        // the kind prefix keeps a type and a unit apart even if they ever collided
        override val stableId: Long = run {
            val key = "${kind.ordinal}:$id"
            val hi = key.hashCode().toLong()
            val lo = key.reversed().hashCode().toLong() and 0xffffffffL
            (hi shl 32) or lo
        }
    }

    data class Empty(
        val kind: AlertFiltersAdapter.Kind,
        val text: String
    ) : AlertFilterRow() {
        override val stableId: Long = -(30L + kind.ordinal)
    }
}
