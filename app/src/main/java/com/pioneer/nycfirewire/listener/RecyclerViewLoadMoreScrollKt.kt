package com.pioneer.nycfirewire.listener

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class RecyclerViewLoadMoreScrollKt(private val layoutManager: RecyclerView.LayoutManager) : RecyclerView.OnScrollListener() {

    private var visibleThreshold = 5
    private var mOnLoadMoreListener: OnLoadMoreListener? = null
    private var isLoading = false
    private var lastVisibleItem = 0
    private var totalItemCount = 0

    fun setLoaded() {
        isLoading = false
    }

    fun getLoaded(): Boolean {
        return isLoading
    }

    fun setOnLoadMoreListener(onLoadMoreListener: OnLoadMoreListener) {
        this.mOnLoadMoreListener = onLoadMoreListener
    }

    init {
        when (layoutManager) {
            is GridLayoutManager -> visibleThreshold *= layoutManager.spanCount
            is StaggeredGridLayoutManager -> visibleThreshold *= layoutManager.spanCount
        }
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        if (dy <= 0) return

        totalItemCount = layoutManager.itemCount

        lastVisibleItem = when (layoutManager) {
            is StaggeredGridLayoutManager -> getLastVisibleItem(layoutManager.findLastVisibleItemPositions(null))
            is GridLayoutManager -> layoutManager.findLastVisibleItemPosition()
            is LinearLayoutManager -> layoutManager.findLastVisibleItemPosition()
            else -> 0
        }

        if (!isLoading && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
            mOnLoadMoreListener?.onLoadMore()
            isLoading = true
        }
    }

    private fun getLastVisibleItem(lastVisibleItemPositions: IntArray): Int {
        return lastVisibleItemPositions.maxOrNull() ?: 0
    }
}

