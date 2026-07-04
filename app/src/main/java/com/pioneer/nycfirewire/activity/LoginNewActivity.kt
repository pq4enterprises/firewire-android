package com.pioneer.nycfirewire.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.HideReturnsTransformationMethod
import android.text.method.LinkMovementMethod
import android.text.method.PasswordTransformationMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.facebook.*
import com.facebook.appevents.ml.Utils
import com.pioneer.nycfirewire.model.user.request.LoginRequest
import com.pioneer.nycfirewire.model.user.request.RefreshTokenRequest
import com.pioneer.nycfirewire.model.user.response.LoginResponse
import com.pioneer.nycfirewire.model.user.response.RefreshTokenResponse
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.Constants.CODE_SUCCESS
import com.pioneer.nycfirewire.utils.IntentUtils.REFRESH_TOKEN
import com.pioneer.nycfirewire.utils.NetworkUtils.isOnline
import com.pioneer.nycfirewire.viewModel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint

import com.facebook.login.LoginResult

import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn

import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException

import com.google.android.gms.auth.api.signin.GoogleSignInAccount

import com.pioneer.nycfirewire.model.user.request.SocialLoginRequest
import com.pioneer.nycfirewire.utils.Constants.FACEBOOK
import com.pioneer.nycfirewire.utils.Constants.GOOGLE
import com.google.android.gms.tasks.Task
import com.onesignal.OneSignal
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.ActivityLoginBinding
import com.pioneer.nycfirewire.utils.AppUtils
import com.pioneer.nycfirewire.utils.Constants.CODE_EMAIL_NOT_VERIFIED
import com.pioneer.nycfirewire.utils.Constants.EMAIL_ID
import com.pioneer.nycfirewire.utils.Constants.FORGOT_PASSWORD
import com.pioneer.nycfirewire.utils.Constants.FROM_PAGE
import com.pioneer.nycfirewire.utils.Constants.LOGIN
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.isValidEmail
import com.pioneer.nycfirewire.utils.showToast
import com.pioneer.nycfirewire.utils.startNewActivity
import com.pioneer.nycfirewire.utils.visible


@AndroidEntryPoint
class LoginNewActivity: BaseActivity() {

    private var callbackManager: CallbackManager? = null
    private lateinit var binding: ActivityLoginBinding
    private lateinit var vm: UserViewModel
   // private lateinit var googleSignInClient: GoogleSignInClient
   // private lateinit var firebaseAuth: FirebaseAuth
    private var isPasswordVisible: Boolean = false
    private val RC_SIGN_IN = 9001
    private var mGoogleSignInClient: GoogleSignInClient? = null
    private var isFaceBook= false


    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUi()
        initViewModel()
        clickEvent()
        callbackManager = CallbackManager.Factory.create();

        if(prefs.isDarkMode) {
            binding.nsView.setBackgroundColor(resources.getColor(R.color.white))
        }else binding.nsView.background =resources.getDrawable(R.drawable.ic_login_bg)

    }

