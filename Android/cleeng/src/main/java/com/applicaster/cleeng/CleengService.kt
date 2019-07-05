package com.applicaster.cleeng

import android.content.Context
import com.applicaster.app.APProperties
import com.applicaster.atom.model.APAtomEntry
import com.applicaster.cam.CamFlow
import com.applicaster.cam.ContentAccessManager
import com.applicaster.cleeng.cam.CamContract
import com.applicaster.cleeng.data.Offer
import com.applicaster.cleeng.data.User
import com.applicaster.cleeng.network.NetworkHelper
import com.applicaster.cleeng.network.Result
import com.applicaster.cleeng.network.error.WebServiceError
import com.applicaster.cleeng.network.executeRequest
import com.applicaster.cleeng.network.response.AuthResponseData
import com.applicaster.cleeng.network.response.SubscriptionsResponseData
import com.applicaster.cleeng.utils.CleengAsyncTaskListener
import com.applicaster.cleeng.utils.SharedPreferencesUtil
import com.applicaster.loader.json.APChannelLoader
import com.applicaster.loader.json.APVodItemLoader
import com.applicaster.model.APChannel
import com.applicaster.model.APModel
import com.applicaster.model.APVodItem
import com.applicaster.plugin_manager.hook.HookListener
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.util.AppData

class CleengService {

    val networkHelper: NetworkHelper by lazy { NetworkHelper(pluginConfigurator?.getPublisherId().orEmpty()) }
    val camContract: CamContract by lazy { CamContract(this@CleengService) }

    private var pluginConfigurator: PluginConfigurator? = null
    private val preferences: SharedPreferencesUtil by lazy { SharedPreferencesUtil() }

    private val user: User = User()

    private val productIds: ArrayList<String> = arrayListOf()
    private val accessGrantedItems: HashSet<String> = hashSetOf()

    fun mockStart(context: Context) {
        ContentAccessManager.onProcessStarted(camContract, context)
    }

    fun handleStartupHook(context: Context, listener: HookListener?) {
        executeRequest {
            val result = networkHelper.extendToken(getUserToken())
            when (result) {
                is Result.Success -> {
                    val responseDataResult: List<AuthResponseData>? = result.value
                    parseAuthResponse(responseDataResult.orEmpty())
                    listener?.onHookFinished()
                }

                is Result.Failure -> {
                    // handle error and open loginEmail or sign up screen
                    if (pluginConfigurator?.isTriggerOnAppLaunch() == true)
                        ContentAccessManager.onProcessStarted(camContract, context)
                }
            }
        }
    }

    fun parseAuthResponse(responseDataResult: List<AuthResponseData>) {
        val offers = arrayListOf<Offer>()
        for (authData in responseDataResult) {
            if (authData.offerId.isNullOrEmpty()) { //parse user data
                saveUserToken(authData.token.orEmpty())
            } else {//parse owned offers
                offers.add(Offer(authData.offerId, authData.token, authData.authId))
                productIds.clear()
                productIds.add(authData.authId.orEmpty())
            }
        }
        setUserOffers(offers)
    }

    fun isItemLocked(model: Any?, loginContractCallback: (Boolean) -> Unit) {

        when (model) {
            is APChannel -> {
                var itemChannelLoader: APChannelLoader? = null
                itemChannelLoader = APChannelLoader(
                    object : CleengAsyncTaskListener<APChannel>() {
                        override fun onComplete(result: APChannel) {
                            loginContractCallback(isItemLocked(itemChannelLoader?.bean))
                        }

                        override fun onError() {
                            loginContractCallback(false)
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
                            loginContractCallback(isItemLocked(vodItemLoader?.bean))
                        }

                        override fun onError() {
                            loginContractCallback(false)
                        }
                    },
                    model,
                    AppData.getProperty(APProperties.ACCOUNT_ID_KEY),
                    AppData.getProperty(APProperties.BROADCASTER_ID_KEY)
                )
                vodItemLoader.loadBean()
            }

            else -> isItemLocked(model)
        }
    }

    fun isItemLocked(model: Any?): Boolean {
        if (model is Playable) {
            fetchFeedData(model)
            productIds.forEach { productId ->
                if (isUserOffersComply(productId))
                    return false
            }
        }
        return true
    }

