package com.pioneer.nycfirewire.data

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.util.concurrent.TimeUnit
import com.onesignal.BuildConfig

class XMLClient (val context: Context) {

    companion object {
        const val BASE_URL = "https://firewire-api.atomgroups.com"
        var isXml=false
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG)
            HttpLoggingInterceptor.Level.BODY
        else
            HttpLoggingInterceptor.Level.NONE
    }

    /**
     * This client talks to exactly one endpoint: the public WordPress RSS feed at
     * https://nycfirewire.net/feed (see NewsEndPoint). It is not our API.
     *
     * It used to attach the user's FireWire access token as an Authorization header to
     * that request — sending a live credential to a host that has no use for it — and
     * then treat any 401 coming back from WordPress as a FireWire session expiry,
     * wiping the token and throwing the user onto the login screen mid-session. Both
     * behaviours are gone: no credential leaves the app here, and session renewal is
     * owned solely by TokenAuthenticator on the API client.
     */
    private val defaultHttpClient = OkHttpClient.Builder()
        .readTimeout(2, TimeUnit.MINUTES)
        .addInterceptor(logging)
        .build()



    val newsEndPoints: NewsEndPoint = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(SimpleXmlConverterFactory.create())
        .client(defaultHttpClient)
        .build()
        .create(NewsEndPoint::class.java)
}
