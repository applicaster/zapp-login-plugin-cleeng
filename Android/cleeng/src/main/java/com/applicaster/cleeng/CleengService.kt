package com.applicaster.cleeng

import android.content.Context
import com.applicaster.authprovider.AuthenticationProviderUtil
import com.applicaster.cam.CamFlow
import com.applicaster.cam.ContentAccessManager
import com.applicaster.cleeng.cam.CamContract
import com.applicaster.cleeng.data.Offer
import com.applicaster.cleeng.network.NetworkHelper
import com.applicaster.cleeng.network.Result
import com.applicaster.cleeng.network.executeRequest
import com.applicaster.cleeng.network.response.AuthResponseData
import com.applicaster.cleeng.utils.SharedPreferencesUtil
import com.applicaster.cleeng.utils.isNullOrEmpty
import com.applicaster.hook_screen.HookScreen
import com.applicaster.hook_screen.HookScreenListener
import com.applicaster.plugin_manager.hook.HookListener
import com.applicaster.plugin_manager.login.LoginContract

class CleengService {

    val networkHelper: NetworkHelper by lazy { NetworkHelper() }
    val itemAccessHandler: ContentAccessHandler by lazy { ContentAccessHandler(this@CleengService) }
    private val camContract: CamContract by lazy { CamContract(this@CleengService) }
    private val preferences: SharedPreferencesUtil by lazy { SharedPreferencesUtil() }

    var startUpHookListener: HookListener? = null
    var screenHookListener: HookScreenListener? = null
    var logoutListener: LoginContract.Callback? = null

    fun mockStart(context: Context) {
        ContentAccessManager.onProcessStarted(camContract, context)
    }

    /**
     * This fun will be called on the application startup. Trying to execute extend token request and decide which [CamFlow]
     * need to be executed
     *
     *                                                                                  +--------+
     *                                                                   yes  --------->|purchase|  yes
     *                                                              ---------/          |needed? |------->Launch CamFlow.STOREFRONT
     *                                                   +-----------------------+      +--------+
     *                                                   |app level entitlements |         ---\
     *                                          -------->|exist in plugin config?|          no -----\
     *                            yes  --------/         +-----------------------+                   ---> X
     *                        +---------------+                      ---------\
     *                        |trigger on app |                          no    --------> X
     * request succeed ------>|launch enabled?|
     *          ------/       +---------------+
     *+-------------------+        no  ----------> X
     *| login user & get  |
     *| owned entitlements|                               +-----------------------+  yes   -----> Launch CamFlow.AUTH_AND_STOREFRONT
     *+-------------------+       yes ------------------> |app level entitlements | ------/
     *          ------\       +---------------+           |exist in plugin config?|
     *  request failed ------>|trigger on app |           +-----------------------+
     *                        |launch enabled?|                                 --------\
     *                        +---------------+                                      no  -------> Launch CamFlow.AUTHENTICATION
     *                           no  ------------>X
     */
    fun handleLaunchWithoutPlayable(context: Context, listener: HookListener?, isUITriggerEnabled: Boolean) {
        startUpHookListener = listener
        executeRequest {
            val result = networkHelper.extendToken(getUserToken())
            when (result) {
                is Result.Success -> {
                    val responseDataResult: List<AuthResponseData>? = result.value
                    parseAuthResponse(responseDataResult.orEmpty())
                    if (isUITriggerEnabled) {
                        val availableProductsIds = mutableListOf<String>()
                        // we should filter product id's in case applicaster sends them as empty strings
                        Session.pluginConfigurator?.getAppLevelEntitlements()?.filterTo(availableProductsIds) {
                            it.isNotEmpty()
                        }
                        // we should add obtained available products to session to check if access was granted
                        // 'cause when we'll call setSessionParams function all available products will be removed
                        // and added to session from data that was passed as parameter
                        Session.availableProductIds.addAll(availableProductsIds)
                        if (!Session.isAccessGranted()) {
                            itemAccessHandler.setSessionParams(false, availableProductsIds)
                            ContentAccessManager.onProcessStarted(camContract, context)
                        } else {
                            startUpHookListener?.onHookFinished()
                        }
                    } else {
                        startUpHookListener?.onHookFinished()
                    }
                }

                is Result.Failure -> {
                    // handle error and open loginEmail or sign up screen
                    if (isUITriggerEnabled) {
                        val appLevelEntitlements = mutableListOf<String>()
                        // we should filter entitlements in case applicaster sends them as empty strings
                        Session.pluginConfigurator?.getAppLevelEntitlements()?.filterTo(appLevelEntitlements) {
                            it.isNotEmpty()
                        }
                        itemAccessHandler.setSessionParams(true, appLevelEntitlements)
                        ContentAccessManager.onProcessStarted(camContract, context)
                    } else {
                        startUpHookListener?.onHookFinished()
                    }
                }
            }
        }
    }

    fun handleLogin(model: Any?, hookScreen: HookScreen, context: Context) {
        screenHookListener = hookScreen.getListener()
        itemAccessHandler.fetchProductData(model)
        if (getUserToken().isEmpty()) {
            ContentAccessManager.onProcessStarted(camContract, context)
        } else {
            if (Session.isAccessGranted())
                screenHookListener?.hookCompleted(mutableMapOf())
            else
                ContentAccessManager.onProcessStarted(camContract, context)
        }
    }

    fun parseAuthResponse(responseDataResult: List<AuthResponseData>) {
        val offers = arrayListOf<Offer>()
        val ownedProductIds = hashSetOf<String>()
        for (authData in responseDataResult) {
            if (authData.offerId.isNullOrEmpty()) { //parse user data
                saveUserToken(authData.token.orEmpty())
            } else {//parse owned offers
                offers.add(Offer(authData.offerId, authData.token, authData.authId))
                ownedProductIds.add(authData.authId.orEmpty())
                //saving token in the applicaster SDK. Later this token will be used by the player
                AuthenticationProviderUtil.addToken(authData.authId, authData.token)
            }
        }
        Session.setUserOffers(offers)
        Session.addOwnedProducts(ownedProductIds)
    }

    fun getUserToken(): String {
        if (Session.userData.token.isNullOrEmpty()) {
            return preferences.getUserToken()
        }
        return Session.userData.token.orEmpty()
    }

    fun saveUserToken(token: String) {
        Session.userData.token = token
        preferences.saveUserToken(token)
    }

    fun removeUserToken() {
        preferences.removeUserToken()
    }

    fun isUserLogged(): Boolean = getUserToken().isNotEmpty()

    fun logout(context: Context, callback: LoginContract.Callback?) {
        this.logoutListener = callback
        ContentAccessManager.onProcessStarted(camContract, context)
    }

}