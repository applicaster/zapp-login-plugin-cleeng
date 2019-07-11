package com.applicaster.cleeng.cam

import android.util.Log
import com.android.billingclient.api.Purchase
import com.applicaster.cam.*
import com.applicaster.cam.params.billing.BillingOffer
import com.applicaster.cam.params.billing.ProductType
import com.applicaster.cleeng.CleengService
import com.applicaster.cleeng.network.Result
import com.applicaster.cleeng.network.executeRequest
import com.applicaster.cleeng.network.request.RegisterRequestData
import com.applicaster.cleeng.network.request.SubscribeRequestData
import com.applicaster.cleeng.network.request.SubscriptionsRequestData
import com.applicaster.cleeng.network.response.AuthResponseData
import com.applicaster.cleeng.network.response.ResetPasswordResponseData
import com.applicaster.cleeng.network.response.SubscriptionsResponseData
import com.applicaster.cleeng.utils.isNullOrEmpty

class CamContract(private val cleengService: CleengService) : ICamContract {
    private val TAG = CamContract::class.java.canonicalName

    private var camFlow: CamFlow = CamFlow.EMPTY
    private val currentOffers: HashMap<String, String> = hashMapOf()

    override fun activateRedeemCode(redeemCode: String, callback: RedeemCodeActivationCallback) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPluginConfig() = cleengService.getPluginConfigurationParams()

    override fun isUserLogged(): Boolean = cleengService.getUserToken().isNotEmpty()

    override fun loadEntitlements(callback: EntitlementsLoadCallback) {
        val requestData = SubscriptionsRequestData(
            1,
            cleengService.getUser().userOffers?.map { it.authId.orEmpty() }.orEmpty(),
            cleengService.getUserToken()
        )
        executeRequest {
            val result = cleengService.networkHelper.requestSubscriptions(requestData)
            when (result) {
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
                        if (!it.androidProductId.isNullOrEmpty() && !it.id.isNullOrEmpty()) {
                            currentOffers[it.androidProductId!!] = it.id!!
                        }
                    }
                    callback.onSuccess(billingOfferList)
                }

                is Result.Failure -> {
                    callback.onFailure(cleengService.getErrorMessage(result.value))
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
                    callback.onSuccess()
                }

                is Result.Failure -> {
                    callback.onFailure(cleengService.getErrorMessage(result.value))
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
                    callback.onSuccess()
                }

                is Result.Failure -> {
                    callback.onFailure(cleengService.getErrorMessage(result.value))
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
                    cleengService.getUser().country,
                    cleengService.getUser().locale,
                    cleengService.getUser().currency
                )
            )
            when (result) {
                is Result.Success -> {
                    val responseDataResult: List<AuthResponseData>? = result.value
                    if (responseDataResult != null && responseDataResult.isNotEmpty())
                        cleengService.parseAuthResponse(responseDataResult)
                    callback.onSuccess()
                }

                is Result.Failure -> {
                    callback.onFailure(cleengService.getErrorMessage(result.value))
                }
            }
        }
    }

    override fun signupWithFacebook(email: String, id: String, callback: FacebookAuthCallback) {
        executeRequest {
            val result = cleengService.networkHelper.registerFacebook(
                RegisterRequestData(
                    email,
                    null,
                    id,
                    cleengService.getUser().country,
                    cleengService.getUser().locale,
                    cleengService.getUser().currency
                )
            )
            when (result) {
                is Result.Success -> {
                    val responseDataResult: List<AuthResponseData>? = result.value
                    if (responseDataResult != null && responseDataResult.isNotEmpty())
                        cleengService.parseAuthResponse(responseDataResult)
                    callback.onSuccess()
                }

                is Result.Failure -> {
                    callback.onFailure(cleengService.getErrorMessage(result.value))
                }
            }
        }
    }

    override fun onItemPurchased(purchase: List<Purchase>, callback: PurchaseCallback) {
        purchase.forEach { subscribeOn(it, callback) }
    }

    override fun onPurchasesRestored(purchases: List<Purchase>, callback: RestoreCallback) {
        purchases.forEach { subscribeOn(it, callback) }
    }

    private fun subscribeOn(purchaseItem: Purchase, callback: ActionCallback) {
        val entry = currentOffers.entries.find {
            purchaseItem.sku == it.key
        }

        val receipt = SubscribeRequestData.Receipt(
            purchaseItem.originalJson,
            purchaseItem.orderId,
            purchaseItem.packageName,
            purchaseItem.sku,
            (-1).toString(), // stub
            purchaseItem.purchaseTime.toString(),
            purchaseItem.purchaseToken
        )

        val subscribeRequestData = SubscribeRequestData(
            null,
            entry?.value,
            receipt,
            cleengService.getUser().token
        )

        executeRequest {
            val result = cleengService.networkHelper.subscribe(subscribeRequestData)
            when (result) {
                is Result.Success -> {
                    finishPurchaseFlow(entry?.value.orEmpty(), callback)
                }

                is Result.Failure -> {
                    callback.onFailure("")
                    Log.e(TAG, result.value?.name)
                }
            }
        }
    }

    private fun finishPurchaseFlow(offerId: String, callback: ActionCallback) {
        val requestData = SubscriptionsRequestData(
            null,
            listOf(offerId),
            cleengService.getUser().token.orEmpty()
        )

        executeRequest {
            val result = cleengService.networkHelper.requestSubscriptions(requestData)
            when (result) {
                is Result.Success -> {
                    val responseDataResult: List<SubscriptionsResponseData>? = result.value
                    responseDataResult?.forEach {
                        cleengService.parseAccessGranted(it)
                        callback.onSuccess()
                    }
                }

                is Result.Failure -> {
                    callback.onFailure("")
                    Log.e(TAG, result.value.toString())
                }
            }
        }
    }

    override fun isPurchaseRequired(): Boolean = !cleengService.isAccessGranted()

    override fun resetPassword(authFieldsInput: HashMap<String, String>, callback: PasswordResetCallback) {
        executeRequest {
            val result = cleengService.networkHelper.resetPassword(
                authFieldsInput["email"].orEmpty()
            )
            when (result) {
                is Result.Success -> {
                    val responseDataResult: ResetPasswordResponseData? = result.value
                    if (responseDataResult?.success == true)
                        callback.onSuccess()
                    else
                        callback.onFailure("Error")

                }

                is Result.Failure -> {
                    callback.onFailure(cleengService.getErrorMessage(result.value))
                }
            }
        }
    }

    override fun isRedeemActivated(): Boolean = false //TODO: dummy. add proper handling

    // TODO: dummy. Uncomment "camFlow" line and remove hardcoded CamFlow enum value
    override fun getCamFlow(): CamFlow = /*camFlow*/CamFlow.AUTH_AND_STOREFRONT

    /**
     * This one sets CAM flow value obtained form playable item after login call.
     * You MUST! call this method BEFORE! [ContentAccessManager.onProcessStarted] method
     */
    fun setCamFlow(camFlow: CamFlow) {
        this.camFlow = camFlow
    }
}
