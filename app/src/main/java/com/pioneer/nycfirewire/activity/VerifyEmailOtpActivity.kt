package com.pioneer.nycfirewire.activity

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.onesignal.OneSignal
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.ActivityEmailVerifyOtpBinding
import com.pioneer.nycfirewire.model.user.request.ResendOtpRequest
import com.pioneer.nycfirewire.model.user.request.VerifyEmailOtpRequest
import com.pioneer.nycfirewire.model.user.response.LoginResponse
import com.pioneer.nycfirewire.model.user.response.RegisterResponse
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.Constants
import com.pioneer.nycfirewire.utils.Constants.CODE_SUCCESS
import com.pioneer.nycfirewire.utils.Constants.EMAIL_ID
import com.pioneer.nycfirewire.utils.Constants.FORGOT_PASSWORD
import com.pioneer.nycfirewire.utils.Constants.FROM_PAGE
import com.pioneer.nycfirewire.utils.Constants.FROM_START
import com.pioneer.nycfirewire.utils.Constants.LOGIN
import com.pioneer.nycfirewire.utils.IntentUtils.UPDATE_PROFILE
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.visible
import com.pioneer.nycfirewire.viewModel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class VerifyEmailOtpActivity : BaseActivity() {

    private lateinit var binding: ActivityEmailVerifyOtpBinding
    private lateinit var vm: UserViewModel
    var email=""
    private var countDownTimer: CountDownTimer? = null
    private var fromPage="Register"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityEmailVerifyOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this).get(UserViewModel::class.java)

        binding.toolbar.tvTitle.text= getString(R.string.verify_email)
         startOtpTimer()
        initExtra()
        initApiCall()
        clickEvent()

    }

    private fun initExtra() {
        if(intent.hasExtra(FROM_PAGE)) fromPage= intent.getStringExtra(FROM_PAGE).toString()
        email= intent.getStringExtra(EMAIL_ID)?:""
        binding.etEmailAddress.setText(email)

        //if(fromPage==LOGIN || fromPage==UPDATE_PROFILE || fromPage==FORGOT_PASSWORD){
        if(fromPage==UPDATE_PROFILE ){
            resendOtp()
        }
    }

    private fun clickEvent(){
        binding.btnVerifyOtp.setOnClickListener {
            verifyOtp()
        }

        binding.tvResend.setOnClickListener {
            resendOtp()
        }
    }

    private fun initApiCall() {
        vm.emailOtpVerifyLiveData.observe(this, Observer {
            updateVerifyOtp(it)
        })

        vm.resendOtpLiveData.observe(this, Observer{
            updateResendOtp(it)
        })
    }



    private fun updateResendOtp(response: Resource<LoginResponse>?) {

        when(response?.state){
            ResourceState.LOADING -> {binding.progress.visible()
                binding.tvResend.gone()}
            ResourceState.SUCCESS -> {
                startOtpTimer()
                binding.progress.gone()
                binding.tvResend.visible()
                response.data?.let { it1 ->
                    if (it1.code== Constants.CODE_SUCCESS){
                        showSnack(it1.message)

                    }else if(it1.code== Constants.INVALID_OTP){
                        // binding.etOtp.setText("")
                        showSnack(it1.message.toString())
                    }
                }
            }
            ResourceState.ERROR -> {
                startOtpTimer()
                binding.progress.gone()
                binding.tvResend.visible()
                showAlert(response.message)
            }
            else -> {}
        }

    }

    private fun updateVerifyOtp(response: Resource<LoginResponse>?) {

        when(response?.state){
            ResourceState.LOADING -> {binding.progress.visible()
            binding.tvResend.gone()}
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                binding.tvResend.visible()
                response.data?.let { it1 ->
                    if (it1.code== Constants.CODE_SUCCESS){
                        if(fromPage==UPDATE_PROFILE || fromPage==FORGOT_PASSWORD){
                            setResult(RESULT_OK)
                            finish()
                        }else {
                            if (!it1.data.token.isNullOrEmpty()) {
                                prefs.token = it1.data.token
                                prefs.refreshToken = it1.data.refreshToken
                                prefs.isLogin = true
                                prefs.userId = it1.data._id
                                prefs.userRole = it1.data.role
                                prefs.showAppIntro = true
                                prefs.showHomeIntro = true
                                prefs.showFeedIntro = true
                                OneSignal.login(prefs.userId.toString())
                                OneSignal.User.addEmail(email)
                                println("onesignalId:" + OneSignal.User.onesignalId)
                                moveToLogin(it1)
                            }
                        }

                    }else if(it1.code== Constants.INVALID_OTP){
                        // binding.etOtp.setText("")
                        showSnack(it1.message.toString())
                    }
                }
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                binding.tvResend.visible()
                showAlert(response.message)
            }
            else -> {}
        }

    }

    private fun moveToLogin(response: LoginResponse) {
        if(response.code==CODE_SUCCESS){
            var intent = Intent(this, SelectAreaActivity::class.java)
            intent.putExtra(FROM_START,true)
            startActivity(intent)
            finish()
        }

    }


    private fun verifyOtp(){
        val otp= binding.etOtp.text.toString()
        when {
            otp.isEmpty() -> showSnack(getString(R.string.enter_otp))
            else -> vm.verifyOtpEmail(VerifyEmailOtpRequest(email,otp))
        }
    }

    private fun resendOtp(){
        binding.etOtp.setText("")
        binding.tvTimer.text = "Sending OTP..."
        vm.resendOtpEmail(ResendOtpRequest(email))

    }


    private fun startOtpTimer() {
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(30000, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                binding.tvTimer.text = String.format(
                    "Resend OTP in 00:%02d",
                    seconds
                )
            }

            override fun onFinish() {
                binding.tvTimer.text = "Didn't receive the OTP?"
                binding.tvResend.visible()
            }
        }.start()
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }

}