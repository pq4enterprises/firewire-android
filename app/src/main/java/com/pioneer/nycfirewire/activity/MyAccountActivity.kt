package com.pioneer.nycfirewire.activity

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.android.billingclient.api.*
import com.bumptech.glide.Glide
import com.pioneer.nycfirewire.model.user.response.UserDetails
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.utils.Constants.SUB_PRODUCT_ID
import com.pioneer.nycfirewire.utils.IntentUtils.UPDATE_PROFILE
import com.pioneer.nycfirewire.utils.showToast
import com.google.android.gms.common.util.CollectionUtils.listOf
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.onesignal.OneSignal
import com.pioneer.nycfirewire.databinding.ActivityMyAccountBinding
import com.pioneer.nycfirewire.model.payment.PaymentRequest
import com.pioneer.nycfirewire.model.user.response.CommonResponse
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.service.BackgroundAudioService
import com.pioneer.nycfirewire.utils.Constants
import com.pioneer.nycfirewire.utils.Constants.MAP_PAGE
import com.pioneer.nycfirewire.utils.Constants.MY_ACCOUNT
import com.pioneer.nycfirewire.utils.Constants.USER_BASIC_USER
import com.pioneer.nycfirewire.utils.Constants.USER_PREMIUM_FREE
import com.pioneer.nycfirewire.utils.DateUtils.formatToIso8601
import com.pioneer.nycfirewire.utils.DateUtils.getExpiryDate
import com.pioneer.nycfirewire.utils.IntentUtils.FROM_ACCOUNT
import com.pioneer.nycfirewire.utils.IntentUtils.OTHER
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.startNewActivity
import com.pioneer.nycfirewire.utils.visible
import com.pioneer.nycfirewire.viewModel.FireWireViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import kotlin.String


@AndroidEntryPoint
class MyAccountActivity : BaseActivity(), PurchasesUpdatedListener,PurchaseHistoryResponseListener {

