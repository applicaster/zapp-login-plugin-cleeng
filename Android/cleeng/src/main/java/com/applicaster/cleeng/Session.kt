package com.applicaster.cleeng

import com.applicaster.cam.CamFlow
import com.applicaster.cam.ContentAccessManager
import com.applicaster.cleeng.analytics.AnalyticsDataProvider
import com.applicaster.cleeng.data.Offer
import com.applicaster.cleeng.data.User
import com.applicaster.cleeng.network.response.SubscriptionsResponseData
import com.applicaster.cleeng.utils.isNullOrEmpty

object Session {

    var user: User? = User()

    val analyticsDataProvider: AnalyticsDataProvider = AnalyticsDataProvider()

    /**
     * List of productId available to be bought (in old cleeng implementation named authId)
     */
    val availableProductIds: ArrayList<String> = arrayListOf()

    var pluginConfigurator: PluginConfigurator? = null

    var triggerStatus: TriggerStatus = TriggerStatus.NOT_SET

    private var camFlow: CamFlow = CamFlow.EMPTY


    fun setUserOffers(offers: ArrayList<Offer>) {
        user?.userOffers = offers
    }

    fun addOwnedProducts(ownedProductIds: HashSet<String>) {
        user?.ownedProductIds?.addAll(ownedProductIds)
    }

    fun getPluginConfigurationParams() = pluginConfigurator?.getPluginConfig().orEmpty()

    fun parseAccessGranted(subscriptionData: SubscriptionsResponseData) {
//        save current authId if access granted
        if (subscriptionData.accessGranted == true && !subscriptionData.authId.isNullOrEmpty()) {
            subscriptionData.authId?.let { user?.ownedProductIds?.add(it) }
        }
    }

    fun isAccessGranted(): Boolean = user?.ownedProductIds?.intersect(availableProductIds)?.isNotEmpty() ?: false


    fun getCamFlow(): CamFlow = camFlow

    /**
     * This one sets CAM flow value obtained form playable item after login call.
     * You MUST! call this method BEFORE! [ContentAccessManager.onProcessStarted] method
     */
    fun setCamFlow(camFlow: CamFlow) {
        this.camFlow = camFlow
    }

    fun drop() {
        user = null
        pluginConfigurator = null
        analyticsDataProvider.dropAllData()
        availableProductIds.clear()
    }

    enum class TriggerStatus {
        APP_LAUNCH,
        TAP_CELL,
        NOT_SET
    }
}