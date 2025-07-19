package com.pioneer.nycfirewire.fragment

import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingState
import com.pioneer.nycfirewire.data.ApiEndPoints
import com.pioneer.nycfirewire.model.incident.response.Incident
import retrofit2.HttpException
import javax.inject.Inject

class PostPagingSource @Inject constructor(
    private val api: ApiEndPoints,
    private val listener: PagingMetadataListener
)  : PagingSource<Int, Incident>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Incident> {
        val currentPage = params.key ?: 1
        val limit = 100 // Fixed limit

        return try {
            val response = api.getIncidentList(currentPage, limit)
            if (response.isSuccessful) {
                val list = response.body()?.data?.data ?: emptyList()
                var incidentData = response.body()?.data

                // Send total count
                incidentData?.pageInfo?.let { listener.onTotalCountUpdated(it.totalCount.toString()) }

                val nextPage = if (list.isEmpty()) null else currentPage + 1
                val prevPage = if (currentPage == 1) null else currentPage - 1

                LoadResult.Page(
                    data = list,
                    prevKey = prevPage,
                    nextKey = nextPage
                )
            } else {
                LoadResult.Error(HttpException(response))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }



    override fun getRefreshKey(state: PagingState<Int, Incident>): Int? {
        return state.anchorPosition?.let { position ->
            state.closestPageToPosition(position)?.nextKey?.minus(1)
                ?: state.closestPageToPosition(position)?.prevKey?.plus(1)
        }
    }
}


interface PagingMetadataListener {
    fun onTotalCountUpdated(total: String)
}