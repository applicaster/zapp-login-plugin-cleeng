package com.applicaster.cleeng

import android.content.Context
import com.applicaster.app.APProperties
import com.applicaster.atom.model.APAtomEntry
import com.applicaster.cam.ContentAccessManager
import com.applicaster.cleeng.cam.CamContract
import com.applicaster.cleeng.data.User
import com.applicaster.cleeng.network.NetworkHelper
import com.applicaster.cleeng.network.Result
import com.applicaster.cleeng.network.executeRequest
import com.applicaster.cleeng.network.handleResponse
import com.applicaster.cleeng.network.response.AuthResponse
import com.applicaster.cleeng.utils.CleengAsyncTaskListener
import com.applicaster.cleeng.utils.SharedPreferencesUtil
import com.applicaster.loader.json.APChannelLoader
import com.applicaster.loader.json.APVodItemLoader
import com.applicaster.model.APChannel
import com.applicaster.model.APModel
import com.applicaster.model.APVodItem
import com.applicaster.plugin_manager.hook.HookListener
import com.applicaster.util.AppData
import kotlin.collections.ArrayList

class CleengService {

    private var publisherId: String = ""
    val networkHelper: NetworkHelper by lazy { NetworkHelper(publisherId) }
    private val camContract: CamContract by lazy { CamContract(this@CleengService) }
    private val preferences: SharedPreferencesUtil by lazy { SharedPreferencesUtil() }

    private val KEY_AUTHORIZATION_PROVIDERS_IDS = "authorization_providers_ids"

    fun handleStartupHook(context: Context, listener: HookListener?) {
        executeRequest {
            val response = networkHelper.extendToken(getUser().token.orEmpty())
            when (val result = handleResponse(response)) {
                is Result.Success -> {
                    val responseResult: AuthResponse? = result.value
                    // save token to prefs?
                    saveUserToken(responseResult?.token.orEmpty())
                    // finish hook
                    listener?.onHookFinished()
                }

                is Result.Failure -> {
                    // handle error and open loginEmail or sign up screen
                    ContentAccessManager.onProcessStarted(camContract, context)
                }
            }
        }
        TODO("Go to the network and get available subscriptions and tokens?")
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
        return when (model) {
            is APModel -> {
                model.authorization_providers_ids?.forEach { providerId ->
                    if (isUserOffersComply(providerId)) return false
                }
                true
            }

            is APAtomEntry -> {
                val providerIds: Array<String> = getAuthorizationProviderIds(model)
                if (providerIds.isEmpty()) return false
                providerIds.forEach { providerId ->
                    if (isUserOffersComply(providerId)) return false
                }
                true
            }
            else -> false
        }
    }

    private fun getAuthorizationProviderIds(model: APAtomEntry): Array<String> {
        val ids: ArrayList<*> = model.getExtension(KEY_AUTHORIZATION_PROVIDERS_IDS, ArrayList::class.java)
        return ids.map { id -> (id as Double).toInt().toString() }.toTypedArray()
    }

    private fun isUserOffersComply(providerId: String): Boolean {
        val offersList = getUser().userOffers
        offersList.forEach { offer ->
            if (offer.productId.isNotEmpty() && offer.productId == providerId)
                return true
        }
        return false
    }

    private fun fetchFeedData(model: Any?): CamFlow {
        val authKey = "requires_authentication"
        val productIDsKey = "ds_product_ids"
        when (model) {
            is APModel -> {
                val isAuthRequired: Boolean = model.getExtension(authKey).toString().toBoolean()
                val productIDs: String? = model.getExtension(productIDsKey)?.toString()
                val option: Option = productIDs?.let { Option.Some } ?: Option.None
                return matchAuthFlowValues(isAuthRequired to option)
            }

            is APAtomEntry -> {
                val isAuthRequired: Boolean = model.getExtension(authKey, Boolean::class.java) ?: false
                val productIDs: String? = model.getExtension(productIDsKey, String::class.java)
                val option: Option = productIDs?.let { Option.Some } ?: Option.None
                return matchAuthFlowValues(isAuthRequired to option)
            }

            is APChannel -> {
                val isAuthRequired: Boolean = model.getExtension(authKey).toString().toBoolean()
                val productIDs: String? = model.getExtension(productIDsKey)?.toString()
                val option: Option = productIDs?.let { Option.Some } ?: Option.None
                return matchAuthFlowValues(isAuthRequired to option)
            }
        }
        return CamFlow.Authentication
    }

    private fun matchAuthFlowValues(extensionsData: Pair<Boolean, Option>): CamFlow {
        return when (extensionsData) {
            (true to Option.None) -> { CamFlow.Authentication }
            (true to Option.Some) -> { CamFlow.AuthAndStorefront }
            (false to Option.Some) -> { CamFlow.Storefront }
            (false to Option.None) -> { CamFlow.Empty }
            else -> { CamFlow.Authentication }
        }
    }

    fun getUser(): User {
        TODO("should be returned from shared preferences?")
    }

    fun saveUserToken(token: String) {
        preferences.saveUserToken(token)
    }

    fun logout() {
        preferences.removeUserToken()
    }

    private enum class Option {
        Some,
        None
    }

    private enum class CamFlow {
        Authentication,
        Storefront,
        AuthAndStorefront,
        Empty
    }
}