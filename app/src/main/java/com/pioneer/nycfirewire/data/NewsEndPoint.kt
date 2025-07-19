package com.pioneer.nycfirewire.data

import com.pioneer.nycfirewire.model.TrendingSearchResponseWrapper
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers

interface NewsEndPoint {

    @Headers("Accept: application/xml")
    @GET("https://nycfirewire.net/feed")
    suspend fun getNewsDetail(): Response<TrendingSearchResponseWrapper>
}