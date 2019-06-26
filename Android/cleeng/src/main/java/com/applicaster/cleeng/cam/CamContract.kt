package com.applicaster.cleeng.cam

import com.applicaster.cam.*
import com.applicaster.cleeng.CleengService
import com.applicaster.cleeng.data.Offer
import com.applicaster.cleeng.network.Result
import com.applicaster.cleeng.network.executeRequest
import com.applicaster.cleeng.network.handleResponse
import com.applicaster.cleeng.network.request.RegisterRequestData
import com.applicaster.cleeng.network.response.AuthResponseData
import com.applicaster.cleeng.network.response.ResetPasswordResponseData

class CamContract(var cleengService: CleengService) : ICamContract {
    override fun activateRedeemCode(redeemCode: String, callback: RedeemCodeActivationCallback) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPluginConfig(): Map<String, String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isPurchaseRequired(entitlements: List<String>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isUserLogged(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun loadEntitlements(callback: EntitlementsLoadCallback) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun login(authFieldsInput: HashMap<String, String>, callback: LoginCallback) {
        executeRequest {
            val response = cleengService.networkHelper.login(
                authFieldsInput["email"].orEmpty(),
                authFieldsInput["password"].orEmpty()
            )
            when (val result = handleResponse(response)) {
                is Result.Success -> {
                    val responseDataResult: List<AuthResponseData>? = result.value
                    if (!responseDataResult.isNullOrEmpty())
                        parseAuthResponse(responseDataResult)
                    callback.onSuccess()
                }

                is Result.Failure -> {
                    callback.onFailure(result.value?.message().orEmpty())
                }
            }
        }
    }

    override fun loginWithFacebook(email: String, id: String, callback: FacebookAuthCallback) {
        executeRequest {
            val response = cleengService.networkHelper.loginFacebook(
                email,
                id
            )
            when (val result = handleResponse(response)) {
                is Result.Success -> {
                    val responseDataResult: List<AuthResponseData>? = result.value
                    if (!responseDataResult.isNullOrEmpty())
                        parseAuthResponse(responseDataResult)
                    callback.onSuccess()
                }

                is Result.Failure -> {
                    callback.onFailure(result.value?.message().orEmpty())
                }
            }
        }
    }

    override fun signUp(authFieldsInput: HashMap<String, String>, callback: SignUpCallback) {
        executeRequest {
            val response = cleengService.networkHelper.register(
                RegisterRequestData(
                    authFieldsInput["email"].orEmpty(),
                    authFieldsInput["password"].orEmpty(),
                    null,
                    cleengService.getUser().country,
                    cleengService.getUser().currency,
                    cleengService.getUser().locale
                )
            )
            when (val result = handleResponse(response)) {
                is Result.Success -> {
                    val responseDataResult: List<AuthResponseData>? = result.value
                    if (!responseDataResult.isNullOrEmpty())
                        parseAuthResponse(responseDataResult)
                    callback.onSuccess()
                }

                is Result.Failure -> {
                    callback.onFailure(result.value?.message().orEmpty())
                }
            }
        }
    }

    override fun signupWithFacebook(email: String, id: String, callback: FacebookAuthCallback) {
        executeRequest {
            val response = cleengService.networkHelper.register(
                RegisterRequestData(
                    email,
                    null,
                    id,
                    cleengService.getUser().country,
                    cleengService.getUser().currency,
                    cleengService.getUser().locale
                )
            )
            when (val result = handleResponse(response)) {
                is Result.Success -> {
                    val responseDataResult: List<AuthResponseData>? = result.value
                    if (!responseDataResult.isNullOrEmpty())
                        parseAuthResponse(responseDataResult)
                    callback.onSuccess()
                }

                is Result.Failure -> {
                    callback.onFailure(result.value?.message().orEmpty())
                }
            }
        }
    }

    private fun parseAuthResponse(responseDataResult: List<AuthResponseData>) {
        val offers = arrayListOf<Offer>()
        for (authData in responseDataResult) {
            if (authData.offerId.isNullOrEmpty()) { //parse user data
                cleengService.saveUserToken(authData.token.orEmpty())
            } else {//parse owned offers
                offers.add(Offer(authData.offerId, authData.token, authData.authId))
            }
        }
        cleengService.setUserOffers(offers)
    }

    override fun onItemPurchased() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPurchasesRestored(callback: RestoreCallback) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun resetPassword(authFieldsInput: HashMap<String, String>, callback: PasswordResetCallback) {
        executeRequest {
            val response = cleengService.networkHelper.resetPassword(
                authFieldsInput["email"].orEmpty()
            )
            when (val result = handleResponse(response)) {
                is Result.Success -> {
                    val responseDataResult: ResetPasswordResponseData? = result.value
                    if (responseDataResult?.success == true)
                        callback.onSuccess()
                    else
                        callback.onFailure("Error") //TODO: replace with separated error

                }

                is Result.Failure -> {
                    callback.onFailure(result.value?.message().orEmpty())
                }
            }
        }
    }

    override fun isRedeemActivated(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}