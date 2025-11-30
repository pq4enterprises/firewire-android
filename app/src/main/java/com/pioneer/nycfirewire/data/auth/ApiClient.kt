package com.pioneer.nycfirewire.data.auth

import android.content.Context
import com.pioneer.nycfirewire.data.ApiEndPoints
import com.pioneer.nycfirewire.prefs
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(context: Context) {

    companion object {
       // const val BASE_URL = "https://api.nycfirewireapp.com"
      const val BASE_URL = "https://staging.api.nycfirewireapp.com"
      // const val BASE_URL = "https://dev-firewire-api.atomgroups.work"
       // const val BASE_INCIDENT_URL = "https://admin.nycfirewireapp.com"
        const val BASE_INCIDENT_URL = "https://staging.admin.nycfirewireapp.com"
    }

    // Logging interceptor
 /*   private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG)
            HttpLoggingInterceptor.Level.BODY
        else
            HttpLoggingInterceptor.Level.NONE
    }*/

    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Options: NONE, BASIC, HEADERS, BODY
    }

    // Access token injector
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = prefs.token
        val requestBuilder = original.newBuilder()

        println("token:"+token)

        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val request = requestBuilder.build()
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(2, TimeUnit.MINUTES)
        .addInterceptor(logging)
        .addInterceptor(authInterceptor)
        .authenticator(TokenAuthenticator(context, createSimpleApi()))
        .build()

    // Used only to refresh token inside authenticator
    private fun createSimpleApi(): ApiEndPoints {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiEndPoints::class.java)
    }

    // Main API service
    val apiEndPoint: ApiEndPoints = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()
        .create(ApiEndPoints::class.java)
}


