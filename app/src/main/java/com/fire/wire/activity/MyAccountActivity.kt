package com.fire.wire.activity

import android.content.Intent
import android.os.Bundle
import com.android.billingclient.api.*
import com.bumptech.glide.Glide
import com.fire.wire.databinding.ActivityMyAccountBinding
import com.fire.wire.model.user.response.UserDetails
import com.fire.wire.prefs
import com.fire.wire.utils.Constants.SUB_PRODUCT_ID
import com.fire.wire.utils.IntentUtils.UPDATE_PROFILE
import com.fire.wire.utils.showToast
import com.google.android.gms.common.util.CollectionUtils.listOf
import com.google.android.gms.wallet.*
import com.onesignal.OneSignal
import dagger.hilt.android.AndroidEntryPoint
import java.util.*


@AndroidEntryPoint
class MyAccountActivity : BaseActivity(), PurchasesUpdatedListener,PurchaseHistoryResponseListener {

    private  var skuItem:ProductDetails?=null
    private lateinit var binding: ActivityMyAccountBinding
    private lateinit var paymentsClient: PaymentsClient
    private var isPurchased= false

    //private var isGpayAvailable= false
    //private var LOAD_PAYMENT_DATA_REQUEST_CODE=203
    private var userDetails = UserDetails()

    private lateinit var billingClient: BillingClient



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //paymentsClient = createPaymentsClient(this)


