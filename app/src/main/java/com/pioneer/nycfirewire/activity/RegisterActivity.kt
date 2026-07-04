package com.pioneer.nycfirewire.activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.*
import android.text.method.HideReturnsTransformationMethod
import android.text.method.LinkMovementMethod
import android.text.method.PasswordTransformationMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.pioneer.nycfirewire.model.user.request.RegisterRequest
import com.pioneer.nycfirewire.model.user.response.RegisterResponse
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.Constants.CODE_SUCCESS
import com.pioneer.nycfirewire.viewModel.UserViewModel
import com.onesignal.OneSignal
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.ActivityRegisterBinding
import com.pioneer.nycfirewire.utils.Constants.EMAIL_ID
import com.pioneer.nycfirewire.utils.Constants.FROM_START
import com.pioneer.nycfirewire.utils.Constants.NOTIFICATION_AREAS
import com.pioneer.nycfirewire.utils.Constants.REGISTRATION
import com.pioneer.nycfirewire.utils.NetworkUtils.isOnline
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.isValidEmail
import com.pioneer.nycfirewire.utils.startNewActivity
import com.pioneer.nycfirewire.utils.visible
import dagger.hilt.android.AndroidEntryPoint


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

        setMandatory(binding.tvFirstName,getString(R.string.first_name))
        setMandatory(binding.tvLastName,getString(R.string.last_name))
        setMandatory(binding.tvEmailName,getString(R.string.email_address))
        setMandatory(binding.tvPhoneNum,getString(R.string.phone_num))
        setMandatory(binding.tvTitlePos,getString(R.string.title_position))
        setMandatory(binding.tvPassword,getString(R.string.password))
        setMandatory(binding.tvConfirmPassword,getString(R.string.confirm_password))

    }

  /*  override fun onResume() {
        super.onResume()
        analyticMethod(REGISTRATION,"RegisterActivity")
    }*/

    private fun initUi() {
        setMessageWithClickableLink(this,binding.tvHaveAccount)
        binding.toolbar.tvTitle.text=""

        binding.etConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance();
        binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance();
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

                        if(it1.code==CODE_SUCCESS) {

                            //showSnack(getString(R.string.register_success))
                            showSnack(it1.message)
                            moveToEmailOtpVerification()


                            /*if (!it1.data.token.isNullOrEmpty()) {
                                prefs.token = it1.data.token
                                prefs.refreshToken = it1.data.refreshToken
                                prefs.isLogin = true
                                prefs.userId = it1.data._id
                                prefs.userRole= it1.data.role
                                prefs.showAppIntro=true
                                prefs.showHomeIntro=true
                                prefs.showFeedIntro=true
                                OneSignal.login(prefs.userId.toString())
                                OneSignal.User.addEmail(binding.etEmailAddress.text.toString())
                                println("onesignalId:" + OneSignal.User.onesignalId)
                                moveToLogin(it1)

                            }*/
                        }else showSnack(it1.message)
                    }
                }
                ResourceState.ERROR -> {
                    binding.progress.gone()
                    showAlert(it.message)
                }
            }
        }
    }

    private fun moveToEmailOtpVerification() {
        var intent = Intent(this, VerifyEmailOtpActivity::class.java)
        intent.putExtra(EMAIL_ID,binding.etEmailAddress.text.toString())
        startActivity(intent)
        finish()
    }
    // numz@gmail.com, qwert123#

   /* private fun moveToLogin(response: RegisterResponse) {
        if(response.code==CODE_SUCCESS){
            var intent = Intent(this, SelectAreaActivity::class.java)
            intent.putExtra(FROM_START,true)
            startActivity(intent)
            finish()
        }

    }*/

    private fun clickEvent() {
        binding.toolbar.tvBack.setOnClickListener {
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

    private fun validateData() {
        if(binding.etFirstName.text.toString().trim().isEmpty()){
            showSnack(getString(R.string.enter_first_name))
        }else if(binding.etLastName.text.toString().trim().isEmpty()){
            showSnack(getString(R.string.enter_last_name))
        }else if(binding.etEmailAddress.text.toString().trim().isEmpty()){
            showSnack(getString(R.string.enter_email))
        }else if(!isValidEmail(binding.etEmailAddress.text.toString())){
            showSnack(getString(R.string.enter_valid_email))
        }else if(binding.etPhone.text.toString().trim().isEmpty()){
            showSnack(getString(R.string.enter_your_phone))
        }else if(binding.etPhone.text.toString().trim().length<10){
            showSnack(getString(R.string.enter_valid_phone))
        }else if(binding.etTitle.text.toString().trim().isEmpty()){
            showSnack(getString(R.string.enter_your_title))
        }else if(binding.etPassword.text.toString().trim().isEmpty()){
            showSnack(getString(R.string.enter_password))
        }else if(binding.etPassword.text.toString().trim().length<8){
            showSnack(getString(R.string.password_length))
        }else if(binding.etConfirmPassword.text.toString().trim().isEmpty()){
            showSnack(getString(R.string.enter_confirm_password))
        }else if(binding.etPassword.text.toString().trim()!= binding.etConfirmPassword.text.toString().trim()){
            showSnack(getString(R.string.password_confirm_same))
        }else{
            val registerData= RegisterRequest(
                 email= binding.etEmailAddress.text.toString(),
             firstName = binding.etFirstName.text.toString(),
                lastName= binding.etLastName.text.toString(),
             mobile = binding.etPhone.text.toString(),
             password = binding.etPassword.text.toString(),
             title= binding.etTitle.text.toString()
            )
            if(isOnline(this)) vm.registerUser(registerData) else showSnack(getString(R.string.check_network_connection))
        }
    }



    private fun setMessageWithClickableLink(context: Context, textView: TextView) {
        val content = context.getString(R.string.have_account_signin)
        val clickableSpan = object : ClickableSpan(){
            override fun onClick(widget: View) {
                val intent = Intent(this@RegisterActivity, LoginNewActivity::class.java)
                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.isUnderlineText=false
            }
        }
        val startIndex = content.indexOf("Sign In")
        val endIndex = content.lastIndexOf("Sign In")+"Sign In".length
        val spannableString = SpannableString(content)
        val spanColor= ForegroundColorSpan(Color.RED)

        spannableString.setSpan(clickableSpan, startIndex, endIndex,   Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(spanColor, startIndex, endIndex,   Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.setText(spannableString)
        textView.setTextColor(getColor(R.color.black))
        textView.setMovementMethod(LinkMovementMethod.getInstance())
        textView.setHighlightColor(Color.TRANSPARENT)
    }

    private fun setMandatory(textView: TextView, valueText:String){
        val spannable = SpannableStringBuilder(valueText)
        spannable.insert(spannable.length,"*")
        spannable.setSpan(
            ForegroundColorSpan(Color.RED),
            valueText.length,
        valueText.length+1,
        Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        textView.text= spannable

    }
}