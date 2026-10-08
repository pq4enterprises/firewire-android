package com.pioneer.nycfirewire.activity

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import androidx.lifecycle.ViewModelProvider
import com.pioneer.nycfirewire.model.user.request.RegisterRequest
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.Constants.CODE_SUCCESS
import com.pioneer.nycfirewire.viewModel.UserViewModel
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.ActivityRegisterBinding
import com.pioneer.nycfirewire.utils.AuthUi
import com.pioneer.nycfirewire.utils.Constants.CODE_EMAIL_NOT_VERIFIED
import com.pioneer.nycfirewire.utils.Constants.EMAIL_ID
import com.pioneer.nycfirewire.utils.Constants.REGISTRATION
import com.pioneer.nycfirewire.utils.NetworkUtils.isOnline
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.visible
import dagger.hilt.android.AndroidEntryPoint


/**
 * Register (2026 auth redesign). Presentation only: per-field inline errors,
 * a banner for server/network errors, Return-key flow, side-by-side rows that
 * stack at large font scale. The register call and the hand-off to email
 * verification are unchanged.
 */
@AndroidEntryPoint
class RegisterActivity : BaseActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var vm: UserViewModel

    private var isPasswordVisible: Boolean = false
    private var isConfirmPassword: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUi()
        initViewModel()
        clickEvent()
        AuthUi.applyEdgeToEdge(this, binding.root, topBar = binding.toolbar.root, bottomPad = binding.llSaveBar, darkTop = false)
    }

    override fun onResume() {
        super.onResume()
        analyticMethod(REGISTRATION,"RegisterActivity")
    }

    private fun initUi() {
        AuthUi.setLinkSentence(binding.tvHaveAccount, getString(R.string.fw_auth_signin_link), "Sign in") {
            startActivity(Intent(this@RegisterActivity, LoginNewActivity::class.java))
        }
        binding.toolbar.tvToolbarTitle.text= getString(R.string.sign_up).uppercase()
        binding.toolbar.tvToolbarTitle.visible()
        binding.toolbar.ivFeed.gone()

        binding.etConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance();
        binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance();

        AuthUi.stackRowsForLargeText(this, binding.llNameRow, binding.llPhoneRow)

        AuthUi.clearOnEdit(binding.etFirstName, binding.tvFirstNameError, binding.tvFormError)
        AuthUi.clearOnEdit(binding.etLastName, binding.tvLastNameError, binding.tvFormError)
        AuthUi.clearOnEdit(binding.etEmailAddress, binding.tvEmailError, binding.tvFormError)
        AuthUi.clearOnEdit(binding.etPhone, binding.tvPhoneError, binding.tvFormError)
        AuthUi.clearOnEdit(binding.etTitle, binding.tvTitleError, binding.tvFormError)
        AuthUi.clearOnEdit(binding.etPassword, binding.tvPasswordError, binding.tvFormError)
        AuthUi.clearOnEdit(binding.etConfirmPassword, binding.tvConfirmPasswordError, binding.tvFormError)
        AuthUi.submitOnDone(binding.etConfirmPassword) { validateData() }
    }

    private fun initViewModel() {
        vm = ViewModelProvider(this).get(UserViewModel::class.java)
        vm.registerLiveData.observe(this){
            when(it.state){
                ResourceState.LOADING -> binding.progress.visible()
                ResourceState.SUCCESS -> {
                    binding.progress.gone()
                    it.data?.let { it1 ->
                      //  {"message":"This email address already registered. Please try a different email.","code":"already_registered"}

                        // The server emails a 6-digit code on every successful registration and
                        // also when an unverified address registers again ("email_not_verified").
                        // Either way the next step is the verification screen (same as iOS).
                        if(it1.code==CODE_SUCCESS || it1.code==CODE_EMAIL_NOT_VERIFIED) {
                            moveToEmailOtpVerification()
                        }else showFormError(it1.message)
                    }
                }
                ResourceState.ERROR -> {
                    binding.progress.gone()
                    showFormError(it.message)
                }
            }
        }
    }

    private fun showFormError(message: String?) {
        AuthUi.showBanner(binding.tvFormError, AuthUi.friendly(this, message), AuthUi.Banner.ERROR)
    }

    private fun moveToEmailOtpVerification() {
        var intent = Intent(this, VerifyEmailOtpActivity::class.java)
        intent.putExtra(EMAIL_ID,binding.etEmailAddress.text.toString().trim().lowercase())
        startActivity(intent)
        finish()
    }

    private fun clickEvent() {
        binding.toolbar.ivMenu.setOnClickListener {
            finish()
        }

        binding.btnSignIn.setOnClickListener {
            validateData()
        }

        binding.ivPasswordShow.setOnClickListener {
            if (isPasswordVisible) {
                // Hide the password
                binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.ivPasswordShow.setImageResource(R.drawable.ic_hide_password)
            } else {
                // Show the password
                binding.etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                binding.ivPasswordShow.setImageResource(R.drawable.ic_show_password)
            }
            isPasswordVisible = !isPasswordVisible;
            // Move the cursor to the end of the text after the transformation change
            binding.etPassword.setSelection(binding.etPassword.text.length)
        }

        binding.ivConfirmPasswordShow.setOnClickListener {
            if (isConfirmPassword) {
                // Hide the password
                binding.etConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.ivConfirmPasswordShow.setImageResource(R.drawable.ic_hide_password);
            } else {
                // Show the password
                binding.etConfirmPassword.transformationMethod = HideReturnsTransformationMethod.getInstance();
                binding.ivConfirmPasswordShow.setImageResource(R.drawable.ic_show_password)
            }
            isConfirmPassword = !isConfirmPassword;
            // Move the cursor to the end of the text after the transformation change
            binding.etConfirmPassword.setSelection(binding.etConfirmPassword.text.length);
        }
    }

    /** Same rules as before, surfaced per field instead of one snackbar at a time. */
    private fun validateData() {
        hideKeyboard()
        AuthUi.hideBanner(binding.tvFormError)

        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val email = binding.etEmailAddress.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val title = binding.etTitle.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirm = binding.etConfirmPassword.text.toString()

        val errors = listOf(
            Triple(binding.etFirstName, binding.tvFirstNameError, if (firstName.isEmpty()) getString(R.string.fw_auth_error_first_name) else null),
            Triple(binding.etLastName, binding.tvLastNameError, if (lastName.isEmpty()) getString(R.string.fw_auth_error_last_name) else null),
            Triple(binding.etEmailAddress, binding.tvEmailError, AuthUi.emailError(this, email)),
            Triple(binding.etPhone, binding.tvPhoneError, when {
                phone.isEmpty() -> getString(R.string.fw_auth_error_phone_empty)
                phone.length < 10 -> getString(R.string.fw_auth_error_phone_invalid)
                else -> null
            }),
            Triple(binding.etTitle, binding.tvTitleError, if (title.isEmpty()) getString(R.string.fw_auth_error_title) else null),
            Triple(binding.etPassword, binding.tvPasswordError, AuthUi.passwordError(this, password)),
            Triple(binding.etConfirmPassword, binding.tvConfirmPasswordError, when {
                confirm.isEmpty() -> getString(R.string.fw_auth_error_confirm_empty)
                password != confirm -> getString(R.string.fw_auth_error_confirm_mismatch)
                else -> null
            })
        )
        errors.forEach { (input, errorView, message) -> AuthUi.setFieldError(input, errorView, message) }
        errors.firstOrNull { it.third != null }?.let { (input, _, _) ->
            input.requestFocus()
            return
        }

        val registerData= RegisterRequest(
             email= email,
         firstName = firstName,
            lastName= lastName,
         mobile = phone,
         password = password,
         title= title
        )
        if(isOnline(this)) vm.registerUser(registerData) else showFormError(getString(R.string.fw_auth_no_connection))
    }
}
