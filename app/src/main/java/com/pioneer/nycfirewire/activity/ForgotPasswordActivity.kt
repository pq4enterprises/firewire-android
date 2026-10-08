package com.pioneer.nycfirewire.activity

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.pioneer.nycfirewire.model.user.request.ForgotPasswordRequest
import com.pioneer.nycfirewire.model.user.request.ResetPasswordRequest
import com.pioneer.nycfirewire.model.user.request.VerifyOtpRequest
import com.pioneer.nycfirewire.model.user.response.CommonResponse
import com.pioneer.nycfirewire.model.user.response.VerifyOtpResponse
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.viewModel.UserViewModel
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.ActivityForgotPasswordBinding
import com.pioneer.nycfirewire.utils.AuthUi
import com.pioneer.nycfirewire.utils.Constants
import com.pioneer.nycfirewire.utils.Constants.EMAIL_ID
import com.pioneer.nycfirewire.utils.Constants.FORGOT_PASSWORD
import com.pioneer.nycfirewire.utils.Constants.FROM_PAGE
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.visible
import dagger.hilt.android.AndroidEntryPoint


/**
 * Forgot password (2026 auth redesign): three stages on one screen, each
 * labelled STEP n OF 3 like the three iOS screens. Presentation only —
 * forgot / otp-verify / reset-password calls are unchanged.
 */
