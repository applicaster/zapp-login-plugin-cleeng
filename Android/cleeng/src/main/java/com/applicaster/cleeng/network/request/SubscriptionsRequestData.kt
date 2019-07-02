package com.applicaster.cleeng.network.request

import com.google.gson.annotations.SerializedName

data class SubscriptionsRequestData(
    @SerializedName("byAuthId")
    val byAuthId: Int?,
    @SerializedName("offers")
    val offers: List<String>,
    @SerializedName("token")
    val token: String
)