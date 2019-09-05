package com.applicaster.cleeng.network.response

import com.google.gson.annotations.SerializedName

data class RestoreSubscriptionsResponseData(
    @SerializedName("offerId")
    val offerId: String?,
    @SerializedName("message")
    val message: String?
)