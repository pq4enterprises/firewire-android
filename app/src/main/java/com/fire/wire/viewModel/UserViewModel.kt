package com.fire.wire.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.fire.wire.model.incident.request.AddCommentRequest
import com.fire.wire.model.user.request.*
import com.fire.wire.model.user.response.*
import com.fire.wire.repository.UserRepository
import com.fire.wire.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(val context: Application) : AndroidViewModel(context) {

    @Inject
    lateinit var userRepository: UserRepository

     val registerLiveData = MutableLiveData<Resource<RegisterResponse>>()
     val loginLiveData = MutableLiveData<Resource<LoginResponse>>()
     val refreshLiveData = MutableLiveData<Resource<RefreshTokenResponse>>()
    val forgotPasswordLiveData = MutableLiveData<Resource<CommonResponse>>()
    val resetPasswordLiveData = MutableLiveData<Resource<CommonResponse>>()
    val otpVerifyLiveData = MutableLiveData<Resource<VerifyOtpResponse>>()
    val socialLoginLiveData = MutableLiveData<Resource<LoginResponse>>()


    fun registerUser(registerRequest: RegisterRequest){
         viewModelScope.launch {
             userRepository.registerUser(registerLiveData,registerRequest)
         }
     }

    fun loginUser(loginRequest: LoginRequest){
        viewModelScope.launch {
            userRepository.loginUser(loginLiveData,loginRequest)
        }
    }
    fun refreshToken(refreshRequest: RefreshTokenRequest){
        viewModelScope.launch {
            userRepository.refreshToken(refreshLiveData,refreshRequest)
        }
    }

    fun forgotPassword(request: ForgotPasswordRequest){
        viewModelScope.launch {
            userRepository.forgotPassword(forgotPasswordLiveData,request)
        }
    }

    fun verifyOtp(request: VerifyOtpRequest){
        viewModelScope.launch {
            userRepository.verifyOtp(otpVerifyLiveData,request)
        }
    }

    fun resetPassword(request: ResetPasswordRequest){
        viewModelScope.launch {
            userRepository.resetPassword(resetPasswordLiveData,request)
        }
    }

    fun postSocialLogin(request: SocialLoginRequest){
        viewModelScope.launch {
            userRepository.postSocialLogin(socialLoginLiveData,request)
        }
    }



}