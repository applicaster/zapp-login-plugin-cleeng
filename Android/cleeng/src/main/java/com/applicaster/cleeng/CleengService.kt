package com.applicaster.cleeng

import android.content.Context
import com.applicaster.app.APProperties
import com.applicaster.atom.model.APAtomEntry
import com.applicaster.cam.CamFlow
import com.applicaster.cam.ContentAccessManager
import com.applicaster.cleeng.cam.CamContract
import com.applicaster.cleeng.data.Offer
import com.applicaster.cleeng.data.User
import com.applicaster.cleeng.data.playable.ProductDataProvider
import com.applicaster.cleeng.network.NetworkHelper
import com.applicaster.cleeng.network.Result
import com.applicaster.cleeng.network.error.WebServiceError
import com.applicaster.cleeng.network.executeRequest
import com.applicaster.cleeng.network.response.AuthResponseData
import com.applicaster.cleeng.network.response.SubscriptionsResponseData
import com.applicaster.cleeng.utils.CleengAsyncTaskListener
import com.applicaster.cleeng.utils.SharedPreferencesUtil
import com.applicaster.cleeng.utils.isNullOrEmpty
import com.applicaster.loader.json.APChannelLoader
import com.applicaster.loader.json.APVodItemLoader
import com.applicaster.model.APChannel
import com.applicaster.model.APVodItem
import com.applicaster.plugin_manager.hook.HookListener
import com.applicaster.plugin_manager.login.LoginContract
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.util.AppData

class CleengService {

    val networkHelper: NetworkHelper by lazy { NetworkHelper() }
    val camContract: CamContract by lazy { CamContract(this@CleengService) }