    private fun isUserOffersComply(productId: String): Boolean {
        val offersList = getUser().userOffers
        offersList?.forEach { offer ->
            if (offer.authId.isNullOrEmpty() && offer.authId == productId)
                return true
        }
        return false
    }

    fun fetchFeedData(playable: Playable?) {
        val authKey = "requires_authentication"
        val productIDsKey = "ds_product_ids"
        when (playable) {
            is APAtomEntry.APAtomEntryPlayable -> {
                val isAuthRequired: Boolean =
                    playable.entry.getExtension(authKey, Boolean::class.java) ?: false
                productIds.addAll((playable.entry.getExtension(productIDsKey, List::class.java) as? ArrayList<String>).orEmpty())
                val option: Option = if (productIds.isNotEmpty()) Option.SOME else Option.NONE
                camContract.setCamFlow(matchAuthFlowValues(isAuthRequired to option))
            }

            is APChannel -> {
                val isAuthRequired: Boolean = playable.getExtension(authKey).toString().toBoolean()
                productIds.addAll((playable.getExtension(productIDsKey) as? ArrayList<String>).orEmpty())
                val option: Option = if (productIds.isNotEmpty()) Option.SOME else Option.NONE
                camContract.setCamFlow(matchAuthFlowValues(isAuthRequired to option))
            }

            is APModel -> {
                val isAuthRequired: Boolean = playable.getExtension(authKey).toString().toBoolean()
                productIds.addAll((playable.getExtension(productIDsKey) as? ArrayList<String>).orEmpty())
                val option: Option = if (productIds.isNotEmpty()) Option.SOME else Option.NONE
                camContract.setCamFlow(matchAuthFlowValues(isAuthRequired to option))
            }

            is APAtomEntry -> {
                val isAuthRequired: Boolean =
                    playable.getExtension(authKey, Boolean::class.java) ?: false
                productIds.addAll((playable.getExtension(productIDsKey, List::class.java) as? ArrayList<String>).orEmpty())
                val option: Option = if (productIds.isNotEmpty()) Option.SOME else Option.NONE
                camContract.setCamFlow(matchAuthFlowValues(isAuthRequired to option))
            }
        }
    }

    private fun matchAuthFlowValues(extensionsData: Pair<Boolean, Option>): CamFlow {
        return when (extensionsData) {
            (true to Option.NONE) -> {
                if (!isUserLogged())
                    CamFlow.AUTHENTICATION
                CamFlow.EMPTY
            }
            (true to Option.SOME) -> {
                if (!isUserLogged())
                    CamFlow.AUTH_AND_STOREFRONT
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

    fun getUser() = user

    fun saveUserToken(token: String) {
        user.token = token
        preferences.saveUserToken(token)
    }

    fun getUserToken(): String {
        if (user.token.isNullOrEmpty()) {
            return preferences.getUserToken()
        }
        return user.token.orEmpty()
    }

    fun isUserLogged(): Boolean = getUserToken().isNotEmpty()

    fun logout() {
        preferences.removeUserToken()
    }

    fun setUserOffers(offers: ArrayList<Offer>) {
        user.userOffers = offers
    }

    fun setPluginConfigurator(pluginConfigurator: PluginConfigurator) {
        this@CleengService.pluginConfigurator = pluginConfigurator
    }

    fun getPluginConfigurationParams() = pluginConfigurator?.getPluginConfig().orEmpty()

    fun parseAccessGranted(subscriptionData: SubscriptionsResponseData) {
        accessGrantedItems.clear()
//        save current authId if access granted
       if (subscriptionData.accessGranted == true && !subscriptionData.authId.isNullOrEmpty()) {
           accessGrantedItems.add(subscriptionData.authId)
       }
    }

    fun isAccessGranted(): Boolean = accessGrantedItems.intersect(productIds).isNotEmpty()

    fun getErrorMessage(webError: WebServiceError?): String {
        return pluginConfigurator?.getCleengErrorMessage(webError ?: WebServiceError.DEFAULT).orEmpty()
    }

    private enum class Option {
        SOME,
        NONE
    }
}