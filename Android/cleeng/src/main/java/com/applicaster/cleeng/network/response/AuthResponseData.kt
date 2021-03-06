package com.applicaster.cleeng.network.response


import com.google.gson.annotations.SerializedName

data class AuthResponseData(
    @SerializedName("offerId")
    val offerId: String?,
    @SerializedName("token")
    val token: String?,
    @SerializedName("authId")
    val authId: String?
)