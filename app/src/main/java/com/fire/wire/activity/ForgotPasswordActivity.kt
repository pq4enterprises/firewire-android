package com.fire.wire.activity

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.fire.wire.R
import com.fire.wire.databinding.ActivityForgotPasswordBinding
import com.fire.wire.model.user.request.ForgotPasswordRequest
import com.fire.wire.model.user.request.ResetPasswordRequest
import com.fire.wire.model.user.request.VerifyOtpRequest
import com.fire.wire.model.user.response.CommonResponse
import com.fire.wire.model.user.response.VerifyOtpResponse
import com.fire.wire.resource.Resource
import com.fire.wire.resource.ResourceState
import com.fire.wire.utils.*
import com.fire.wire.viewModel.UserViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ForgotPasswordActivity :BaseActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var vm: UserViewModel
    private var resetToken=""
    private var isPasswordVisible: Boolean = false
    private var isConfirmPasswordVisible: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this).get(UserViewModel::class.java)

        initApiCall()
        initUi()
        clickEvent()
    }

    private fun clickEvent() {

        binding.btnSendOtp.setOnClickListener {
            val email = binding.etEmailAddress.text.toString()
            when {
                email.isEmpty() -> showSnack(getString(R.string.enter_email))
                !isValidEmail(email) -> showSnack(getString(R.string.enter_valid_email))
                else -> vm.forgotPassword(ForgotPasswordRequest(email))
            }
        }

        binding.btnVerifyOtp.setOnClickListener {
            val otp= binding.etOtp.text.toString()
            val email = binding.etEmailAddress.text.toString()
            when {
                otp.isEmpty() -> showSnack(getString(R.string.enter_otp))
                else -> vm.verifyOtp(VerifyOtpRequest(email,otp))
            }
        }

        binding.btnResetPassword.setOnClickListener {
            val password= binding.etPassword.text.toString()
            val confirmPass= binding.etConfirmPassword.text.toString()
            when {
                password.isEmpty() -> showSnack(getString(R.string.enter_password))
                confirmPass.isEmpty() -> showSnack(getString(R.string.enter_confirm_password))
                (password != confirmPass)-> showSnack(getString(R.string.password_confirm_same))
                else -> vm.resetPassword(ResetPasswordRequest(resetToken,password))
            }
        }

        binding.toolbar.tvBack.setOnClickListener {
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
          binding.toolbar.tvTitle.text= getString(R.string.reset_password)

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

    private fun updateResetPassword(response: Resource<CommonResponse>?) {
        when(response?.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                response.data?.let { it1 ->
                    if (it1.code== Constants.CODE_SUCCESS){
                        showToast(this,it1.message.toString())
                       // Snackbar.make(window.decorView.rootView,it1.message.toString(), Snackbar.LENGTH_LONG).show()
                        startNewActivity(LoginNewActivity::class.java)
                    }
                }
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showAlert(response.message)
            }
            else -> {}
        }

    }

    private fun updateVerifyOtp(response: Resource<VerifyOtpResponse>?) {

        when(response?.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                response.data?.let { it1 ->
                    if (it1.code== Constants.CODE_SUCCESS){
                        resetPassword()
                        resetToken= it1.data?.resetToken?:""
                    }
                }
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showAlert(response.message)
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
                        showSnack(getString(R.string.message_sent_email))
                        binding.etEmailAddress.isEnabled=false
                        binding.etEmailAddress.isClickable=false
                        binding.etEmailAddress.isFocusable=false
                        otpVerify()
                    }
                }
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showAlert(response.message)
            }
            else -> {}
        }
    }

    private fun otpVerify(){
        binding.tvOtp.visible()
        binding.etOtp.visible()
        binding.btnVerifyOtp.visible()
        binding.btnSendOtp.gone()
    }

    private fun resetPassword(){
        binding.tvOtp.gone()
        binding.etOtp.gone()
        binding.tvEmailName.gone()
        binding.etEmailAddress.gone()
        binding.tvPassword.visible()
        binding.tvConfirmPassword.visible()
        binding.clPassword.visible()
        binding.clConfirmPassword.visible()
        binding.btnVerifyOtp.gone()
        binding.btnSendOtp.gone()
        binding.btnResetPassword.visible()
    }
}