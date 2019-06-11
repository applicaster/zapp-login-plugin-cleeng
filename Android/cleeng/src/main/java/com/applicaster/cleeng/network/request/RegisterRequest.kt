package com.applicaster.cleeng.network.request

data class RegisterRequest(
    var email: String,
    var password: String? = null,
    var facebookId: String? = null,
    var country: String,
    var locale: String,
    var currency: String
)