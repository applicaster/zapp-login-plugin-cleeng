package com.applicaster.cleeng.data

data class User(
    val email: String? = null,
    val password: String? = null,
    val facebookId: String? = null,
    var token: String? = null,
    var userOffers: ArrayList<Offer> = arrayListOf(),
    var ownedProductIds: HashSet<String> = hashSetOf(),
    val country: String = "US",
    val locale: String = "en_US",
    val currency: String = "USD"
)