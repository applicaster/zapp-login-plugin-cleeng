package com.applicaster.cleeng.network.request


import com.google.gson.annotations.SerializedName

data class RestoreSubscriptionsRequestData(
    @SerializedName("receipts")
    val receipts: List<PaymentReceipt?>,
    @SerializedName("token")
    val token: String?
)