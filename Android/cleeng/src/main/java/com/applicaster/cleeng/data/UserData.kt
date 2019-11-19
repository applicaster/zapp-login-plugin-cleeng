package com.applicaster.cleeng.data

data class UserData(
    var token: String? = null,
    var userOffers: ArrayList<Offer> = arrayListOf(),
    var ownedProductIds: HashSet<String> = hashSetOf(),
    val country: String = "US",
    val locale: String = "en_US",
    val currency: String = "USD"
) {
    fun dropSessionData() {
        token = null
        userOffers.clear()
        ownedProductIds.clear()
    }
}