        initExtra()
        clickEvent()
        initUi()
    }

    override fun onResume() {
        super.onResume()
        if(prefs.userImg?.isNotEmpty() == true) {
            Glide.with(this)
                .load(prefs.userImg)
                .into(binding.ivProfile)
            userDetails.img= prefs.userImg
        }
    }

    private fun initExtra() {
        userDetails = intent.getParcelableExtra(UPDATE_PROFILE) ?: UserDetails()
        binding.tvProfileName.text = userDetails.firstName.plus(" ").plus(userDetails.lastName)
        binding.tvProfileEmail.text = userDetails.email
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
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                showToast(this@MyAccountActivity, "Billing service disconnected...")
            }
        }
        )


        /*val readyToPayRequest =
            IsReadyToPayRequest.fromJson(googlePayBaseConfiguration.toString())

        val readyToPayTask = paymentsClient.isReadyToPay(readyToPayRequest)
        readyToPayTask.addOnCompleteListener{ task ->
            try {
                task.getResult(ApiException::class.java)?.let(::setGooglePayAvailable)
            } catch (exception: ApiException) {
                // Error determining readiness to use Google Pay.
                // Inspect the logs for more details.
            }
        }*/

    }

    private fun getProductList(): ArrayList<String> {
        val productIdsList = ArrayList<String>()
        productIdsList.add("monthly");

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
      /*  val params = SkuDetailsParams.newBuilder()
        params.setSkusList(getProductList()).setType(BillingClient.SkuType.SUBS)
        billingClient.querySkuDetailsAsync(params.build())
        { billingResult: BillingResult, skuDetailsList: List<SkuDetails> ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (!skuDetailsList.isNullOrEmpty()) {
                    for (item in skuDetailsList.withIndex()) {
                         skuItem = item.value

                    }
                }
            }
        }
*/

        val productList =
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
                    }
                }
            }
        }
    }

    /*fun createPaymentsClient(activity: Activity): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(WalletConstants.ENVIRONMENT_TEST).build()
        return Wallet.getPaymentsClient(activity, walletOptions)
    }

    private val baseCardPaymentMethod = JSONObject().apply {
        put("type", "CARD")
        put("parameters", JSONObject().apply {
            put("allowedCardNetworks", JSONArray(listOf("VISA", "MASTERCARD")))
            put("allowedAuthMethods", JSONArray(listOf("PAN_ONLY", "CRYPTOGRAM_3DS")))
        })
    }

    private val googlePayBaseConfiguration = JSONObject().apply {
        put("apiVersion", 2)
        put("apiVersionMinor", 0)
        put("allowedPaymentMethods",  JSONArray().put(baseCardPaymentMethod))
    }*/


    /*private fun setGooglePayAvailable(available: Boolean) {
        if (available) {
            isGpayAvailable = available
        } else {
            // Unable to pay using Google Pay. Update your UI accordingly.
            showSnack("kindly download gpay application from playstore")
        }
    }

    private val tokenizationSpecification = JSONObject().apply {
        put("type", "PAYMENT_GATEWAY")
        put("parameters", JSONObject(mapOf(
            "gateway" to "example",
            "gatewayMerchantId" to "exampleGatewayMerchantId")))
    }

    private val cardPaymentMethod = JSONObject().apply {
        put("type", "CARD")
        put("tokenizationSpecification", tokenizationSpecification)
        put("parameters", JSONObject().apply {
            put("allowedCardNetworks", JSONArray(listOf("VISA", "MASTERCARD")))
            put("allowedAuthMethods", JSONArray(listOf("PAN_ONLY", "CRYPTOGRAM_3DS")))
            put("billingAddressRequired", true)
            put("billingAddressParameters", JSONObject(mapOf("format" to "FULL")))
        })
    }*/

    /*private val transactionInfo = JSONObject().apply {
        put("totalPrice", "123.45")
        put("totalPriceStatus", "FINAL")
        put("currencyCode", "USD")
    }

    private val merchantInfo = JSONObject().apply {
        put("merchantName", "Example Merchant")
        put("merchantId", "01234567890123456789")
    }

    private val paymentDataRequestJson = JSONObject(googlePayBaseConfiguration.toString()).apply {
        put("allowedPaymentMethods", JSONArray().put(cardPaymentMethod))
        put("transactionInfo", transactionInfo)
        put("merchantInfo", merchantInfo)
    }*/


    private fun clickEvent() {
        binding.tvBack.setOnClickListener {
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
                           // .setOfferToken(selectedOfferToken)
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
            prefs.deleteToken
            prefs.isLogin = false
            val intent = Intent(this, LoginNewActivity::class.java)
            startActivity(intent)
            finish()
            OneSignal.logout();

        }
        //ButtonConstants.ButtonType = SUBSCRIBE
    }


    private fun acknowledgePurchase(purchaseToken: String) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val debugMessage = billingResult.debugMessage
                println("debugMessa:"+debugMessage)
                showToast(this,"Item Purchased")
                isPurchased=true
            }
        }
    }

   /* override fun onPurchaseHistoryResponse(
        billingResult: BillingResult?,
        purchaseHistoryList: MutableList<PurchaseHistoryRecord>?
    ) {


    }*/

    override fun onPurchasesUpdated(billingResult: BillingResult, purchaseList: MutableList<Purchase>?) {
        if (billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
            for (purchase in purchaseList!!) {
                acknowledgePurchase(purchase.purchaseToken)
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
        if (billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
            if (!purchaseHistoryList.isNullOrEmpty()) {
                for (purchase in purchaseHistoryList) {
                    println("purchaseList:"+purchase)
                    /* for (books in booksList) {
                         if (purchase.sku == books.id) {
                             books.isPurchased = true
                         }
                     }*/
                }
            }
        }
    }


    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            LOAD_PAYMENT_DATA_REQUEST_CODE -> {
                when (resultCode) {
                    Activity.RESULT_OK ->
                        PaymentData.getFromIntent(data!!)?.let(::handlePaymentSuccess)

                    Activity.RESULT_CANCELED -> {
                        // The user cancelled without selecting a payment method.
                    }

                    AutoResolveHelper.RESULT_ERROR -> {
                        AutoResolveHelper.getStatusFromIntent(data)?.let {
                            handleError(it.statusCode)
                        }
                    }
                }
            }
        }
    }*/

    /*private fun handleError(statusCode: Int) {
        println("errorCode:"+statusCode)
    }

    private fun handlePaymentSuccess(paymentData: PaymentData) {
        val paymentMethodToken = paymentData.paymentMethodToken?.token
        println("paymentMethodToken:"+paymentMethodToken)


        // Sample TODO: Use this token to perform a payment through your payment gateway
    }
*/


}