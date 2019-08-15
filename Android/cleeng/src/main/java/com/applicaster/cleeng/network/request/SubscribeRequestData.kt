package com.applicaster.cleeng.network.request


import com.google.gson.annotations.SerializedName

data class SubscribeRequestData(
    @SerializedName("offerId")
    val offerId: String?,
    @SerializedName("receipt")
    val receipt: PaymentReceipt?,
    @SerializedName("token")
    val token: String?
)