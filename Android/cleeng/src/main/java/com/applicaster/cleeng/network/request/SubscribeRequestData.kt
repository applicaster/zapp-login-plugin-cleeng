package com.applicaster.cleeng.network.request


import com.google.gson.annotations.SerializedName

data class SubscribeRequestData(
    @SerializedName("offerId")
    val offerId: String?,
    @SerializedName("receipt")
    val receipt: Receipt?,
    @SerializedName("token")
    val token: String?
) {
    data class Receipt(
        @SerializedName("developerPayload")
        val developerPayload: String?,
        @SerializedName("orderId")
        val orderId: String?,
        @SerializedName("packageName")
        val packageName: String?,
        @SerializedName("productId")
        val productId: String?,
        @SerializedName("purchaseState")
        val purchaseState: Int?,
        @SerializedName("purchaseTime")
        val purchaseTime: String?,
        @SerializedName("purchaseToken")
        val purchaseToken: String?
    )
}