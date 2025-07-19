package com.pioneer.nycfirewire.data

import android.content.Context
import android.content.Intent
import com.onesignal.BuildConfig
import com.pioneer.nycfirewire.activity.LoginNewActivity
import com.pioneer.nycfirewire.model.user.request.RefreshTokenRequest
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.utils.IntentUtils.REFRESH_TOKEN
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit




class APIClient (val context: Context) {

    companion object {
        //const val BASE_URL = "https://firewire-api.atomgroups.work"
        //const val BASE_URL = "https://api.nycfirewireapp.com"
        const val BASE_URL = "https://dev-firewire-api.atomgroups.work"
        //const val BASE_INCIDENT_URL = "https://firewire.atomgroups.work"
        const val BASE_INCIDENT_URL = "https://admin.nycfirewireapp.com"

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
            println("request:"+request)

            if (response.code == 401 && token != null) {
                val authResponse= apiEndPoint.tokenRefresh(RefreshTokenRequest(prefs.refreshToken.toString())).execute()
                if(authResponse.isSuccessful == true){
                    val auth= authResponse.body()
                    auth?.data.let {
                        prefs.refreshToken= it?.refreshToken
                        prefs.token = it?.token
                        val authHead = "Bearer ${it?.token}"
                        val retryRequest = chain.request()
                            .newBuilder()
                            .addHeader("Authorization", authHead)
                            .build()
                        response.body.close()
                        val retryResponse = chain.proceed(retryRequest)
                        retryResponse
                    }
                }else {
                    prefs.deleteToken
                    val intent = Intent(context, LoginNewActivity::class.java)
                    //intent.putExtra(REFRESH_TOKEN, prefs.refreshToken)
                    println("refreshToken:" + prefs.refreshToken)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    response
                }
            }else{
                if(token==null){
                    prefs.deleteToken
                    val intent = Intent(context, LoginNewActivity::class.java)
                    //intent.putExtra(REFRESH_TOKEN, prefs.refreshToken)
                    println("refreshToken:" + prefs.refreshToken)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                }

                response
            }


        }).build()



    val apiEndPoint: ApiEndPoints = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .client(defaultHttpClient)
        .build()
        .create(ApiEndPoints::class.java)
}


