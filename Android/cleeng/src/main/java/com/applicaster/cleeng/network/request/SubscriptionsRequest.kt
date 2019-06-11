package com.applicaster.cleeng.network.request


import com.google.gson.annotations.SerializedName

data class SubscriptionsRequest(
    @SerializedName("byAuthIds")
    val byAuthIds: List<Int>,
    @SerializedName("offers")
    val offers: List<String>,
    @SerializedName("publisherId")
    val publisherId: String,
    @SerializedName("token")
    val token: String
)