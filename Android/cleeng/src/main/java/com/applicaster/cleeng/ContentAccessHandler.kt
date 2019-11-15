package com.applicaster.cleeng

import com.applicaster.app.APProperties
import com.applicaster.atom.model.APAtomEntry
import com.applicaster.cam.CamFlow
import com.applicaster.cam.ContentAccessManager
import com.applicaster.cam.Trigger
import com.applicaster.cleeng.data.playable.ProductDataProvider
import com.applicaster.cleeng.utils.CleengAsyncTaskListener
import com.applicaster.cleeng.utils.isNullOrEmpty
import com.applicaster.loader.json.APChannelLoader
import com.applicaster.loader.json.APVodItemLoader
import com.applicaster.model.APChannel
import com.applicaster.model.APVodItem
import com.applicaster.plugin_manager.login.LoginContract
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.util.AppData

class ContentAccessHandler(private val cleengService: CleengService) {

    /**
     * Set available product ids relevant to this user session and decide which [CamFlow] we need to be used
     * Available product ids can be obtained in many different ways (from playable / from plugin config)
     */
    fun setSessionParams(isAuthRequired: Boolean, parsedProductIds: List<String>) {
        Session.availableProductIds.clear()
        //check if session started on app launch or tap cell and state of storefront on launch
        if (
            (Session.triggerStatus == Session.TriggerStatus.TAP_CELL)
            || (Session.triggerStatus == Session.TriggerStatus.APP_LAUNCH
                    && Session.pluginConfigurator?.isPresentStorefrontOnLaunch() == true)
        ) {
            Session.availableProductIds.addAll(parsedProductIds)
        }

        // constructing flow based on content data
        val productsOption = Purchase.fromValue(Session.availableProductIds)
        val authOption = Auth.fromValue(isAuthRequired)
        val camFlow = matchAuthFlowValues(authOption to productsOption)
        Session.setCamFlow(camFlow)
    }

    /**
     * Constructing [CamFlow] based on available data: check is auth required and is available products exist
     */
    private fun matchAuthFlowValues(extensionsData: Pair<Auth, Purchase>): CamFlow {
        return when (extensionsData) {
            (Auth.REQUIRED to Purchase.NOT_AVAILABLE) -> {
                if (!cleengService.isUserLogged())
                    return CamFlow.AUTHENTICATION
                CamFlow.EMPTY
            }
            (Auth.REQUIRED to Purchase.AVAILABLE) -> {
                if (!cleengService.isUserLogged())
                    return CamFlow.AUTH_AND_STOREFRONT
                CamFlow.STOREFRONT
            }
            (Auth.NOT_REQUIRED to Purchase.AVAILABLE) -> {
                CamFlow.STOREFRONT
            }
            (Auth.NOT_REQUIRED to Purchase.NOT_AVAILABLE) -> {
                CamFlow.EMPTY
            }
            else -> {
                CamFlow.AUTHENTICATION
            }
        }
    }

    /**
     * parsing data from video item, obtaining information about
     */
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

            //analytics data
            setAnalyticsSessionParams(
                    productDataProvider.getEntityType(),
                    productDataProvider.getEntityName()
            )
        }
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
        val offersList = Session.userData.userOffers
        offersList?.forEach { offer ->
            if (!offer.authId.isNullOrEmpty() && offer.authId == productId)
                return true
        }
        return false
    }

    private fun setAnalyticsSessionParams(entityType: String, entityName: String) {
        Session.analyticsDataProvider.apply {
            this.entityType = entityType
            this.entityName = entityName
        }
    }

    /**
     *  Marker class to show if auth needed to access content
     */
    private enum class Auth {
        REQUIRED,
        NOT_REQUIRED;

        companion object {
            fun fromValue(isAuthRequired: Boolean): Auth {
                return if (isAuthRequired) REQUIRED else NOT_REQUIRED
            }
        }
    }

    /**
     *  Marker class to show if there are locked products which can be purchased
     */
    private enum class Purchase {
        AVAILABLE,
        NOT_AVAILABLE;

        companion object {
            fun fromValue(products: List<String>?): Purchase {
                return if (products == null || products.isEmpty()) NOT_AVAILABLE else AVAILABLE
            }
        }
    }
}