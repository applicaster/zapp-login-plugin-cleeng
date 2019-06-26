package com.applicaster.cleeng.cam

import com.applicaster.cam.*
import com.applicaster.cleeng.CleengService
import com.applicaster.cleeng.data.Offer
import com.applicaster.cleeng.network.Result
import com.applicaster.cleeng.network.executeRequest
import com.applicaster.cleeng.network.handleResponse
import com.applicaster.cleeng.network.response.AuthResponse

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
                    val responseResult: List<AuthResponse>? = result.value
                    if (!responseResult.isNullOrEmpty())
                        parseAuthResponse(responseResult)
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
                    val responseResult: List<AuthResponse>? = result.value
                    if (!responseResult.isNullOrEmpty())
                        parseAuthResponse(responseResult)
                    callback.onSuccess()
                }

                is Result.Failure -> {
                    callback.onFailure(result.value?.message().orEmpty())
                }
            }
        }
    }

    override fun signUp(authFieldsInput: HashMap<String, String>, callback: SignUpCallback) {
//        executeRequest {
//            val response = cleengService.networkHelper.register(
//                authFieldsInput["email"].orEmpty(),
//                authFieldsInput["password"].orEmpty(),
//                cleengService.getUser().country,
//
//
//            )
//            when (val result = handleResponse(response)) {
//                is Result.Success -> {
//                    val responseResult: List<AuthResponse>? = result.value
//                    if (!responseResult.isNullOrEmpty())
//                        parseAuthResponse(responseResult)
//                    callback.onSuccess()
//                }
//
//                is Result.Failure -> {
//                    callback.onFailure(result.value?.message().orEmpty())
//                }
//            }
//        }
    }

    override fun signupWithFacebook(email: String, id: String, callback: FacebookAuthCallback) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun parseAuthResponse(responseResult: List<AuthResponse>) {
        val offers  = arrayListOf<Offer>()
        for (authData in responseResult) {
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}