    private  var skuItem:ProductDetails?=null
    private lateinit var binding: ActivityMyAccountBinding
    private var isPurchased= false
    private var userDetails = UserDetails()
    private lateinit var billingClient: BillingClient
    private lateinit var vm: FireWireViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)

        initExtra()
        clickEvent()
        initUi()

        vm.paymentLiveData.observe(this, Observer{
            updatePayment(it)
        })
    }

    private fun updatePayment(response: Resource<CommonResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                prefs.userRole= USER_PREMIUM_FREE
                binding.progress.gone()
                println("PaymentSuccess"+response)
                 if(response.data?.code== Constants.CODE_SUCCESS) {
                    println("You are an active user now!")
                     userView()
                 }
            }
            ResourceState.ERROR -> {
                binding.progress.gone()

            }
        }
    }

    override fun onResume() {
        super.onResume()

        analyticMethod(MY_ACCOUNT,"MyAccountActivity")

        if(prefs.userImg?.isNotEmpty() == true) {
            Glide.with(this)
                .load(prefs.userImg)
                .into(binding.ivProfile)
            userDetails.img= prefs.userImg
        }

        if(prefs.userFirstName?.isNotEmpty() == true) {
            binding.tvProfileName.text = prefs.userFirstName?.plus(" ").plus(prefs.userLastName)
            binding.tvProfileEmail.text = prefs.userEmail
        }

        if(isPurchased) {
            binding.btnGetNow.gone()
            binding.btnSubscribe.visible()
        }
    }

    private fun initExtra() {
        var from= intent.getStringExtra(FROM_ACCOUNT)

        if(from==OTHER){
            billingView()
        }else{
            userDetails = intent.getParcelableExtra(UPDATE_PROFILE) ?: UserDetails()
            binding.tvProfileName.text = userDetails.firstName.plus(" ").plus(userDetails.lastName)
            binding.tvProfileEmail.text = userDetails.email
        }


    }

    private fun initUi() {

        billingClient = BillingClient.newBuilder(this)
            .enablePendingPurchases()
            .setListener(this)
            .build()



        billingClient.startConnection(object : BillingClientStateListener {
            override
            fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    loadProducts()
                    // The BillingClient is ready. You can query purchases here.
                    queryInventoryAsync(); // This is used to fetch purchased items from google play store
                    checkIfUserAlreadyPurchased()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                showToast(this@MyAccountActivity, "Billing service disconnected...")
            }
        }
        )


    }

    private fun userView(){
        binding.tvTermsService.visible()
        binding.tvPrivacyPolicy.visible()
        binding.tvSignOut.visible()
        binding.userView.gone()
        binding.ivClose.gone()
        binding.bgView.gone()
    }

    private fun billingView(){
       // binding.tvTermsService.gone()
       // binding.tvPrivacyPolicy.gone()
        binding.tvTermsService.setTextColor(getColor(R.color.white))
        val drawable: Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_terms_service)
        drawable?.let { it.setColorFilter(ContextCompat.getColor(this, R.color.white), android.graphics.PorterDuff.Mode.SRC_IN) }

        val drawable1: Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_right_arrow)
        drawable1?.let { it.setColorFilter(ContextCompat.getColor(this, R.color.white), android.graphics.PorterDuff.Mode.SRC_IN) }

        binding.tvPrivacyPolicy.setTextColor(getColor(R.color.white))
        binding.tvPrivacyPolicy.setCompoundDrawablesWithIntrinsicBounds(drawable, null, drawable1, null)
        binding.tvTermsService.setCompoundDrawablesWithIntrinsicBounds(drawable, null, drawable1, null)

        binding.tvSignOut.gone()
        binding.userView.visible()
        binding.ivClose.visible()
        binding.bgView.visible()
    }




  /*  fun checkIfUserAlreadyPurchased(billingClient: BillingClient) {
        billingClient.queryPurchasesAsync(BillingClient.ProductType.SUBS) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (purchases.isNotEmpty()) {
                    for (purchase in purchases) {
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && purchase.isAcknowledged) {
                            isPurchased=true
                            println("User already has an active subscription: ${purchase.skus}")
                        }
                    }
                }else    isPurchased=false
                println("No active subscriptions found")
            }else if (billingResult.responseCode == BillingClient.BillingResponseCode.ERROR) {
                // Unknown error, maybe retry or show a generic error message
                println("Billing An unknown error occurred: ${billingResult.debugMessage}")
            } else {
                println("Failed to query purchases: ${billingResult.debugMessage}")
            }
        }

    }*/


    private fun checkIfUserAlreadyPurchased() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS) // or SUBS if subscription
                .build()
        ) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {

                if(purchasesList.isNotEmpty()){
                    if(purchasesList.filter { it.products.contains(SUB_PRODUCT_ID)}.isNotEmpty()){
                for (purchase in purchasesList) {
                    if (purchase.products.contains(SUB_PRODUCT_ID)) {
                        val isAutoRenewing = purchase.isAutoRenewing
                        val purchaseTime = purchase.purchaseTime
                        val currentTime = System.currentTimeMillis()
                        val isExpired = !isAutoRenewing && (purchaseTime < currentTime)

                        if (isExpired) {
                            isPurchased=false
                            binding.btnGetNow.visible()
                            binding.btnSubscribe.gone()
                            Log.d("SubscriptionCheck", "Subscription is expired")
                            //TODO call update profile api BASEIC_USER
                        } else {
                            isPurchased=true
                            binding.btnGetNow.gone()
                            binding.btnSubscribe.visible()
                            if (prefs.userRole == USER_BASIC_USER) {
                                //TODO call update profile api PREMIUM_FREE
                            }
                            Log.d("SubscriptionCheck", "Subscription is active")
                        }
                    }
                    }}else{
                        basedOnUserRoleWithoutPurchase()
                    }
                }else{
                    basedOnUserRoleWithoutPurchase()
                }

            } else {
                showSnack("Error checking purchases: ${billingResult.debugMessage}")
            }
        }
    }

    private fun basedOnUserRoleWithoutPurchase(){
        if(prefs.userRole == USER_PREMIUM_FREE){
            isPurchased=true
            binding.btnGetNow.gone()
            binding.btnSubscribe.visible()
        }
    }


    private fun getProductList(): ArrayList<String> {
        val productIdsList = ArrayList<String>()
        productIdsList.add(SUB_PRODUCT_ID);

        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(productIdsList)
            .setType(BillingClient.SkuType.SUBS)

        return productIdsList
    }

    private fun loadProducts() {
        billingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.SUBS, this)
    }

    private fun queryInventoryAsync() {
        if (getProductList().isNullOrEmpty()) {
            return
        }
        var productList =
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(SUB_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) {
                billingResult,
                productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (!productDetailsList.isEmpty()) {
                    for (item in productDetailsList) {
                        skuItem = item
                       binding.tvPrice.text= skuItem?.subscriptionOfferDetails?.get(0)?.pricingPhases?.pricingPhaseList?.get(0)?.formattedPrice
                    }
                }
            }
        }
    }

    private fun clickEvent() {
        binding.tvBack.setOnClickListener {
            finish()
        }

        binding.ivClose.setOnClickListener {
            finish()
        }
        binding.tvUpdateProfile.setOnClickListener {
            val intent = Intent(this, UpdateProfileActivity::class.java)
            intent.putExtra(UPDATE_PROFILE, userDetails)
            startActivity(intent)
        }

        binding.btnGetNow.setOnClickListener {
            if(!isPurchased)  {
                if(skuItem!=null) {

                    val productDetailsParamsList = listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                            .setProductDetails(skuItem!!)
                            // For One-time product, "setOfferToken" method shouldn't be called.
                            // For subscriptions, to get the offer token corresponding to the selected
                            // offer call productDetails.subscriptionOfferDetails?.get(selectedOfferIndex)?.offerToken
                            .setOfferToken(skuItem?.subscriptionOfferDetails?.get(0)?.offerToken.toString())
                            .build()
                    )

                    val flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build()
                    billingClient.launchBillingFlow(this, flowParams)
                }
            }else{
                showToast(this,"Already Purchased")
            }

        }

        binding.tvTermsService.setOnClickListener {
            moveToLink("https://nycfirewire.net/terms")
        }
        binding.tvPrivacyPolicy.setOnClickListener {
            moveToLink("https://nycfirewire.net/privacy")
        }
        binding.tvSignOut.setOnClickListener {

            showSignOut(getString(R.string.you_want_to_sign_out))

        }
        //ButtonConstants.ButtonType = SUBSCRIBE
    }

    fun showSignOut(message: String? = "") {
        AlertDialog.Builder(this)
            .setTitle(resources.getString(R.string.app_name))
            .setMessage(message)
            .setCancelable(false)
            .setNegativeButton(android.R.string.cancel) { _, _ ->
            }
            .setPositiveButton(R.string.sign_out) { _, _ ->

                prefs.deleteToken
                prefs.isLogin = false
                startNewActivity(LoginNewActivity::class.java)
                prefs.userImg= ""
                prefs.userFirstName= ""
                prefs.userLastName= ""
                prefs.userEmail= ""
                prefs.soundName=""

                BackgroundAudioService.stopService(this)
                prefs.feedMainPosition=-1
                prefs.feedSubPosition= -1

                AppCompatDelegate
                    .setDefaultNightMode(
                        AppCompatDelegate
                            .MODE_NIGHT_NO);

                prefs.isDarkMode= false

                OneSignal.logout();
            }
            .show()
    }


    private fun acknowledgePurchase(purchaseToken: String, purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            println("billingResult:"+ billingResult.responseCode )
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                callPaymentBackendApi(purchase)
                println("acknowledge Purchase completed")
            }else{
                println("Exit in acknowledge Purchase")
            }
        }
    }


    fun callPaymentBackendApi(purchase: Purchase) {
        try {
            isPurchased=true
            binding.btnGetNow.gone()
            binding.btnSubscribe.visible()
            binding.tvFullAccess.text= "Premium Account"

            var pricing= skuItem?.subscriptionOfferDetails?.get(0)?.pricingPhases?.pricingPhaseList?.get(0)

            var paymentRequest = PaymentRequest(
                userId = prefs.userId.toString(),
                paymentMethod = "Play Store",
                paymentToken = purchase.purchaseToken,
                transactionId = purchase.orderId.toString(),
                amount= pricing?.formattedPrice.toString(),
                currency=pricing?.priceCurrencyCode.toString(),
                status= "success",
                purchaseDate= formatToIso8601(purchase.purchaseTime) ,
                expiredDate= getExpiryDate(purchase.purchaseTime),
                type= if (purchase.isAutoRenewing) "Auto-renewable subscription" else "one-time"
            )
            println("PaymentRequest"+paymentRequest)
            vm.paymentPost(paymentRequest)
        }catch (e: Exception){
            e.printStackTrace()
        }

    }



    override fun onPurchasesUpdated(billingResult: BillingResult, purchaseList: MutableList<Purchase>?) {
        if (billingResult?.responseCode == BillingClient.BillingResponseCode.OK && purchaseList != null) {
            for (purchase in purchaseList!!) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                    acknowledgePurchase(purchase.purchaseToken,purchase)
                }
            }
        } else if (billingResult?.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            showToast(this,"You've cancelled the Google play billing process...")
        } else {
            showToast(this,"Item not found or Google play billing error...")
        }
    }

    override fun onPurchaseHistoryResponse(
        billingResult: BillingResult,
        purchaseHistoryList: MutableList<PurchaseHistoryRecord>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            if (!purchaseHistoryList.isNullOrEmpty()) {

                var purchase= purchaseHistoryList[0]
                val purchaseTimestamp: Long = purchase.purchaseTime
                val days = getDaysSincePurchase(purchaseTimestamp)
            }
        }


    }

    fun getDaysSincePurchase(purchaseTimestamp: Long): Int {
        val purchaseDate = Date(purchaseTimestamp)

        val calendarPurchase = Calendar.getInstance().apply {
            time = purchaseDate
        }

        val calendarToday = Calendar.getInstance()

        val diffInMillis = calendarToday.timeInMillis - calendarPurchase.timeInMillis
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    }






}