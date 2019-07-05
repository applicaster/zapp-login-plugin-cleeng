package com.applicaster.cleeng.network.request

import com.google.gson.annotations.SerializedName

data class RegisterRequestData(
    @SerializedName("email")
    var email: String,
    @SerializedName("password")
    var password: String? = null,
    @SerializedName("facebookId")
    var facebookId: String? = null,
    @SerializedName("country")
    var country: String,
    @SerializedName("locale")
    var locale: String,
    @SerializedName("currency")
    var currency: String
)