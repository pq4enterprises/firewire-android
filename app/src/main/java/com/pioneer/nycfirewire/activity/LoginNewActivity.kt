package com.pioneer.nycfirewire.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.facebook.*
import com.pioneer.nycfirewire.model.user.request.LoginRequest
import com.pioneer.nycfirewire.model.user.response.LoginResponse
import com.pioneer.nycfirewire.data.auth.TokenAuthenticator
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.Constants.CODE_SUCCESS
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
import com.pioneer.nycfirewire.utils.AuthUi
import com.pioneer.nycfirewire.utils.Constants.CODE_EMAIL_NOT_VERIFIED
import com.pioneer.nycfirewire.utils.Constants.EMAIL_ID
import com.pioneer.nycfirewire.utils.Constants.FROM_PAGE
import com.pioneer.nycfirewire.utils.Constants.LOGIN
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.startNewActivity
import com.pioneer.nycfirewire.utils.visible


/**
 * Sign in (2026 auth redesign). Presentation only: validation and server
 * errors are inline (field outline + message, or a banner inside the card),
 * the session-ended notice is an inline banner, link text uses design tokens.
 * API calls, token storage and the social providers are unchanged.
 */
@AndroidEntryPoint
class LoginNewActivity: BaseActivity() {

    companion object {
        /** One-shot success notice to show on arrival (e.g. after a password reset). */
        const val EXTRA_SUCCESS_NOTICE = "success_notice"
    }

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
        showArrivalNoticeIfNeeded()
        callbackManager = CallbackManager.Factory.create();
    }

    override fun onResume() {
        super.onResume()
        analyticMethod(LOGIN,"LoginActivity")
    }

    private fun initViewModel() {
        vm = ViewModelProvider(this).get(UserViewModel::class.java)
        vm.loginLiveData.observe(this){
           updateLoginData(it)
        }
        vm.socialLoginLiveData.observe(this){
            updateLoginData(it)
        }
    }

    /**
     * Explains why the user is looking at a login screen they did not ask for.
     *
     * Set by TokenAuthenticator in the one case where re-authentication is unavoidable
     * (the refresh token itself is dead). Everything the user needs to recover is
     * already on this screen — email, password, Forgot Password, and social sign-in —
     * so this is a persistent inline banner, not a modal the user has to dismiss
     * before they can type. The same banner carries the "password updated" notice
     * when the user arrives from the reset flow.
     */
    private fun showArrivalNoticeIfNeeded() {
        if (intent.getBooleanExtra(TokenAuthenticator.EXTRA_SESSION_EXPIRED, false)) {
            AuthUi.showBanner(binding.tvNotice, getString(R.string.fw_auth_session_ended), AuthUi.Banner.INFO)
        } else intent.getStringExtra(EXTRA_SUCCESS_NOTICE)?.let {
            AuthUi.showBanner(binding.tvNotice, it, AuthUi.Banner.SUCCESS)
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
                }else showFormError(response.data?.message)
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showFormError(response.message)
            }
        }
    }

    private fun showFormError(message: String?) {
        AuthUi.showBanner(binding.tvFormError, AuthUi.friendly(this, message), AuthUi.Banner.ERROR)
        binding.nsView.post { binding.nsView.smoothScrollTo(0, binding.tvFormError.bottom) }
    }

    private fun moveToOtpScreen(email: String) {
        var intent = Intent(this, VerifyEmailOtpActivity::class.java)
        intent.putExtra(EMAIL_ID,email)
        intent.putExtra(FROM_PAGE,LOGIN)
        startActivity(intent)
        finish()
    }

    private fun clickEvent() {
        binding.btnSignIn.setOnClickListener { attemptSignIn() }
        AuthUi.submitOnDone(binding.etPassword) { attemptSignIn() }
        AuthUi.clearOnEdit(binding.etEmail, binding.tvEmailError, binding.tvFormError)
        AuthUi.clearOnEdit(binding.etPassword, binding.tvPasswordError, binding.tvFormError)

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

    /** Inline validation (same rules as iOS), then the unchanged login call. */
    private fun attemptSignIn() {
        hideKeyboard()
        AuthUi.hideBanner(binding.tvFormError)
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val emailError = AuthUi.emailError(this, email)
        val passwordError = AuthUi.passwordError(this, password)
        AuthUi.setFieldError(binding.etEmail, binding.tvEmailError, emailError)
        AuthUi.setFieldError(binding.etPassword, binding.tvPasswordError, passwordError)
        if (emailError != null || passwordError != null) return

        if (isOnline(this)) {
            vm.loginUser(LoginRequest(email = email, password = password))
        } else {
            showFormError(getString(R.string.fw_auth_no_connection))
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
            }else showFormError(getString(R.string.fw_auth_no_connection))

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
                }else showFormError(getString(R.string.fw_auth_no_connection))

            } else {
                // The user backed out of the Google sheet, or Play Services refused: stay quiet
                // unless it looks like a real failure (same behaviour as before, minus the toast).
                Log.d("Google Sign-In", "No account returned")
            }
        }catch (e:Exception){
            e.printStackTrace()
        }
    }



    private fun initUi() {
        AuthUi.setLinkSentence(binding.tvRegister, getString(R.string.fw_auth_register_link), "Register") {
            startActivity(Intent(this@LoginNewActivity, RegisterActivity::class.java))
        }
        AuthUi.setLinkSentence(binding.tvTermsCondition, getString(R.string.terms_condition), "Terms and Conditions", uppercase = false) {
            moveToLink("https://nycfirewire.net/terms")
        }

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

}
