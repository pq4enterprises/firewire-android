package com.pioneer.nycfirewire.data.auth

import android.content.Context
import com.pioneer.nycfirewire.BuildConfig
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
        // ===================================================================
        // ⚠️  THIS IS CURRENTLY POINTED AT **STAGING**.
        //
        // It has been since 2025-11-30, on a branch carrying release version
        // codes — so a release built straight from source would silently ship
        // against staging. Debug builds on staging are fine and expected.
        //
        // Before ANY release build, uncomment the production lines below and
        // comment out the staging ones. The `checkReleaseApiUrl` Gradle task in
        // app/build.gradle.kts enforces this: assembleRelease / bundleRelease
        // FAIL while a staging or dev URL is active. Do not weaken that task.
        // ===================================================================

        // const val BASE_URL = "https://api.nycfirewireapp.com"          // PRODUCTION
        const val BASE_URL = "https://staging.api.nycfirewireapp.com"      // staging
        // const val BASE_URL = "https://dev-firewire-api.atomgroups.work" // dead host

        // const val BASE_INCIDENT_URL = "https://admin.nycfirewireapp.com"        // PRODUCTION
        const val BASE_INCIDENT_URL = "https://staging.admin.nycfirewireapp.com"   // staging
    }

    /**
     * Level.BODY logs full headers and bodies. On this client that means every access
     * token, every refresh token, and the login request body — password included —
     * written to logcat. It was running unconditionally, so release builds did it too:
     * a live token could be lifted straight out of `adb logcat`, or out of any
     * `adb bugreport` the user was ever asked to send in.
     *
     * Restored to the DEBUG gate that was sitting commented out directly above it.
     * XMLClient already gated its logging this way; this client was the outlier.
     */
    val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG)
            HttpLoggingInterceptor.Level.BODY
        else
            HttpLoggingInterceptor.Level.NONE
    }

    // Access token injector
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = prefs.token
        val requestBuilder = original.newBuilder()

        // (removed) println("token:" + token) — printed the bearer token to logcat on
        // every single request, in release builds as well as debug.

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


