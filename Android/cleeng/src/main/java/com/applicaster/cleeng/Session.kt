package com.applicaster.cleeng

import com.applicaster.cleeng.data.Offer
import com.applicaster.cleeng.data.User
import com.applicaster.cleeng.network.error.WebServiceError
import com.applicaster.cleeng.network.response.SubscriptionsResponseData
import com.applicaster.cleeng.utils.isNullOrEmpty

object Session {

    var user: User? = User()

    /**
     * List of productId available to be bought (in old cleeng implementation named authId)
     */
    val availableProductIds: ArrayList<String> = arrayListOf()

    var pluginConfigurator: PluginConfigurator? = null

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
            user?.ownedProductIds?.add(subscriptionData.authId!!)
        }
    }

    fun isAccessGranted(): Boolean = user?.ownedProductIds?.intersect(availableProductIds)?.isNotEmpty() ?: false

    fun getErrorMessage(webError: WebServiceError?): String {
        return pluginConfigurator?.getCleengErrorMessage(webError ?: WebServiceError.DEFAULT).orEmpty()
    }

    fun drop() {
        user = null
        pluginConfigurator = null
        availableProductIds.clear()
    }
}