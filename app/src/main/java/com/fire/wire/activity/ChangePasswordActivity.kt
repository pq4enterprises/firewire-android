package com.fire.wire.activity


import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.fire.wire.R
import com.fire.wire.databinding.ActivityChangePasswordBinding
import com.fire.wire.model.user.request.UpdatePasswordRequest
import com.fire.wire.model.user.response.CommonResponse
import com.fire.wire.resource.Resource
import com.fire.wire.resource.ResourceState
import com.fire.wire.utils.Constants
import com.fire.wire.utils.gone
import com.fire.wire.utils.showToast
import com.fire.wire.utils.visible
import com.fire.wire.viewModel.FireWireViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ChangePasswordActivity:BaseActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var vm: FireWireViewModel
    private var isPasswordVisible: Boolean = false
    private var isNewPasswordVisible: Boolean = false
    private var isConfirmPasswordVisible: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)

        initApiCall()
        initUi()
        clickEvent()
    }

    private fun initApiCall() {
        vm.updatePasswordLiveData.observe(this, Observer {
            updateData(it)
        })
    }

    private fun updateData(response: Resource<CommonResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                if(response.data?.code== Constants.CODE_SUCCESS) {
                    showSnack(response.data.message.toString())
                }else{
                    showAlert(response.data?.message.toString())
                }
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showAlert(response.message)
            }
        }
    }

    private fun initUi() {
        binding.toolbar.tvTitle.text = getString(R.string.change_password_text)

        binding.etConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance();
        binding.etNewPassword.transformationMethod = PasswordTransformationMethod.getInstance();
        binding.etCurrentPassword.transformationMethod = PasswordTransformationMethod.getInstance();
    }

    private fun clickEvent() {
      binding.toolbar.tvBack.setOnClickListener {
          finish()
      }
        binding.btnChangePassword.setOnClickListener {
            changePassword()
        }

        binding.ivCurrentPasswordShow.setOnClickListener {
            if (isPasswordVisible) {
                // Hide the password
                binding.etCurrentPassword.transformationMethod = PasswordTransformationMethod.getInstance();
                binding.ivCurrentPasswordShow.setImageResource(R.drawable.ic_hide_password);
            } else {
                // Show the password
                binding.etCurrentPassword.transformationMethod = HideReturnsTransformationMethod.getInstance();
                binding.ivCurrentPasswordShow.setImageResource(R.drawable.ic_show_password);
            }
            isPasswordVisible = !isPasswordVisible;
            // Move the cursor to the end of the text after the transformation change
            binding.etCurrentPassword.setSelection(binding.etCurrentPassword.text.length);
        }

        binding.ivNewPasswordShow.setOnClickListener {
            if (isNewPasswordVisible) {
                // Hide the password
                binding.etNewPassword.transformationMethod = PasswordTransformationMethod.getInstance();
                binding.ivNewPasswordShow.setImageResource(R.drawable.ic_hide_password);
            } else {
                // Show the password
                binding.etNewPassword.transformationMethod = HideReturnsTransformationMethod.getInstance();
                binding.ivNewPasswordShow.setImageResource(R.drawable.ic_show_password);
            }
            isNewPasswordVisible = !isNewPasswordVisible;
            // Move the cursor to the end of the text after the transformation change
            binding.etNewPassword.setSelection(binding.etNewPassword.text.length);
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

    private fun changePassword() {
        val currentPassword= binding.etCurrentPassword.text.toString()
        val newPassword= binding.etNewPassword.text.toString()
        val confirmPassword= binding.etConfirmPassword.text.toString()
        when{
            currentPassword.isEmpty() -> showSnack(getString(R.string.enter_current_password))
            newPassword.isEmpty() -> showSnack(getString(R.string.error_new_password))
            confirmPassword.isEmpty() -> showSnack(getString(R.string.error_confirm_password))
            (currentPassword == newPassword) -> showSnack(getString(R.string.error_new_current_match))
            (newPassword != confirmPassword) -> showSnack(getString(R.string.error_confirm_current_match))
            else->{
                val request= UpdatePasswordRequest(
                    currentPassword,newPassword, confirmPassword
                )
                vm.updatePasswordData(request)
            }
        }

    }
}