    private val preferences: SharedPreferencesUtil by lazy { SharedPreferencesUtil() }

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
     *                        +---------------+                                      no  -------> Launch CAMFlow.AUTHENTICATION
     *                           no  ------------>X
     */
    fun handleStartupHook(context: Context, listener: HookListener?) {
        executeRequest {
            val result = networkHelper.extendToken(getUserToken())
            when (result) {
                is Result.Success -> {
                    val responseDataResult: List<AuthResponseData>? = result.value
                    parseAuthResponse(responseDataResult.orEmpty())
                    if (Session.pluginConfigurator?.isTriggerOnAppLaunch() == true) {
                        Session.availableProductIds.addAll(Session.pluginConfigurator?.getAppLevelEntitlements().orEmpty())
                        if (!Session.isAccessGranted()) {
                            setSessionParams(false, Session.availableProductIds)
                            ContentAccessManager.onProcessStarted(camContract, context)
                        } else {
                            listener?.onHookFinished()
                        }
                    } else {
                        listener?.onHookFinished()
                    }
                }

                is Result.Failure -> {
                    // handle error and open loginEmail or sign up screen
                    if (Session.pluginConfigurator?.isTriggerOnAppLaunch() == true) {
                        val appLevelEntitlements = Session.pluginConfigurator?.getAppLevelEntitlements().orEmpty()
                        setSessionParams(true, appLevelEntitlements)
                        ContentAccessManager.onProcessStarted(camContract, context)
                    } else {
                        listener?.onHookFinished()
                    }
                }
            }
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

    fun checkItemLocked(model: Any?, callback: LoginContract.Callback?) {

        when (model) {
            is APChannel -> {
                var itemChannelLoader: APChannelLoader? = null
                itemChannelLoader = APChannelLoader(
                    object : CleengAsyncTaskListener<APChannel>() {
                        override fun onComplete(result: APChannel) {
                            callback?.onResult(isItemLocked(itemChannelLoader?.bean))
                        }

                        override fun onError() {
                            callback?.onResult(false)
                        }
                    },
                    model.id,
                    AppData.getProperty(APProperties.ACCOUNT_ID_KEY),
                    AppData.getProperty(APProperties.BROADCASTER_ID_KEY)
                )
                itemChannelLoader.loadBean()
            }

            is String -> {
                var vodItemLoader: APVodItemLoader? = null
                vodItemLoader = APVodItemLoader(
                    object : CleengAsyncTaskListener<APVodItem>() {
                        override fun onComplete(result: APVodItem) {
                            callback?.onResult(isItemLocked(vodItemLoader?.bean))
                        }

                        override fun onError() {
                            callback?.onResult(false)
                        }
                    },
                    model,
                    AppData.getProperty(APProperties.ACCOUNT_ID_KEY),
                    AppData.getProperty(APProperties.BROADCASTER_ID_KEY)
                )
                vodItemLoader.loadBean()
            }

            else -> isItemLocked(model, callback)
        }
    }

    fun isItemLocked(model: Any?, callback: LoginContract.Callback? = null): Boolean {
        when (model) {
            is Playable, is APAtomEntry -> {
                fetchProductData(model)
                if (Session.availableProductIds.isEmpty()) {
                    callback?.onResult(false)
                    return false
                } else {
                    Session.availableProductIds.forEach { productId ->
                        if (isUserOffersComply(productId)) {
                            callback?.onResult(false)
                            return false
                        }
                    }
                }
            }
        }
        callback?.onResult(true)
        return true
    }

    private fun isUserOffersComply(productId: String): Boolean {
        val offersList = Session.user?.userOffers
        offersList?.forEach { offer ->
            if (!offer.authId.isNullOrEmpty() && offer.authId == productId)
                return true
        }
        return false
    }

    fun fetchProductData(playableData: Any?) {
        val productDataProvider = ProductDataProvider.fromPlayable(playableData)
        if (productDataProvider != null) {
            val legacyAuthProviderIds = productDataProvider.getLegacyProviderIds()
            if (legacyAuthProviderIds == null) {
                //obtain data from DSP
                val isAuthRequired: Boolean = productDataProvider.isAuthRequired()
                val productIds = productDataProvider.getProductIds()
                setSessionParams(isAuthRequired, productIds.orEmpty())
            } else {
                // if legacyAuthProviderIds is not empty we should init CamFlow.AUTH_AND_STOREFRONT
                // otherwise CamFlow.EMPTY
                setSessionParams(legacyAuthProviderIds.isNotEmpty(), legacyAuthProviderIds)
            }
        }
    }

    /**
     * Set available product ids relevant to this user session and decide which [CamFlow] we need to be used
     * Available product ids can be obtained in many different ways (from playable / from plugin config)
     */
    private fun setSessionParams(isAuthRequired: Boolean, parsedProductIds: List<String>) {
        Session.availableProductIds.clear()
        Session.availableProductIds.addAll(parsedProductIds)
        val option: Option = if (Session.availableProductIds.isNotEmpty()) Option.SOME else Option.NONE
        camContract.setCamFlow(matchAuthFlowValues(isAuthRequired to option))
    }

    private fun matchAuthFlowValues(extensionsData: Pair<Boolean, Option>): CamFlow {
        return when (extensionsData) {
            (true to Option.NONE) -> {
                if (!isUserLogged())
                    return CamFlow.AUTHENTICATION
                CamFlow.EMPTY
            }
            (true to Option.SOME) -> {
                if (!isUserLogged())
                    return CamFlow.AUTH_AND_STOREFRONT
                CamFlow.STOREFRONT
            }
            (false to Option.SOME) -> {
                CamFlow.STOREFRONT
            }
            (false to Option.NONE) -> {
                CamFlow.EMPTY
            }
            else -> {
                CamFlow.AUTHENTICATION
            }
        }
    }

    fun saveUserToken(token: String) {
        Session.user?.token = token
        preferences.saveUserToken(token)
    }

    fun getUserToken(): String {
        if (Session.user?.token.isNullOrEmpty()) {
            return preferences.getUserToken()
        }
        return Session.user?.token.orEmpty()
    }

    fun isUserLogged(): Boolean = getUserToken().isNotEmpty()

    fun logout() {
        preferences.removeUserToken()
    }

    private enum class Option {
        SOME,
        NONE
    }
}