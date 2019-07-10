package com.applicaster.cleeng.data

data class User(
    val email: String? = null,
    val password: String? = null,
    val facebookId: String? = null,
    var token: String? = null,
    var userOffers: ArrayList<Offer>? = null,
    var ownedProductIds: HashSet<String> = hashSetOf(),
    var ownedSubscriptions: ArrayList<Subscription>? = null,
    val country: String = "US",
    val locale: String = "en_US",
    val currency: String = "USD"
)