@AndroidEntryPoint
class ForgotPasswordActivity : BaseActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var vm: UserViewModel
    private var resetToken=""
    private var isPasswordVisible: Boolean = false
    private var isConfirmPasswordVisible: Boolean = false
    private var countdown: AuthUi.ResendCountdown? = null
    private var isVerifying = false

    private val verifyOtpLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // VerifyEmailOtpActivity confirmed the address; the reset can now go ahead.
            AuthUi.showBanner(binding.tvFormError, getString(R.string.fw_auth_email_verified_reset), AuthUi.Banner.SUCCESS)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this).get(UserViewModel::class.java)

        initApiCall()
        initUi()
        clickEvent()
        AuthUi.applyEdgeToEdge(this, binding.root, topBar = binding.toolbar.root, bottomPad = binding.nsView, darkTop = false)
    }

    override fun onResume() {
        super.onResume()
        analyticMethod(FORGOT_PASSWORD,"ForgotPasswordActivity")
    }

    override fun onDestroy() {
        countdown?.cancel()
        super.onDestroy()
    }

    private fun sendOtp(){
        hideKeyboard()
        AuthUi.hideBanner(binding.tvFormError)
        val email = binding.etEmailAddress.text.toString().trim()
        val error = AuthUi.emailError(this, email)
        AuthUi.setFieldError(binding.etEmailAddress, binding.tvEmailError, error)
        if (error != null) return
        vm.forgotPassword(ForgotPasswordRequest(email))
    }

    private fun verifyOtp() {
        if (isVerifying) return
        val otp = binding.etOtp.text.toString().filter { it.isDigit() }
        val email = binding.etEmailAddress.text.toString().trim()
        if (otp.length < 6) {
            AuthUi.setFieldError(binding.etOtp, binding.tvOtpError, getString(R.string.fw_auth_error_code_incomplete))
            return
        }
        hideKeyboard()
        AuthUi.hideBanner(binding.tvFormError)
        isVerifying = true
        vm.verifyOtp(VerifyOtpRequest(email, otp))
    }

    private fun submitNewPassword() {
        hideKeyboard()
        AuthUi.hideBanner(binding.tvFormError)
        val password = binding.etPassword.text.toString()
        val confirmPass = binding.etConfirmPassword.text.toString()
        val passwordError = AuthUi.passwordError(this, password)
        val confirmError = when {
            confirmPass.isEmpty() -> getString(R.string.fw_auth_error_confirm_empty)
            password != confirmPass -> getString(R.string.fw_auth_error_confirm_mismatch)
            else -> null
        }
        AuthUi.setFieldError(binding.etPassword, binding.tvPasswordError, passwordError)
        AuthUi.setFieldError(binding.etConfirmPassword, binding.tvConfirmPasswordError, confirmError)
        if (passwordError != null || confirmError != null) return
        vm.resetPassword(ResetPasswordRequest(resetToken, password, confirmPass))
    }

    private fun clickEvent() {

        binding.btnSendOtp.setOnClickListener { sendOtp() }
        AuthUi.submitOnDone(binding.etEmailAddress) { sendOtp() }

        binding.tvResend.setOnClickListener {
            countdown?.showSending()
            binding.etOtp.setText("")
            vm.forgotPassword(ForgotPasswordRequest(binding.etEmailAddress.text.toString().trim()))
        }

        binding.btnVerifyOtp.setOnClickListener { verifyOtp() }
        AuthUi.autoSubmitCode(binding.etOtp) { verifyOtp() }

        binding.btnResetPassword.setOnClickListener { submitNewPassword() }
        AuthUi.submitOnDone(binding.etConfirmPassword) { submitNewPassword() }

        AuthUi.clearOnEdit(binding.etEmailAddress, binding.tvEmailError, binding.tvFormError)
        AuthUi.clearOnEdit(binding.etOtp, binding.tvOtpError, binding.tvFormError)
        AuthUi.clearOnEdit(binding.etPassword, binding.tvPasswordError, binding.tvFormError)
        AuthUi.clearOnEdit(binding.etConfirmPassword, binding.tvConfirmPasswordError, binding.tvFormError)

        binding.toolbar.ivMenu.setOnClickListener {
            finish()
        }

        binding.ivPasswordShow.setOnClickListener {
            if (isPasswordVisible) {
                // Hide the password
                binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance();
                binding.ivPasswordShow.setImageResource(R.drawable.ic_hide_password);
            } else {
                // Show the password
                binding.etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance();
                binding.ivPasswordShow.setImageResource(R.drawable.ic_show_password);
            }
            isPasswordVisible = !isPasswordVisible;
            // Move the cursor to the end of the text after the transformation change
            binding.etPassword.setSelection(binding.etPassword.text.length);
        }

        binding.ivConfirmPasswordShow.setOnClickListener {
            if (isConfirmPasswordVisible) {
                // Hide the password
                binding.etConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance();
                binding.ivConfirmPasswordShow.setImageResource(R.drawable.ic_hide_password);
            } else {
                // Show the password
                binding.etConfirmPassword.transformationMethod = HideReturnsTransformationMethod.getInstance();
                binding.ivConfirmPasswordShow.setImageResource(R.drawable.ic_show_password);
            }
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            // Move the cursor to the end of the text after the transformation change
            binding.etConfirmPassword.setSelection(binding.etConfirmPassword.text.length);
        }

    }

    private fun initUi() {
          binding.toolbar.tvToolbarTitle.text= getString(R.string.reset_password).uppercase()
          binding.toolbar.tvToolbarTitle.visible()
          binding.toolbar.ivFeed.gone()

        binding.etConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance();
        binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance();
    }

    private fun initApiCall() {
        vm.forgotPasswordLiveData.observe(this, Observer {
            updatePassword(it)
        })

        vm.otpVerifyLiveData.observe(this, Observer {
            updateVerifyOtp(it)
        })

        vm.resetPasswordLiveData.observe(this, Observer {
            updateResetPassword(it)
        })
    }

    private fun showFormError(message: String?) {
        AuthUi.showBanner(binding.tvFormError, AuthUi.friendly(this, message), AuthUi.Banner.ERROR)
    }

    private fun updateResetPassword(response: Resource<CommonResponse>?) {
        when(response?.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                response.data?.let { it1 ->
                    if (it1.code== Constants.CODE_SUCCESS){
                        // Back to sign-in with a success banner (no toast, no dialog).
                        val intent = Intent(this, LoginNewActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            putExtra(LoginNewActivity.EXTRA_SUCCESS_NOTICE, getString(R.string.fw_auth_password_updated))
                        }
                        startActivity(intent)
                        finish()
                    } else showFormError(it1.message)
                }
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showFormError(response.message)
            }
            else -> {}
        }

    }

    private fun updateVerifyOtp(response: Resource<VerifyOtpResponse>?) {

        when(response?.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                isVerifying = false
                response.data?.let { it1 ->
                    if (it1.code== Constants.CODE_SUCCESS){
                        resetPassword()
                        resetToken= it1.data?.resetToken?:""
                    }else if(it1.code== Constants.INVALID_OTP){
                        binding.etOtp.setText("")
                        showFormError(getString(R.string.fw_auth_error_code_invalid))
                        binding.etOtp.requestFocus()
                    }else{
                        showFormError(it1.message.toString())
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

    private fun updatePassword(response: Resource<CommonResponse>?) {
        when(response?.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                response.data?.let { it1 ->
                    if (it1.code== Constants.CODE_SUCCESS){
                        binding.etEmailAddress.isEnabled=false
                        binding.etEmailAddress.isClickable=false
                        binding.etEmailAddress.isFocusable=false
                        val alreadyOnCodeStep = binding.etOtp.visibility == android.view.View.VISIBLE
                        otpVerify()
                        if (alreadyOnCodeStep) {
                            AuthUi.showBanner(binding.tvFormError, getString(R.string.fw_auth_new_code_sent), AuthUi.Banner.SUCCESS)
                        }
                    }else if(it1.code== Constants.CODE_EMAIL_NOT_VERIFIED){
                        countdown?.start()
                        moveToOtpScreen(binding.etEmailAddress.text.toString().trim())
                    }else{
                        countdown?.start()
                        showFormError(it1.message.toString())
                    }
                }
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                countdown?.start()
                showFormError(response.message)
            }
            else -> {}
        }
    }


    private fun moveToOtpScreen(email: String) {
        val intent = Intent(this, VerifyEmailOtpActivity::class.java).apply {
            putExtra(EMAIL_ID, email)
            putExtra(FROM_PAGE, FORGOT_PASSWORD)
        }
        verifyOtpLauncher.launch(intent)
    }

    /** Stage 2: the code was emailed. */
    private fun otpVerify(){
        binding.tvStep.text = getString(R.string.fw_auth_step_2)
        binding.tvStepTitle.text = getString(R.string.fw_auth_step_code)
        binding.tvIntro.text = getString(R.string.fw_auth_code_sent_to, maskEmail(binding.etEmailAddress.text.toString().trim()))
        binding.tvOtp.visible()
        binding.etOtp.visible()
        binding.llResend.visible()
        binding.btnVerifyOtp.visible()
        binding.btnSendOtp.gone()
        binding.tvHint.gone()
        if (countdown == null) countdown = AuthUi.ResendCountdown(binding.tvTimer, binding.tvResend)
        countdown?.start()
        binding.etOtp.requestFocus()
    }

    /** Stage 3: the code was accepted. */
    private fun resetPassword(){
        countdown?.cancel()
        binding.tvStep.text = getString(R.string.fw_auth_step_3)
        binding.tvStepTitle.text = getString(R.string.fw_auth_step_password)
        binding.tvIntro.gone()
        binding.tvOtp.gone()
        binding.etOtp.gone()
        binding.tvOtpError.gone()
        binding.llResend.gone()
        binding.tvEmailName.gone()
        binding.etEmailAddress.gone()
        binding.tvEmailError.gone()
        binding.tvPassword.visible()
        binding.tvPasswordRule.visible()
        binding.tvConfirmPassword.visible()
        binding.clPassword.visible()
        binding.clConfirmPassword.visible()
        binding.btnVerifyOtp.gone()
        binding.btnSendOtp.gone()
        binding.btnResetPassword.visible()
        binding.etPassword.requestFocus()
    }

    /** ph***l@example.com — same masking as iOS String.maskEmail. */
    private fun maskEmail(email: String): String {
        val at = email.indexOf('@')
        if (at <= 0) return email
        val local = email.substring(0, at)
        val masked = local.mapIndexed { i, c -> if (i == 0 || i == 1 || i == local.length - 1) c else '*' }.joinToString("")
        return masked + email.substring(at)
    }
}
