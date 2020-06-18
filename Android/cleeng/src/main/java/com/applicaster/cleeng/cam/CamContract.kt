package com.applicaster.cleeng.cam

import android.util.Log
import com.android.billingclient.api.Purchase
import com.applicaster.authprovider.AuthenticationProviderUtil
import com.applicaster.cam.*
import com.applicaster.cam.analytics.AnalyticsUtil
import com.applicaster.cam.analytics.PurchaseEntityType
import com.applicaster.cam.analytics.PurchaseType
import com.applicaster.cam.params.billing.BillingOffer
import com.applicaster.cam.params.billing.ProductType
import com.applicaster.cleeng.CleengService
import com.applicaster.cleeng.Session
import com.applicaster.cleeng.data.Offer
import com.applicaster.cleeng.network.*
import com.applicaster.cleeng.network.error.WebServiceError
import com.applicaster.cleeng.network.request.*
import com.applicaster.cleeng.network.response.AuthResponseData
import com.applicaster.cleeng.network.response.ResetPasswordResponseData
import com.applicaster.cleeng.network.response.SubscriptionsResponseData
import com.applicaster.cleeng.utils.isNullOrEmpty
import kotlinx.coroutines.*
import org.json.JSONObject
import kotlin.coroutines.CoroutineContext

class CamContract(private val cleengService: CleengService) : ICamContract {
    private val TAG = CamContract::class.java.canonicalName

    //pending offers, androidProductId as key and Cleeng offerID as value
    private val currentOffers: HashMap<String, String> = hashMapOf()

    private val scope by lazy {
        val coroutineContext: CoroutineContext = Dispatchers.Main + Job()
        CoroutineScope(coroutineContext)
    }

    override fun getPluginConfig() = Session.getPluginConfigurationParams()

    override fun isUserLogged(): Boolean = cleengService.getUserToken().isNotEmpty()

    //user activation is not used for Cleeng service
    override fun isUserActivated(): Boolean = true

    override fun loadEntitlements(callback: EntitlementsLoadCallback) {
        val requestData = SubscriptionsRequestData(
                1,
                Session.availableProductIds,
                cleengService.getUserToken()
        )
        executeRequest {
            when (val result = cleengService.networkHelper.requestSubscriptions(requestData)) {
                is Result.Success -> {
                    val responseDataResult: List<SubscriptionsResponseData>? = result.value
                    val billingOfferList: ArrayList<BillingOffer> = arrayListOf()
                    currentOffers.clear()
                    responseDataResult?.forEach {
                        val billingOffer = BillingOffer(
                                it.authId.orEmpty(),
                                it.androidProductId.orEmpty(),
                                if (it.period.isNullOrEmpty()) ProductType.INAPP else ProductType.SUBS
                        )
                        billingOfferList.add(billingOffer)
                        Log.i(TAG, "Billing offer: ${billingOfferList[0].productId}")
                        if (!it.androidProductId.isNullOrEmpty() && !it.id.isNullOrEmpty()) {
                            currentOffers[it.androidProductId!!] = it.id!!
                        }
                    }
                    callback.onSuccess(billingOfferList)

                    // collect analytics data
                    collectPurchaseDataForAnalytics(responseDataResult)
                }

                is Result.Failure -> {
                    callback.onFailure(getErrorMessage(result.value))
                }
            }
        }
    }

    override fun login(authFieldsInput: HashMap<String, String>, callback: LoginCallback) {
        executeRequest {
            val result = cleengService.networkHelper.login(
                    authFieldsInput["email"].orEmpty(),
                    authFieldsInput["password"].orEmpty()
            )
            when (result) {
                is Result.Success -> {
                    val responseDataResult: List<AuthResponseData>? = result.value
                    if (responseDataResult != null && responseDataResult.isNotEmpty())
                        cleengService.parseAuthResponse(responseDataResult)
                    callback.onActionSuccess()
                }

                is Result.Failure -> {
                    callback.onFailure(getErrorMessage(result.value))
                }
            }
        }
    }

    override fun loginWithFacebook(email: String, id: String, callback: FacebookAuthCallback) {
        executeRequest {
            val result = cleengService.networkHelper.loginFacebook(
                    email,
                    id
            )
            when (result) {
                is Result.Success -> {
                    val responseDataResult: List<AuthResponseData>? = result.value
                    if (responseDataResult != null && responseDataResult.isNotEmpty())
                        cleengService.parseAuthResponse(responseDataResult)
                    callback.onFacebookAuthSuccess()
                }

                is Result.Failure -> {
                    callback.onFacebookAuthFailure(getErrorMessage(result.value))
                }
            }
        }
    }

