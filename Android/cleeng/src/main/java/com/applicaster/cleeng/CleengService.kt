package com.applicaster.cleeng

import android.content.Context
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
import com.applicaster.plugin_manager.hook.HookListener

class CleengService {

    val networkHelper: NetworkHelper by lazy { NetworkHelper() }
    val itemAccessHandler: ContentAccessHandler by lazy { ContentAccessHandler(this@CleengService) }
    private val camContract: CamContract by lazy { CamContract(this@CleengService) }
    private val preferences: SharedPreferencesUtil by lazy { SharedPreferencesUtil() }

    var startUpHookListener: HookListener? = null

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
    fun handleStartupHook(context: Context, listener: HookListener?) {
        startUpHookListener = listener
        executeRequest {
            val result = networkHelper.extendToken(getUserToken())
            when (result) {
                is Result.Success -> {
                    val responseDataResult: List<AuthResponseData>? = result.value
                    parseAuthResponse(responseDataResult.orEmpty())
                    if (Session.pluginConfigurator?.isTriggerOnAppLaunch() == true) {
                        Session.availableProductIds.addAll(Session.pluginConfigurator?.getAppLevelEntitlements().orEmpty())
                        if (!Session.isAccessGranted()) {
                            itemAccessHandler.setSessionParams(false, Session.availableProductIds)
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
                    if (Session.pluginConfigurator?.isTriggerOnAppLaunch() == true) {
                        val appLevelEntitlements = Session.pluginConfigurator?.getAppLevelEntitlements().orEmpty()
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
        itemAccessHandler.fetchProductData(model)
        if (camContract.getCamFlow() != CamFlow.EMPTY) {
            if (getUserToken().isEmpty()) {
                ContentAccessManager.onProcessStarted(camContract, context)
            } else {
                if (Session.isAccessGranted())
                    hookScreen.getListener().hookCompleted(hashMapOf())
                else
                    ContentAccessManager.onProcessStarted(camContract, context)
            }
        } else {
            hookScreen.getListener().hookCompleted(mutableMapOf())
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
            }
        }
        Session.setUserOffers(offers)
        Session.addOwnedProducts(ownedProductIds)
    }

    fun getUserToken(): String {
        if (Session.user?.token.isNullOrEmpty()) {
            return preferences.getUserToken()
        }
        return Session.user?.token.orEmpty()
    }

    private fun saveUserToken(token: String) {
        Session.user?.token = token
        preferences.saveUserToken(token)
    }

    fun isUserLogged(): Boolean = getUserToken().isNotEmpty()

    fun logout() {
        preferences.removeUserToken()
    }

}