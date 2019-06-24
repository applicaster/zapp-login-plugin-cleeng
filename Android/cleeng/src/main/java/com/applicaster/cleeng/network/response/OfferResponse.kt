package com.applicaster.cleeng.network.response


import com.google.gson.annotations.SerializedName

data class OfferResponse(
    @SerializedName("authId")
    val authId: String?,
    @SerializedName("offerId")
    val offerId: String?,
    @SerializedName("token")
    val token: String?
)