package com.pioneer.nycfirewire.activity

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.onesignal.OneSignal
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.ActivityEmailVerifyOtpBinding
import com.pioneer.nycfirewire.model.user.request.ResendOtpRequest
import com.pioneer.nycfirewire.model.user.request.VerifyEmailOtpRequest
import com.pioneer.nycfirewire.model.user.response.LoginResponse
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.AuthUi
import com.pioneer.nycfirewire.utils.Constants
import com.pioneer.nycfirewire.utils.Constants.VERIFY_EMAIL
import com.pioneer.nycfirewire.utils.Constants.CODE_SUCCESS
import com.pioneer.nycfirewire.utils.Constants.EMAIL_ID
import com.pioneer.nycfirewire.utils.Constants.FORGOT_PASSWORD
import com.pioneer.nycfirewire.utils.Constants.FROM_PAGE
import com.pioneer.nycfirewire.utils.Constants.FROM_START
import com.pioneer.nycfirewire.utils.IntentUtils.UPDATE_PROFILE
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.visible
import com.pioneer.nycfirewire.viewModel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint


/**
 * Verify email (2026 auth redesign). Presentation only: inline code errors,
 * a banner for server/network errors, resend with countdown (RESEND stays
 * visible but disabled while counting), auto-submit at six digits. The
 * verify-email-otp / resend-email-otp calls and session storage are unchanged.
 */
@AndroidEntryPoint
class VerifyEmailOtpActivity : BaseActivity() {

    private lateinit var binding: ActivityEmailVerifyOtpBinding
    private lateinit var vm: UserViewModel
    var email=""
    private var countdown: AuthUi.ResendCountdown? = null
    private var fromPage="Register"
    private var isVerifying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityEmailVerifyOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this).get(UserViewModel::class.java)

        binding.toolbar.tvToolbarTitle.text= getString(R.string.verify_email).uppercase()
        binding.toolbar.tvToolbarTitle.visible()
        binding.toolbar.ivFeed.gone()
        binding.toolbar.ivMenu.setOnClickListener { finish() }
        countdown = AuthUi.ResendCountdown(binding.tvTimer, binding.tvResend)
        startOtpTimer()
        initExtra()
        initApiCall()
        clickEvent()

    }

    override fun onResume() {
        super.onResume()
        analyticMethod(VERIFY_EMAIL,"VerifyEmailOtpActivity")
    }

    private fun initExtra() {
        if(intent.hasExtra(FROM_PAGE)) fromPage= intent.getStringExtra(FROM_PAGE).toString()
        email= intent.getStringExtra(EMAIL_ID)?:""
        binding.etEmailAddress.setText(email)

        //if(fromPage==LOGIN || fromPage==UPDATE_PROFILE || fromPage==FORGOT_PASSWORD){
        if(fromPage==UPDATE_PROFILE ){
            resendOtp()
        }
        binding.etOtp.requestFocus()
    }

    private fun clickEvent(){
        binding.btnVerifyOtp.setOnClickListener {
            verifyOtp()
        }

        binding.tvResend.setOnClickListener {
            resendOtp()
        }

        AuthUi.autoSubmitCode(binding.etOtp) { verifyOtp() }
        AuthUi.clearOnEdit(binding.etOtp, binding.tvOtpError, binding.tvFormError)
    }

    private fun initApiCall() {
        vm.emailOtpVerifyLiveData.observe(this, Observer {
            updateVerifyOtp(it)
        })

        vm.resendOtpLiveData.observe(this, Observer{
            updateResendOtp(it)
        })
    }

    private fun showFormError(message: String?) {
        AuthUi.showBanner(binding.tvFormError, AuthUi.friendly(this, message), AuthUi.Banner.ERROR)
    }


    private fun updateResendOtp(response: Resource<LoginResponse>?) {

        when(response?.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                startOtpTimer()
                binding.progress.gone()
                response.data?.let { it1 ->
                    if (it1.code== Constants.CODE_SUCCESS){
                        AuthUi.showBanner(binding.tvFormError, getString(R.string.fw_auth_new_code_sent), AuthUi.Banner.SUCCESS)
                    }else if(it1.code== Constants.INVALID_OTP){
                        showFormError(it1.message.toString())
                    }else{
                        showFormError(it1.message.toString())
                    }
                }
            }
            ResourceState.ERROR -> {
                startOtpTimer()
                binding.progress.gone()
                showFormError(response.message)
            }
            else -> {}
        }

    }

    private fun updateVerifyOtp(response: Resource<LoginResponse>?) {

        when(response?.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                isVerifying = false
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
                        binding.etOtp.setText("")
                        showFormError(getString(R.string.fw_auth_error_code_invalid))
                        binding.etOtp.requestFocus()
                    }else if(it1.code== "already_registered_login"){
                        AuthUi.showBanner(binding.tvFormError, getString(R.string.fw_auth_already_verified), AuthUi.Banner.INFO)
                    }else{
                        showFormError(it1.message)
                    }
                }
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                isVerifying = false
                showFormError(response.message)
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
        if (isVerifying) return
        val otp= binding.etOtp.text.toString().filter { it.isDigit() }
        if (otp.length < 6) {
            AuthUi.setFieldError(binding.etOtp, binding.tvOtpError, getString(R.string.fw_auth_error_code_incomplete))
            return
        }
        hideKeyboard()
        AuthUi.hideBanner(binding.tvFormError)
        isVerifying = true
        vm.verifyOtpEmail(VerifyEmailOtpRequest(email,otp))
    }

    private fun resendOtp(){
        binding.etOtp.setText("")
        AuthUi.hideBanner(binding.tvFormError)
        countdown?.showSending()
        vm.resendOtpEmail(ResendOtpRequest(email))

    }


    private fun startOtpTimer() {
        countdown?.start()
    }

    override fun onDestroy() {
        countdown?.cancel()
        super.onDestroy()
    }

}
