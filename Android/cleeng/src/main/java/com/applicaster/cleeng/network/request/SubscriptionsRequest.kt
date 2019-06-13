package com.applicaster.cleeng.network.request


import com.google.gson.annotations.SerializedName

data class SubscriptionsRequest(
    @SerializedName("byAuthIds")
    val byAuthIds: Int,
    @SerializedName("offers")
    val offers: List<String>,
    @SerializedName("token")
    val token: String
)