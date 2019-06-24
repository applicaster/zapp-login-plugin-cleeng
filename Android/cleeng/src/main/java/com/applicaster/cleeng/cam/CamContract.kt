package com.applicaster.cleeng.cam

import com.applicaster.cam.*
import com.applicaster.cleeng.CleengService
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
//                    TODO("save token")
//                    cleengService.saveUserToken(context, responseResult?.token.orEmpty())
                    callback.onSuccess()
                }

                is Result.Failure -> {
                    callback.onFailure(result.value?.message().orEmpty())
                }
            }
        }
    }

    override fun loginWithFacebook(email: String, id: String, callback: FacebookAuthCallback) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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

    override fun signUp(authFieldsInput: HashMap<String, String>, callback: SignUpCallback) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun signupWithFacebook(email: String, id: String, callback: FacebookAuthCallback) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}