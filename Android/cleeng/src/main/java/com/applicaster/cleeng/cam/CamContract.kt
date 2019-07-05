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

class CamContract(private val cleengService: CleengService) : ICamContract {
    private val TAG = CamContract::class.java.canonicalName

    private var camFlow: CamFlow = CamFlow.EMPTY
    private val currentOffers: HashMap<String, String> = hashMapOf()

    override fun activateRedeemCode(redeemCode: String, callback: RedeemCodeActivationCallback) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPluginConfig() =
        cleengService.getPluginConfigurationParams()

    override fun isPurchaseRequired(entitlements: List<String>) = true //TODO: dummy. add proper handling

    override fun isUserLogged(): Boolean = !cleengService.getUser().token.isNullOrEmpty()

    override fun loadEntitlements(callback: EntitlementsLoadCallback) {
        val requestData = SubscriptionsRequestData(
            1,
            /*cleengService.getUser().userOffers?.map { it.authId.orEmpty() }.orEmpty()*/listOf("48"),
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
                            currentOffers[it.androidProductId] = it.id
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
                    if (!responseDataResult.isNullOrEmpty())
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
                    if (!responseDataResult.isNullOrEmpty())
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
                    cleengService.getUser().currency,
                    cleengService.getUser().locale
                )
            )
            when (result) {
                is Result.Success -> {
                    val responseDataResult: List<AuthResponseData>? = result.value
                    if (!responseDataResult.isNullOrEmpty())
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
                    cleengService.getUser().currency,
                    cleengService.getUser().locale
                )
            )
            when (result) {
                is Result.Success -> {
                    val responseDataResult: List<AuthResponseData>? = result.value
                    if (!responseDataResult.isNullOrEmpty())
                        cleengService.parseAuthResponse(responseDataResult)
                    callback.onSuccess()
                }

                is Result.Failure -> {
                    callback.onFailure(cleengService.getErrorMessage(result.value))
                }
            }
        }
    }

    override fun onItemPurchased(purchase: Purchase) {
        val entry = currentOffers.entries.find {
            purchase.sku == it.key
        }

        val receipt = SubscribeRequestData.Receipt(
            purchase.originalJson,
            purchase.orderId,
            purchase.packageName,
            purchase.sku,
            (-1).toString(), // stub
            purchase.purchaseTime.toString(),
            purchase.purchaseToken
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
                    finishPurchaseFlow(entry?.value.orEmpty())
                }

                is Result.Failure -> {
                    Log.e(TAG, result.value?.name)
                }
            }
        }
    }

    private fun finishPurchaseFlow(offerId: String) {
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
                    }
                }

                is Result.Failure -> {
//                    val error: Error? = result.value
                    Log.e(TAG, result.value.toString())
                }
            }
        }
    }

    override fun onPurchasesRestored(callback: RestoreCallback) {
        callback.onSuccess(listOf())
    }

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
                        callback.onFailure("Error") //TODO: replace with separated error

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