package com.applicaster.cleeng

import com.applicaster.cam.CamFlow
import com.applicaster.cam.ContentAccessManager
import com.applicaster.cleeng.analytics.AnalyticsDataProvider
import com.applicaster.cleeng.data.Offer
import com.applicaster.cleeng.data.UserData

object Session {

    var userData: UserData = UserData()

    val analyticsDataProvider: AnalyticsDataProvider = AnalyticsDataProvider()

    /**
     * List of productId available to be bought (in old cleeng implementation named authId)
     */
    val availableProductIds: ArrayList<String> = arrayListOf()

    var pluginConfigurator: PluginConfigurator? = null

    var triggerStatus: TriggerStatus = TriggerStatus.NOT_SET

    private var camFlow: CamFlow = CamFlow.EMPTY


    fun setUserOffers(offers: ArrayList<Offer>) {
        userData.userOffers = offers
    }

    fun addOwnedProducts(ownedProductIds: HashSet<String>) {
        userData.ownedProductIds?.addAll(ownedProductIds)
    }

    fun getPluginConfigurationParams() = pluginConfigurator?.getPluginConfig().orEmpty()

    fun isAccessGranted(): Boolean = userData.ownedProductIds?.intersect(availableProductIds)?.isNotEmpty() ?: false

    fun getCamFlow(): CamFlow = camFlow

    /**
     * This one sets CAM flow value obtained form playable item after login call.
     * You MUST! call this method BEFORE! [ContentAccessManager.onProcessStarted] method
     */
    fun setCamFlow(camFlow: CamFlow) {
        this.camFlow = camFlow
    }

    fun drop() {
        userData.dropSessionData()
        analyticsDataProvider.dropAllData()
        availableProductIds.clear()
    }

    enum class TriggerStatus {
        APP_LAUNCH,
        TAP_CELL,
        USER_ACCOUNT_COMPONENT,
        NOT_SET
    }
}