package com.pioneer.nycfirewire.activity

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pioneer.nycfirewire.databinding.ItemSubNotificationCbBinding
import com.pioneer.nycfirewire.model.locality.IncidentType
import com.pioneer.nycfirewire.model.locality.SubLocality
import com.pioneer.nycfirewire.model.locality.FireUnit

class SubNotificationAdapter(
    private val onCheckedChange: (Int) -> Unit
) : ListAdapter<SubLocality, SubNotificationAdapter.SubViewHolder>(DiffCallback) {

    inner class SubViewHolder(private val binding: ItemSubNotificationCbBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SubLocality, position: Int) {
            binding.tvSubName.text = item.name
            binding.tvTitle.isChecked = item.isChecked
            binding.tvTitle.setOnClickListener { onCheckedChange(position) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubViewHolder {
        val binding = ItemSubNotificationCbBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SubViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<SubLocality>() {
        override fun areItemsTheSame(oldItem: SubLocality, newItem: SubLocality): Boolean =
            oldItem._id == newItem._id

        override fun areContentsTheSame(oldItem: SubLocality, newItem: SubLocality): Boolean =
            oldItem == newItem
    }
}

class UnitNotificationAdapter(
    private val onCheckedChange: (Int) -> Unit
) : ListAdapter<FireUnit, UnitNotificationAdapter.UnitViewHolder>(DiffCallback) {

    inner class UnitViewHolder(private val binding: ItemSubNotificationCbBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FireUnit, position: Int) {
            binding.tvSubName.text = item.unitName
            binding.tvTitle.isChecked = item.isChecked
            binding.tvTitle.setOnClickListener { onCheckedChange(position) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnitViewHolder {
        val binding = ItemSubNotificationCbBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UnitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UnitViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<FireUnit>() {
        override fun areItemsTheSame(oldItem: FireUnit, newItem: FireUnit): Boolean =
            oldItem._id == newItem._id

        override fun areContentsTheSame(oldItem: FireUnit, newItem: FireUnit): Boolean =
            oldItem == newItem
    }
}

class IncidentNotificationAdapter(
    private val onCheckedChange: (Int) -> Unit
) : ListAdapter<IncidentType, IncidentNotificationAdapter.IncidentViewHolder>(DiffCallback) {

    inner class IncidentViewHolder(private val binding: ItemSubNotificationCbBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: IncidentType, position: Int) {
            binding.tvSubName.text = item.optionName
            binding.tvTitle.isChecked = item.isChecked
            binding.tvTitle.setOnClickListener { onCheckedChange(position) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncidentViewHolder {
        val binding = ItemSubNotificationCbBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IncidentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IncidentViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<IncidentType>() {
        override fun areItemsTheSame(oldItem: IncidentType, newItem: IncidentType): Boolean =
            oldItem._id == newItem._id

        override fun areContentsTheSame(oldItem: IncidentType, newItem: IncidentType): Boolean =
            oldItem == newItem
    }
}
