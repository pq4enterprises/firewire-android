package com.pioneer.nycfirewire.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pioneer.nycfirewire.repository.UserRepository
import com.pioneer.nycfirewire.resource.Resource
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

    val emailOtpVerifyLiveData = MutableLiveData<Resource<LoginResponse>>()
    val resendOtpLiveData = MutableLiveData<Resource<LoginResponse>>()


    fun registerUser(registerRequest: RegisterRequest){
         viewModelScope.launch {
             userRepository.registerUser(registerLiveData,registerRequest,context)
         }
     }

    fun loginUser(loginRequest: LoginRequest){
        viewModelScope.launch {
            userRepository.loginUser(loginLiveData,loginRequest,context)
        }
    }
    fun refreshToken(refreshRequest: RefreshTokenRequest){
        viewModelScope.launch {
            userRepository.refreshToken(refreshLiveData,refreshRequest,context)
        }
    }

    fun forgotPassword(request: ForgotPasswordRequest){
        viewModelScope.launch {
            userRepository.forgotPassword(forgotPasswordLiveData,request,context)
        }
    }

    fun verifyOtp(request: VerifyOtpRequest){
        viewModelScope.launch {
            userRepository.verifyOtp(otpVerifyLiveData,request,context)
        }
    }

    fun resetPassword(request: ResetPasswordRequest){
        viewModelScope.launch {
            userRepository.resetPassword(resetPasswordLiveData,request,context)
        }
    }

    fun postSocialLogin(request: SocialLoginRequest){
        viewModelScope.launch {
            userRepository.postSocialLogin(socialLoginLiveData,request,context)
        }
    }


    fun verifyOtpEmail(request: VerifyEmailOtpRequest){
        viewModelScope.launch {
            userRepository.verifyEmailOtp(emailOtpVerifyLiveData,request,context)
        }
    }

    fun resendOtpEmail(request: ResendOtpRequest){
        viewModelScope.launch {
            userRepository.resendEmailOtp(resendOtpLiveData,request,context)
        }
    }



}