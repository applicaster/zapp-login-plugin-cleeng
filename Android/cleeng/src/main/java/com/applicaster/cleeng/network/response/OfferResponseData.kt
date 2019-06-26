package com.applicaster.cleeng.network.response


import com.google.gson.annotations.SerializedName

data class OfferResponseData(
    @SerializedName("authId")
    val authId: String?,
    @SerializedName("offerId")
    val offerId: String?,
    @SerializedName("token")
    val token: String?
)