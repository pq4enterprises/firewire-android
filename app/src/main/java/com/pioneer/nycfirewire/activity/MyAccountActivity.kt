package com.pioneer.nycfirewire.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.android.billingclient.api.*
import com.bumptech.glide.Glide
import com.google.android.gms.common.util.CollectionUtils.listOf
import com.onesignal.OneSignal
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.ActivityMyAccountBinding
import com.pioneer.nycfirewire.model.payment.PaymentRequest
import com.pioneer.nycfirewire.model.user.response.CommonResponse
import com.pioneer.nycfirewire.model.user.response.UserDetails
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.service.BackgroundAudioService
import com.pioneer.nycfirewire.utils.*
import com.pioneer.nycfirewire.utils.Constants.SUB_PRODUCT_ID
import com.pioneer.nycfirewire.utils.DateUtils.formatToIso8601
import com.pioneer.nycfirewire.utils.DateUtils.getExpiryDate
import com.pioneer.nycfirewire.utils.IntentUtils.FROM_ACCOUNT
import com.pioneer.nycfirewire.utils.IntentUtils.OTHER
import com.pioneer.nycfirewire.utils.IntentUtils.UPDATE_PROFILE
import com.pioneer.nycfirewire.viewModel.FireWireViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class MyAccountActivity : BaseActivity(), PurchasesUpdatedListener, PurchaseHistoryResponseListener {

    private var skuItem: ProductDetails? = null
    private lateinit var binding: ActivityMyAccountBinding
    private var isPurchased = false
    private var userDetails = UserDetails()
    private lateinit var billingClient: BillingClient
    private lateinit var vm: FireWireViewModel

    // ✅ Prevent duplicate backend calls
    private val processedPurchaseTokens = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)

        initExtra()
        clickEvent()
        initUi()

        vm.paymentLiveData.observe(this, Observer {
            updatePayment(it)
        })
    }

    private fun updatePayment(response: Resource<CommonResponse>) {
        when (response.state) {
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                isPurchased = true
                prefs.userRole = Constants.USER_PREMIUM_FREE
                renderSubscriptionState()
                binding.progress.gone()

                Log.d("Billing", "Backend confirmed payment: ${response.data}")
                if (response.data?.code == Constants.CODE_SUCCESS) {
                    Log.d("Billing", "User upgraded to premium successfully")
                    // was userView(): the paywall overlay is gone, so re-render
                    // the premium card in its subscribed state instead
                    renderSubscriptionState()
                }
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                Log.e("Billing", "Backend API error: ${response.message}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        analyticMethod(Constants.MY_ACCOUNT, "MyAccountActivity")

        if (prefs.userImg?.isNotEmpty() == true) {
            Glide.with(this).load(prefs.userImg).into(binding.ivProfile)
            userDetails.img = prefs.userImg
        }

        if (prefs.userFirstName?.isNotEmpty() == true) {
            binding.tvProfileName.text = prefs.userFirstName?.plus(" ").plus(prefs.userLastName)
            binding.tvProfileEmail.text = prefs.userEmail
        }

        if (isPurchased) {
            renderSubscriptionState()
        }
    }

    private fun moveToEditProfile() {
        val intent = Intent(this, UpdateProfileActivity::class.java)
        intent.putExtra(UPDATE_PROFILE, userDetails)
        startActivity(intent)
    }

    private fun initExtra() {
        // The paywall is no longer a separate mode: whether we arrived from a
        // premium gate (FROM_ACCOUNT == OTHER) or from the menu, the profile
        // details load and the premium card decides which side to show.
        userDetails = intent.getParcelableExtra(UPDATE_PROFILE) ?: UserDetails()
        binding.tvProfileName.text = userDetails.firstName.plus(" ").plus(userDetails.lastName)
        binding.tvProfileEmail.text = userDetails.email
        renderSubscriptionState()
    }

    private fun initUi() {
        billingClient = BillingClient.newBuilder(this)
            .enablePendingPurchases()
            .setListener(this)
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("Billing", "Billing setup complete")
                    loadProducts()
                    queryInventoryAsync()
                    checkIfUserAlreadyPurchased()
                } else {
                    Log.w("Billing", "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                showToast(this@MyAccountActivity, "Billing service disconnected. Retrying...")
                billingClient.startConnection(this)
            }
        })
    }

    /**
     * The redesign merges the old separate paywall into this screen, so there is
     * no longer a userView/billingView overlay pair — one premium card renders
     * either the price header (with GET NOW) or the subscribed header (with
     * MANAGE SUBSCRIPTION). Entry from a premium gate (FROM_ACCOUNT == OTHER)
     * now lands on the same screen; the card simply shows the unsubscribed side.
     */
    private fun isPremium(): Boolean {
        val role = prefs.userRole ?: ""
        return isPurchased || (role.isNotEmpty() && role != Constants.USER_BASIC_USER)
    }

    private fun renderSubscriptionState() {
        runOnUiThread {
            if (isPremium()) {
                binding.llPriceHeader.gone()
                binding.btnGetNow.gone()
                binding.llSubscribedHeader.visible()
                binding.btnManageSubscription.visible()
                binding.tvFullAccess.text = "Premium Account"
            } else {
                binding.llSubscribedHeader.gone()
                binding.btnManageSubscription.gone()
                binding.llPriceHeader.visible()
                binding.btnGetNow.visible()
                binding.tvFullAccess.text = "Basic Account"
            }
        }
    }

    private fun updateSubscriptionUI(isActive: Boolean) {
        runOnUiThread {
            if (isActive) {
                isPurchased = true
                Log.d("SubscriptionCheck", "Subscription active")
            } else {
                isPurchased = false
                basedOnUserRoleWithoutPurchase()
                Log.d("SubscriptionCheck", "Subscription inactive")
            }
            renderSubscriptionState()
        }
    }

    private fun checkIfUserAlreadyPurchased() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasActiveSubscription = purchasesList?.any { purchase ->
                    purchase.products.contains(SUB_PRODUCT_ID) &&
                            (purchase.isAutoRenewing || purchase.purchaseTime >= System.currentTimeMillis())
                } ?: false
                updateSubscriptionUI(hasActiveSubscription)
            } else {
                showSnack("Error checking purchases: ${billingResult.debugMessage}")
            }
        }
    }

    private fun basedOnUserRoleWithoutPurchase() {
        runOnUiThread {
            if (prefs.userRole == Constants.USER_PREMIUM_FREE) {
                isPurchased = true
                renderSubscriptionState()
            }
        }
    }

    private fun getProductList(): ArrayList<String> {
        val productIdsList = ArrayList<String>()
        productIdsList.add(SUB_PRODUCT_ID)
        return productIdsList
    }

    private fun loadProducts() {
        billingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.SUBS, this)
    }

    private fun queryInventoryAsync() {
        if (getProductList().isNullOrEmpty()) return

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SUB_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !productDetailsList.isNullOrEmpty()) {
                skuItem = productDetailsList.firstOrNull()
                val price = skuItem?.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.pricingPhases
                    ?.pricingPhaseList
                    ?.firstOrNull()
                    ?.formattedPrice.orEmpty()
                binding.tvPrice.text = price
            } else {
                Log.w("Billing", "Failed to query product details: ${billingResult.debugMessage}")
            }
        }
    }

    private fun clickEvent() {
        // the redesigned screen has a single shared toolbar instead of a
        // separate tvBack (account mode) and ivClose (paywall overlay)
        binding.toolbar.ivMenu.setOnClickListener { finish() }
        binding.toolbar.tvToolbarTitle.text = getString(R.string.my_account)
        binding.toolbar.tvToolbarTitle.visible()

        binding.tvUpdateProfile.setOnClickListener { moveToEditProfile() }
        binding.tvEditProfile.setOnClickListener { moveToEditProfile() }

        binding.tvAreasAlerts.setOnClickListener {
            startActivity(Intent(this, AreasAlertsActivity::class.java))
        }

        binding.btnManageSubscription.setOnClickListener {
            moveToLink("https://play.google.com/store/account/subscriptions?sku=$SUB_PRODUCT_ID&package=$packageName")
        }

        binding.tvDeleteAccount.setOnClickListener {
            binding.tvSignOut.performClick()
        }

        binding.btnGetNow.setOnClickListener {
            if (isPurchased) {
                showToast(this, "Already Purchased")
                return@setOnClickListener
            }

            val sku = skuItem ?: run {
                showToast(this, "Product not loaded yet")
                return@setOnClickListener
            }

            val offerToken = sku.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken.isNullOrEmpty()) {
                showToast(this, "Offer token not available")
                return@setOnClickListener
            }

            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(sku)
                    .setOfferToken(offerToken)
                    .build()
            )

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            binding.btnGetNow.isEnabled = false
            billingClient.launchBillingFlow(this, flowParams)
        }

        binding.tvTermsService.setOnClickListener { moveToLink("https://nycfirewire.net/terms") }
        binding.tvPrivacyPolicy.setOnClickListener { moveToLink("https://nycfirewire.net/privacy") }
        binding.tvSignOut.setOnClickListener { showSignOut(getString(R.string.you_want_to_sign_out)) }
    }

    fun showSignOut(message: String? = "") {
        AlertDialog.Builder(this)
            .setTitle(resources.getString(R.string.app_name))
            .setMessage(message)
            .setCancelable(false)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.sign_out) { _, _ ->
                prefs.deleteToken
                prefs.isLogin = false
                startNewActivity(LoginNewActivity::class.java)
                prefs.userImg = ""
                prefs.userFirstName = ""
                prefs.userLastName = ""
                prefs.userEmail = ""
                prefs.soundName = ""
                BackgroundAudioService.stopService(this)
                prefs.feedMainPosition = -1
                prefs.feedSubPosition = -1
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                prefs.isDarkMode = false
                OneSignal.logout()
            }.show()
    }

    // ✅ Updated acknowledge logic
    private fun acknowledgePurchaseWithRetry(
        billingClient: BillingClient,
        purchase: Purchase,
        maxRetries: Int = 3,
        delayMillis: Long = 2000L
    ) {
        // Already acknowledged → no need to call again
        if (purchase.isAcknowledged) {
            Log.d("Billing", "✅ Purchase already acknowledged: ${purchase.purchaseToken}")
            runOnUiThread { callPaymentBackendApi(purchase) }
            return
        }

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        var attempt = 0

        fun attemptAcknowledge() {
            billingClient.acknowledgePurchase(params) { billingResult ->
                attempt++
                Log.d(
                    "Billing",
                    "Acknowledge attempt $attempt result: ${billingResult.responseCode} (${billingResult.debugMessage})"
                )

                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("Billing", "✅ Acknowledged successfully after $attempt attempt(s)")
                    runOnUiThread { callPaymentBackendApi(purchase) }
                } else {
                    // Retry only for transient server/network issues
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE ||
                        billingResult.responseCode == BillingClient.BillingResponseCode.ERROR ||
                        billingResult.responseCode == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED
                    ) {
                        if (attempt < maxRetries) {
                            Log.w("Billing", "⚠️ Retry acknowledgment in ${delayMillis}ms (attempt $attempt/$maxRetries)")
                            Handler(Looper.getMainLooper()).postDelayed({
                                attemptAcknowledge()
                            }, delayMillis)
                        } else {
                            Log.e("Billing", "❌ Failed to acknowledge after $maxRetries attempts")
                            // Optional: Still call backend if purchase state == PURCHASED
                            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                                Log.w("Billing", "Proceeding to backend since purchase is confirmed but not acknowledged")
                                runOnUiThread { callPaymentBackendApi(purchase) }
                            }
                        }
                    } else {
                        Log.e("Billing", "❌ Permanent error acknowledging: ${billingResult.debugMessage}")
                    }
                }
            }
        }

        // Start first attempt
        attemptAcknowledge()
    }

    /*private fun acknowledgePurchase(purchase: Purchase) {
        val token = purchase.purchaseToken
        if (token.isNullOrEmpty()) {
            Log.w("Billing", "acknowledgePurchase called with empty token")
            return
        }

        if (purchase.isAcknowledged) {
            Log.d("Billing", "Purchase already acknowledged: ${purchase.orderId}")
            if (!processedPurchaseTokens.contains(token)) {
                processedPurchaseTokens.add(token)
                runOnUiThread { callPaymentBackendApi(purchase) }
            }
            return
        }

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(token)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            Log.d("Billing", "Acknowledge result: ${billingResult.responseCode} (${billingResult.debugMessage}) for token=$token")

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processedPurchaseTokens.add(token)
                runOnUiThread { callPaymentBackendApi(purchase) }
            } else {
                Log.w("Billing", "Acknowledge failed: ${billingResult.debugMessage}")
            }
        }
    }*/

    private fun callPaymentBackendApi(purchase: Purchase) {
        try {
            val token = purchase.purchaseToken
            Log.d("Billing", "Preparing backend call for token=$token, orderId=${purchase.orderId}")

            val pricing = skuItem
                ?.subscriptionOfferDetails
                ?.firstOrNull()
                ?.pricingPhases
                ?.pricingPhaseList
                ?.firstOrNull()

            val amountValue = pricing?.priceAmountMicros?.div(1_000_000.0) ?: 0.0

            val paymentRequest = PaymentRequest(
                userId = prefs.userId.orEmpty(),
                paymentMethod = "Play Store",
                paymentToken = token,
                transactionId = purchase.orderId.orEmpty(),
                amount = amountValue.toString(),
                currency = pricing?.priceCurrencyCode.orEmpty(),
                status = "success",
                purchaseDate = formatToIso8601(purchase.purchaseTime),
                expiredDate = getExpiryDate(purchase.purchaseTime),
                type = if (purchase.isAutoRenewing) "Auto-renewable subscription" else "One-time"
            )

            Log.d("Billing", "Sending PaymentRequest → $paymentRequest")
            vm.paymentPost(paymentRequest)
        } catch (e: Exception) {
            Log.e("Billing", "Error while calling backend API", e)
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchaseList: MutableList<Purchase>?) {
        Log.d("Billing", "onPurchasesUpdated → code=${billingResult.responseCode}, msg=${billingResult.debugMessage}, purchases=$purchaseList")

        binding.btnGetNow.isEnabled = true

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchaseList?.forEach { purchase ->
                    val token = purchase.purchaseToken
                    if (token.isNullOrEmpty()) return@forEach
                    if (processedPurchaseTokens.contains(token)) {
                        Log.d("Billing", "Already processed token: $token")
                        return@forEach
                    }

                    Log.d("Billing", "PurchaseState: ${purchase.purchaseState}, acknowledged=${purchase.isAcknowledged}")
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchaseWithRetry(billingClient,purchase)
                        } else {
                            processedPurchaseTokens.add(token)
                            runOnUiThread { callPaymentBackendApi(purchase) }
                        }
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                showToast(this, "You've cancelled the Google Play billing process...")
            }
            else -> {
                showToast(this, "Google Play billing error: ${billingResult.debugMessage}")
            }
        }
    }

    override fun onPurchaseHistoryResponse(
        billingResult: BillingResult,
        purchaseHistoryList: MutableList<PurchaseHistoryRecord>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            if (!purchaseHistoryList.isNullOrEmpty()) {
                val purchase = purchaseHistoryList[0]
                val purchaseTimestamp: Long = purchase.purchaseTime
                getDaysSincePurchase(purchaseTimestamp)
            }
        }
    }

    fun getDaysSincePurchase(purchaseTimestamp: Long): Int {
        val purchaseDate = Date(purchaseTimestamp)
        val calendarPurchase = Calendar.getInstance().apply { time = purchaseDate }
        val calendarToday = Calendar.getInstance()
        val diffInMillis = calendarToday.timeInMillis - calendarPurchase.timeInMillis
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    }
}