/*    override fun onResume() {
        super.onResume()
        analyticMethod(LOGIN,"LoginActivity")
    }*/

    private fun initViewModel() {
        vm = ViewModelProvider(this).get(UserViewModel::class.java)
        vm.loginLiveData.observe(this){
           updateLoginData(it)
        }
        vm.refreshLiveData.observe(this){
            updateRefreshData(it)
        }

        vm.socialLoginLiveData.observe(this){
            updateLoginData(it)
        }


        if(intent.getStringExtra(REFRESH_TOKEN)!=null){
            val refrehToken = intent.getStringExtra(REFRESH_TOKEN)
            if(isOnline(this)){
                vm.refreshToken(RefreshTokenRequest(refrehToken!!))
            } else {
            showSnack(getString(R.string.check_network_connection))
        }

        }
    }

    private fun updateRefreshData(response: Resource<RefreshTokenResponse>?) {
        when(response?.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                response.data?.let { it1 ->
                    val data= it1.data
                    prefs.refreshToken = data.refreshToken

                    val intent = Intent(this, MapsActivity::class.java)
                    startActivity(intent)
                }
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showAlert(response.message)
            }
            else -> {}
        }
    }

    private fun updateLoginData(response: Resource<LoginResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                if(response.data?.code== CODE_SUCCESS) {
                    response.data.let { it1 ->
                        prefs.token = it1.data.token
                        prefs.refreshToken = it1.data.refreshToken
                        prefs.isLogin = true
                        prefs.userId= it1.data._id
                        prefs.userRole= it1.data.role
                        prefs.showAppIntro=true
                        prefs.showHomeIntro=true
                        prefs.showFeedIntro=true
                        startNewActivity(MapsActivity::class.java)
                       val externalId = prefs.userId.toString()
                        OneSignal.login(externalId.toString())
                        OneSignal.User.addEmail(binding.etEmail.text.toString())
                       // println("onesignalId:"+ OneSignal.User.onesignalId)
                        //println("onesignalId1:"+ OneSignal.User)
                    }
                }else if(response.data?.code== CODE_EMAIL_NOT_VERIFIED){
                    moveToOtpScreen(binding.etEmail.text.toString())
                }else showToast(this,response.data?.message.toString())
                    //showSnack(response.data?.message.toString())
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showAlert(response.message)
            }
        }
    }

    private fun moveToOtpScreen(email: String) {
        var intent = Intent(this, VerifyEmailOtpActivity::class.java)
        intent.putExtra(EMAIL_ID,email)
        intent.putExtra(FROM_PAGE,LOGIN)
        startActivity(intent)
        finish()
    }

    private fun clickEvent() {
        binding.btnSignIn.setOnClickListener {
            if (isOnline(this)) {
                if (binding.etEmail.text.toString().isEmpty()) {
                    showSnack(getString(R.string.enter_email))
                } else if (!isValidEmail(binding.etEmail.text.toString())) {
                    showSnack(getString(R.string.enter_valid_email))
                } else if (binding.etPassword.text.toString().isEmpty()) {
                    showSnack(getString(R.string.enter_password))
                } else {
                    val loginRequest = LoginRequest(
                        email = binding.etEmail.text.toString(),
                        password = binding.etPassword.text.toString()
                    )
                    vm.loginUser(loginRequest)
                }
            } else {
                showSnack(getString(R.string.check_network_connection))
            }

        }
        binding.cvFb.setOnClickListener {
            isFaceBook= true
            LoginManager.getInstance()
                .logInWithReadPermissions(this, listOf("email", "public_profile"))
            fbLogin()
        }

        binding.cvGoogle.setOnClickListener {
            isFaceBook=false
            val signInIntent = mGoogleSignInClient!!.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        binding.tvForgot.setOnClickListener {
            val intent= Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        binding.btnShowHidePassword.setOnClickListener {
            if (isPasswordVisible) {
                // Hide the password
                binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance();
                binding.btnShowHidePassword.setImageResource(R.drawable.ic_hide_password);
            } else {
                // Show the password
                binding.etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance();
                binding.btnShowHidePassword.setImageResource(R.drawable.ic_show_password);
            }
            isPasswordVisible = !isPasswordVisible;
            // Move the cursor to the end of the text after the transformation change
            binding.etPassword.setSelection(binding.etPassword.text.length);
        }

        binding.etPassword.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) binding.nsView.post {
                binding.nsView.smoothScrollTo(0, v.bottom)
            }
        }




    }

    private fun fbLogin() {
        val callbackManager = CallbackManager.Factory.create()

        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {

            override fun onError(error: FacebookException) {
                Log.e("FacebookLogin", "Error during login: ${error?.message}")
            }

            override fun onSuccess(result: LoginResult) {
                val accessToken = result?.accessToken
                fetchUserProfile(accessToken)
            }

            override fun onCancel() {
                Log.d("FacebookLogin", "Login cancelled")
            }
        })

    }


    private fun fetchUserProfile(accessToken: AccessToken?) {
        val request = GraphRequest.newMeRequest(
            accessToken
        ) { `object`, response ->
            // Here you can retrieve user data from `object`
            val name = `object`?.getString("name")
            val email = `object`?.getString("email")
            Log.d("FacebookLogin", "Name: $name, Email: $email")

            if(AppUtils.isNetworkConnected(this)) {
                vm.postSocialLogin(SocialLoginRequest(accessToken?.token.toString(), FACEBOOK))
            }else showSnack(getString(R.string.check_network_connection))

        }

        val parameters = Bundle()
        parameters.putString("fields", "id,name,email")
        request.parameters = parameters
        request.executeAsync()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(isFaceBook){
            callbackManager?.onActivityResult(requestCode, resultCode, data)
        }else{
            if (requestCode == RC_SIGN_IN) {
                val task: Task<GoogleSignInAccount> =
                    GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    // Signed in successfully, show authenticated UI
                    val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                    // Use account information
                    updateUI(account)
                } catch (e: ApiException) {
                    // Sign-in failed, handle the error
                    Log.w("Google Sign-In", "signInResult:failed code=" + e.statusCode)
                    updateUI(null)
                }
            }
        }
    }

    private fun updateUI(account: GoogleSignInAccount?) {
        try {
            if (account != null) {
                val personName = account.givenName
                val familyName = account.familyName
                val personEmail = account.email
                val idToken = account.idToken // This is useful for server-side authentication


                if(AppUtils.isNetworkConnected(this)) {
                   // vm.postSocialLogin(SocialLoginRequest(idToken.toString(), GOOGLE))

                    vm.postSocialLogin(SocialLoginRequest(socialType = GOOGLE,
                        firstName = personName.toString(),
                        lastName = familyName.toString(),
                        email = personEmail.toString()))
                }else showSnack(getString(R.string.check_network_connection))

            } else {
                // Show a login failure UI
                showToast(this,"Error occured")
            }
        }catch (e:Exception){
            e.printStackTrace()
        }
    }



    private fun initUi() {
        setMessageWithClickableLink(this,binding.tvRegister)
        setMessageForTermsAndCondition(this,binding.tvTermsCondition)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // OAuth 2.0 client ID
            .requestEmail()
            .build()


        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        mGoogleSignInClient?.signOut()?.addOnCompleteListener {
            // Now revoke access to ensure complete disconnection
            mGoogleSignInClient?.revokeAccess()?.addOnCompleteListener {
                // User is now fully signed out and disconnected
            }
        }
    }

    private fun setMessageWithClickableLink(context: Context, textView: TextView) {
        val content = context.getString(R.string.account_register)
        val clickableSpan = object : ClickableSpan(){
            override fun onClick(widget: View) {
                val intent= Intent(this@LoginNewActivity, RegisterActivity::class.java)
                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.isUnderlineText=false
            }
        }
        val startIndex = content.indexOf("Register")
        val endIndex = content.lastIndexOf("Register")+"Register".length
        val spannableString = SpannableString(content)
        val spanColor= ForegroundColorSpan(Color.RED)

        spannableString.setSpan(clickableSpan, startIndex, endIndex,   Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(spanColor, startIndex, endIndex,   Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.setText(spannableString)
        textView.setTextColor(getColor(R.color.black))
        textView.setMovementMethod(LinkMovementMethod.getInstance())
        textView.setHighlightColor(Color.TRANSPARENT)
    }


    private fun setMessageForTermsAndCondition(context: Context, textView: TextView) {
        val content = context.getString(R.string.terms_condition)
        val clickableSpan = object : ClickableSpan(){
            override fun onClick(widget: View) {
                moveToLink("https://nycfirewire.net/terms")
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.isUnderlineText=false
            }
        }
        val startIndex = content.indexOf("Terms and Conditions")
        val endIndex = content.lastIndexOf("Terms and Conditions")+"Terms and Conditions".length
        val spannableString = SpannableString(content)
        val spanColor= ForegroundColorSpan(Color.RED)

        spannableString.setSpan(clickableSpan, startIndex, endIndex,   Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(spanColor, startIndex, endIndex,   Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.setText(spannableString)
        textView.setTextColor(getColor(R.color.black))
        textView.setMovementMethod(LinkMovementMethod.getInstance())
        textView.setHighlightColor(Color.TRANSPARENT)
    }





}