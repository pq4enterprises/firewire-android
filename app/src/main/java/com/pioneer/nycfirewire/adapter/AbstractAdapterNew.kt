package com.pioneer.nycfirewire.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class AbstractAdapterNew<in ITEM,ViewBinding>(
    private var itemList:MutableList<ITEM>,
    private val binding: (LayoutInflater,ViewGroup?,Boolean) ->ViewBinding
) : RecyclerView.Adapter<AbstractAdapterNew.Holder>() {

    protected abstract fun onItemClick(itemView:View, position:Int)

    protected abstract fun View.bind(item:ITEM, position:Int, _bi:androidx.viewbinding.ViewBinding)

    override fun getItemCount() = itemList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val _bi= binding(LayoutInflater.from(parent.context),parent,false)
        val viewHolder= Holder(_bi as androidx.viewbinding.ViewBinding)
        val itemView = viewHolder.itemView

        itemView.setOnClickListener {
            val position= viewHolder.adapterPosition
            if(position!= RecyclerView.NO_POSITION){
                onItemClick(itemView, position)
            }
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = itemList[position]
        holder.itemView.bind(item,position,holder.binding)
    }

    fun update(items:List<ITEM>){
        DiffUtil.calculateDiff(DiffUtilCallback(itemList, items)).dispatchUpdatesTo(this)
    }

    fun add(item: ITEM){
        itemList.add(item)
        notifyItemInserted(itemList.size)
    }

    fun remove(position:Int){
        itemList.removeAt(position)
        notifyItemRemoved(position)
    }



    class Holder(itemView:ViewBinding): RecyclerView.ViewHolder(itemView.root){
        val binding = itemView
    }
}