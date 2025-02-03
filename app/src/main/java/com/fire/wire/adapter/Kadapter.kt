package com.fire.wire.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Kadapter<in ITEM,ViewBinding>(
    private val items: MutableList<ITEM>,
    private val bindHolder: View.(ITEM, Int,ViewBinding) -> Unit,
    private val binding:(LayoutInflater,ViewGroup?,Boolean) -> ViewBinding,
    private val itemClick: ITEM.() -> Unit = {}
) : AbstractAdapterNew<ITEM,ViewBinding>(items, binding) {

    override fun onItemClick(itemView: View, position: Int) {
        items[position].itemClick()
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int) = position

    override fun getItemId(position: Int) = position.toLong()

    override fun View.bind(item: ITEM, position: Int, _bi: androidx.viewbinding.ViewBinding) {
      bindHolder(item,position,_bi as ViewBinding)
    }
}

fun <ITEM,ViewBinding> RecyclerView.setUpAdapter(
    items: MutableList<ITEM>,
    layoutResId: Int,
    binding: (LayoutInflater, ViewGroup?, Boolean) -> ViewBinding,
    bindHolder: View.(ITEM, Int,ViewBinding) -> Unit,
    itemClick: ITEM.() -> Unit = {},
    manager: RecyclerView.LayoutManager = LinearLayoutManager(this.context)
): Kadapter<ITEM,ViewBinding> {
    layoutManager = manager
    return Kadapter(items, bindHolder,binding, itemClick).apply { adapter = this }
}