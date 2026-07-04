package com.pioneer.nycfirewire.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.pioneer.nycfirewire.data.ApiEndPoints
import com.pioneer.nycfirewire.data.NewsEndPoint
import com.pioneer.nycfirewire.extensions.setError
import com.pioneer.nycfirewire.extensions.setLoading
import com.pioneer.nycfirewire.extensions.setSuccess
import com.pioneer.nycfirewire.model.ErrorResponse
import com.pioneer.nycfirewire.resource.Resource

import com.google.gson.Gson
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.model.user.request.ForgotPasswordRequest
import com.pioneer.nycfirewire.model.user.request.LoginRequest
import com.pioneer.nycfirewire.model.user.request.RefreshTokenRequest
import com.pioneer.nycfirewire.model.user.request.RegisterRequest
import com.pioneer.nycfirewire.model.user.request.ResendOtpRequest
import com.pioneer.nycfirewire.model.user.request.ResetPasswordRequest
import com.pioneer.nycfirewire.model.user.request.SocialLoginRequest
import com.pioneer.nycfirewire.model.user.request.VerifyEmailOtpRequest
import com.pioneer.nycfirewire.model.user.request.VerifyOtpRequest
import com.pioneer.nycfirewire.model.user.response.CommonResponse
import com.pioneer.nycfirewire.model.user.response.LoginResponse
import com.pioneer.nycfirewire.model.user.response.RefreshTokenResponse
import com.pioneer.nycfirewire.model.user.response.RegisterResponse
import com.pioneer.nycfirewire.model.user.response.VerifyOtpResponse
import com.pioneer.nycfirewire.utils.NetworkUtils
import retrofit2.Response
import javax.inject.Inject

class UserRepository @Inject constructor(private val apiEndPoint: ApiEndPoints, private val newsEndPoint: NewsEndPoint) {

    suspend fun registerUser(
        liveData: MutableLiveData<Resource<RegisterResponse>>,
        registerLoginRequest: RegisterRequest,
        context: Context
    ){
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }
        liveData.setLoading()
        try {
            val result =  apiEndPoint.registerUser(registerLoginRequest)
            Log.d("RegisterRes",result.toString())
           if(result.isSuccessful){
                liveData.setSuccess(result.body())
            }else{
               liveData.setError(errorHandling(result))
            }

        }catch (e:Exception){
            liveData.setError(e.message.toString())
        }
    }

    suspend fun loginUser(
        liveData: MutableLiveData<Resource<LoginResponse>>,
        loginRequest: LoginRequest,
        context: Context
    ){
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }
        liveData.setLoading()
        try {
            val result =  apiEndPoint.loginUser(loginRequest)
            if(result.isSuccessful){
                liveData.setSuccess(result.body())
            }else{
                liveData.setError(errorHandling(result))
            }

        }catch (e:Exception){
            liveData.setError(e.message.toString())
        }
    }

    suspend fun refreshToken(
        liveData: MutableLiveData<Resource<RefreshTokenResponse>>,
        refreshRequest: RefreshTokenRequest,
        context: Context
    ){
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }
        liveData.setLoading()
        try {
            val result =  apiEndPoint.refreshToken(refreshRequest)
            if(result.isSuccessful){
                liveData.setSuccess(result.body())
            }else{
                liveData.setError(errorHandling(result))
            }

        }catch (e:Exception){
            liveData.setError(e.message.toString())
        }
    }


    suspend fun forgotPassword(
        liveData: MutableLiveData<Resource<CommonResponse>>,
        request: ForgotPasswordRequest,
        context: Context
    ) {
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }
        liveData.setLoading()
        try {
            val result = apiEndPoint.forgotPassword(request)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun verifyOtp(
        liveData: MutableLiveData<Resource<VerifyOtpResponse>>,
        request: VerifyOtpRequest,
        context: Context
    ) {
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }
        liveData.setLoading()
        try {
            val result = apiEndPoint.verifyOtp(request)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun resetPassword(
        liveData: MutableLiveData<Resource<CommonResponse>>,
        request: ResetPasswordRequest,
        context: Context
    ) {
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }
        liveData.setLoading()
        try {
            val result = apiEndPoint.resetPassword(request)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun postSocialLogin(
        liveData: MutableLiveData<Resource<LoginResponse>>,
        request: SocialLoginRequest,
        context: Context
    ){
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }
        liveData.setLoading()
        try {
            val result = apiEndPoint.postGoogle(request)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }


    suspend fun verifyEmailOtp(
        liveData: MutableLiveData<Resource<LoginResponse>>,
        request: VerifyEmailOtpRequest,
        context: Context
    ) {
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }
        liveData.setLoading()
        try {
            val result = apiEndPoint.verifyEmailOtp(request)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun resendEmailOtp(
        liveData: MutableLiveData<Resource<LoginResponse>>,
        request: ResendOtpRequest,
        context: Context
    ) {
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }
        liveData.setLoading()
        try {
            val result = apiEndPoint.resendEmailOtp(request)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }





}



private fun errorHandling(result: Response<out Any>):String{
    val error = result.errorBody()?.source()?.readUtf8()
    val gson= Gson()
    val errorMsg= gson.fromJson(error, ErrorResponse::class.java)
    return if(!errorMsg.error.isNullOrEmpty()) errorMsg.error else errorMsg?.message.toString()
}