    override fun signUp(authFieldsInput: HashMap<String, String>, callback: SignUpCallback) {
        executeRequest {
            val result = cleengService.networkHelper.register(
                    RegisterRequestData(
                            authFieldsInput["email"].orEmpty(),
                            authFieldsInput["password"].orEmpty(),
                            null,
                            Session.userData.country,
                            Session.userData.locale,
                            Session.userData.currency
                    )
            )
            when (result) {
                is Result.Success -> {
                    val responseDataResult: List<AuthResponseData>? = result.value
                    if (responseDataResult != null && responseDataResult.isNotEmpty())
                        cleengService.parseAuthResponse(responseDataResult)
                    callback.onActionSuccess()
                }

                is Result.Failure -> {
                    callback.onFailure(getErrorMessage(result.value))
                }
            }
        }
    }


    /**
     * Feature is not used for Cleeng service
     */
    override fun sendAuthActivationCode(
        authFieldsInput: HashMap<String, String>,
        callback: SendAuthActivationCodeCallback
    ) {
        callback.onCodeSendingSuccess()
    }

    /**
     * Feature is not used for Cleeng service
     */
    override fun activateAccount(
        authFieldsInput: HashMap<String, String>,
        callback: AccountActivationCallback
    ) {
        callback.onActionSuccess()
    }

    override fun signupWithFacebook(email: String, id: String, callback: FacebookAuthCallback) {
        executeRequest {
            val result = cleengService.networkHelper.registerFacebook(
                    RegisterRequestData(
                            email,
                            null,
                            id,
                            Session.userData.country,
                            Session.userData.locale,
                            Session.userData.currency
                    )
            )
            when (result) {
                is Result.Success -> {
                    val responseDataResult: List<AuthResponseData>? = result.value
                    if (responseDataResult != null && responseDataResult.isNotEmpty())
                        cleengService.parseAuthResponse(responseDataResult)
                    callback.onFacebookAuthSuccess()
                }

                is Result.Failure -> {
                    callback.onFacebookAuthFailure(getErrorMessage(result.value))
                }
            }
        }
    }

    override fun logout(isConfirmedByUser: Boolean) {
        if (isConfirmedByUser) {
            Session.drop()
            cleengService.removeUserToken()
            cleengService.logoutListener?.onResult(isConfirmedByUser)
        } else {
            cleengService.logoutListener?.onResult(isConfirmedByUser)
        }
    }

    override fun onItemPurchased(purchase: List<Purchase>, callback: PurchaseCallback) {
        purchase.forEachIndexed { index, item ->  subscribeOn(item, callback, index == purchase.lastIndex) }
    }

    override fun onPurchasesRestored(purchases: List<Purchase>, callback: RestoreCallback) {
        sendRestoredSubscriptions(purchases, callback)
    }

    /**
     *  Test fun for new restore API. Use it in onPurchasesRestored callback
     */
    private fun sendRestoredSubscriptions(
            purchases: List<Purchase>,
            callback: RestoreCallback
    ) {
        val receipts = arrayListOf<PaymentReceipt>()
        purchases.forEach { purchaseItem ->
            val purchaseState = JSONObject(purchaseItem.originalJson).getDouble("purchaseState").toInt()
            receipts.add(
                    PaymentReceipt(
                            "",
                            purchaseItem.orderId,
                            purchaseItem.packageName,
                            purchaseItem.sku,
                            purchaseState,
                            purchaseItem.purchaseTime.toString(),
                            purchaseItem.purchaseToken
                    )
            )
        }
        val restoreSubsData = RestoreSubscriptionsRequestData(
                receipts,
                cleengService.getUserToken()
        )
        executeRequest {
            val result = cleengService.networkHelper.restoreSubscriptions(restoreSubsData)
            when (result) {
                is Result.Success -> {
                    result.value?.forEachIndexed { index, subscriptionsData ->
                        subscriptionsData.offerId?.let {
                            finishPurchaseFlow(it, callback, index == result.value.lastIndex)
                        }
                    }
                }

                is Result.Failure -> {
                    callback.onFailure("")
                    Log.e(TAG, result.value?.name)
                }
            }
        }
    }

    private fun subscribeOn(purchaseItem: Purchase, callback: ActionCallback, shouldSendCallback: Boolean) {
        val offerEntry = currentOffers.entries.find {
            purchaseItem.sku == it.key
        }

        val purchaseState = JSONObject(purchaseItem.originalJson).getDouble("purchaseState").toInt()

        val receipt = PaymentReceipt(
                "",
                purchaseItem.orderId,
                purchaseItem.packageName,
                purchaseItem.sku,
                purchaseState,
                purchaseItem.purchaseTime.toString(),
                purchaseItem.purchaseToken
        )

        val subscribeRequestData = SubscribeRequestData(
                offerEntry?.value,
                receipt,
                cleengService.getUserToken()
        )

        executeRequest {
            val result = cleengService.networkHelper.subscribe(subscribeRequestData)
            when (result) {
                is Result.Success -> {
                    finishPurchaseFlow(offerEntry!!.value, callback, shouldSendCallback)
                }

                is Result.Failure -> {
                    callback.onFailure("")
                    Log.e(TAG, result.value?.name)
                }
            }
        }
    }

