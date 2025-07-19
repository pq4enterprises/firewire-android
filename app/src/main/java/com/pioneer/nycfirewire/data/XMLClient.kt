package com.pioneer.nycfirewire.data

import android.content.Context
import android.content.Intent
import com.pioneer.nycfirewire.activity.LoginNewActivity
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.utils.IntentUtils.REFRESH_TOKEN
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
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

    private val defaultHttpClient = OkHttpClient.Builder()
        .readTimeout(2, TimeUnit.MINUTES)
        .addInterceptor(logging)
        .addInterceptor(Interceptor { chain ->
            val original: Request = chain.request()
            val token = prefs.token
            val request: Request = if (token == null || token.isEmpty()) {
                original.newBuilder()
                    .method(original.method, original.body)
                    .build()
            } else {
                println("token:"+token)

                original.newBuilder()
                    .addHeader("Authorization", "Bearer " + token)
                    .method(original.method, original.body)
                    .build()

            }
            val response: Response = chain.proceed(request)


            if (response.code == 401 && token != null) {
                prefs.deleteToken
                val intent = Intent(context, LoginNewActivity::class.java)
                intent.putExtra(REFRESH_TOKEN, prefs.refreshToken)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                response
            }else response


        }).build()



    val newsEndPoints: NewsEndPoint = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(SimpleXmlConverterFactory.create())
        .client(defaultHttpClient)
        .build()
        .create(NewsEndPoint::class.java)
}
