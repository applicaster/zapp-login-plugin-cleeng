package com.applicaster.cleeng.cam

import com.applicaster.cam.*

class CamContract: ICamContract {
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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