package com.fire.wire.repository

import androidx.lifecycle.MutableLiveData
import com.fire.wire.data.ApiEndPoints
import com.fire.wire.data.NewsEndPoint
import com.fire.wire.extensions.setError
import com.fire.wire.extensions.setLoading
import com.fire.wire.extensions.setSuccess
import com.fire.wire.model.ErrorResponse
import com.fire.wire.model.incident.request.AddCommentRequest
import com.fire.wire.model.user.request.*
import com.fire.wire.model.user.response.*
import com.fire.wire.resource.Resource

import com.google.gson.Gson
import retrofit2.Response
import javax.inject.Inject

class UserRepository @Inject constructor(private val apiEndPoint: ApiEndPoints, private val newsEndPoint: NewsEndPoint) {

    suspend fun registerUser(
        liveData: MutableLiveData<Resource<RegisterResponse>>,
        registerLoginRequest: RegisterRequest
    ){
        liveData.setLoading()
        try {
            val result =  apiEndPoint.registerUser(registerLoginRequest)
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
        loginRequest: LoginRequest
    ){
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
        refreshRequest: RefreshTokenRequest
    ){
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
        request: ForgotPasswordRequest
    ) {
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
        request: VerifyOtpRequest
    ) {
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
        request: ResetPasswordRequest
    ) {
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
        request: SocialLoginRequest
    ){
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




}



private fun errorHandling(result: Response<out Any>):String{
    val error = result.errorBody()?.source()?.readUtf8()
    val gson= Gson()
    val errorMsg= gson.fromJson(error,ErrorResponse::class.java)
    return if(!errorMsg.error.isNullOrEmpty()) errorMsg.error else errorMsg?.message.toString()
}