    /**
     * Registering purchases on the Cleeng server and waiting until it will return response with updated
     * offerIDs, authIDs and purchase tokens
     */
    private fun finishPurchaseFlow(purchasedOfferId: String, callback: ActionCallback, shouldSendCallback: Boolean) {
        var registeredOffers: List<AuthResponseData> = arrayListOf()
        scope.launch{
            try {
                repeat(PURCHASE_VERIFICATION_CALL_MAX_NUM) {
                    when (val result = cleengService.networkHelper.extendToken(cleengService.getUserToken())) {
                        is Result.Success -> {
                            result.value?.forEach {
                                if (it.offerId.isNullOrEmpty()) {
                                    it.token?.let { token -> cleengService.saveUserToken(token) }
                                } else if (it.offerId == purchasedOfferId) {
                                    registeredOffers = result.value
                                    scope.cancel()
                                    return@repeat
                                }
                            }
                        }
                    }
                    delay(PURCHASE_VERIFICATION_DELAY_MILLIS)
                }
            } finally {
                saveOwnedUserProducts(registeredOffers, callback, shouldSendCallback)
            }
        }
    }

    private fun saveOwnedUserProducts(registeredOffers: List<AuthResponseData>, callback: ActionCallback, shouldSendCallback: Boolean) {
        if (registeredOffers.isNotEmpty()) {
            Log.d(TAG, "saveOwnedUserProducts with $registeredOffers")
            val offers = arrayListOf<Offer>()
            val ownedProductIds = hashSetOf<String>()
            registeredOffers.forEach { authData ->
                offers.add(Offer(authData.offerId, authData.token, authData.authId))
                ownedProductIds.add(authData.authId.orEmpty())
                //saving token in the applicaster SDK. Later this token will be used by the player
                AuthenticationProviderUtil.addToken(authData.authId, authData.token)
            }

            Session.setUserOffers(offers)
            Session.addOwnedProducts(ownedProductIds)
            if (shouldSendCallback)
                callback.onActionSuccess()
        } else {
            Log.d(TAG, "saveOwnedUserProducts with $registeredOffers")
            callback.onFailure(Session.pluginConfigurator?.getCleengErrorMessage(WebServiceError.DEFAULT).orEmpty())
        }
    }

    override fun isPurchaseRequired(): Boolean = !Session.isAccessGranted()

    override fun resetPassword(authFieldsInput: HashMap<String, String>, callback: PasswordResetCallback) {
        executeRequest {
            val result = cleengService.networkHelper.resetPassword(
                    authFieldsInput["email"].orEmpty()
            )
            when (result) {
                is Result.Success -> {
                    val responseDataResult: ResetPasswordResponseData? = result.value
                    if (responseDataResult?.success == true)
                        callback.onActionSuccess()
                    else
                        callback.onFailure("Error")

                }

                is Result.Failure -> {
                    callback.onFailure(getErrorMessage(result.value))
                }
            }
        }
    }

    /**
     * Feature is not used for Cleeng service
     */
    override fun sendPasswordActivationCode(
        authFieldsInput: HashMap<String, String>,
        callback: SendPasswordActivationCodeCallback
    ) {
        callback.onCodeSendingSuccess()
    }

    /**
     * Feature is not used for Cleeng service
     */
    override fun updatePassword(
        authFieldsInput: HashMap<String, String>,
        callback: PasswordUpdateCallback
    ) {
        callback.onActionSuccess()
    }

    private fun getErrorMessage(webError: WebServiceError?): String {
        return Session.pluginConfigurator?.getCleengErrorMessage(webError
                ?: WebServiceError.DEFAULT).orEmpty()
    }

    override fun getCamFlow(): CamFlow = Session.getCamFlow()

    override fun onCamFinished(success: Boolean) {
        cleengService.startUpHookListener?.onHookFinished()
        if(success) {
            cleengService.screenHookListener?.hookCompleted(mutableMapOf())
        } else {
            cleengService.screenHookListener?.hookFailed(mutableMapOf())
        }

    }

    override fun getAnalyticsDataProvider(): IAnalyticsDataProvider = Session.analyticsDataProvider

    private fun collectPurchaseDataForAnalytics(subscriptionsData: List<SubscriptionsResponseData>?) {
        subscriptionsData?.forEach {
            val purchaseData = PurchaseData(
                    it.title.orEmpty(),
                    it.price?.toString() ?: AnalyticsUtil.KEY_NONE_PROVIDED,
                    it.description.orEmpty(),
                    it.androidProductId.orEmpty(),
                    it.period.orEmpty(),
                    if (it.period.isNullOrEmpty()) PurchaseType.CONSUMABLE else PurchaseType.SUBSCRIPTION,
                    it.freeDays.orEmpty(),
                    if (it.accessToTags?.isNotEmpty() == true)
                        PurchaseEntityType.CATEGORY.value else PurchaseEntityType.VOD_ITEM.value
            )
            Session.analyticsDataProvider.purchaseData.add(purchaseData)
        }
    }

    companion object {
        const val PURCHASE_VERIFICATION_CALL_MAX_NUM = 12
        const val PURCHASE_VERIFICATION_DELAY_MILLIS = 5000L
    }
}
