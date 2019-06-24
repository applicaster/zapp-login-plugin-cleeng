package com.applicaster.cleeng.data

import com.applicaster.cam.params.billing.Offer

data class User(
    val email: String?,
    val password: String?,
    val facebookId: String?,
    var token: String?,
    var userOffers: ArrayList<Offer> = arrayListOf(),
    var ownedSubscriptions: ArrayList<Subscription> = arrayListOf()
)