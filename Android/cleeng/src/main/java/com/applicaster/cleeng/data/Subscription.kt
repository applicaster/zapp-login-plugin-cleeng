package com.applicaster.cleeng.data

data class Subscription(
    val id: String,
    val androidProductId: String,
    val authID: String,
    // optional params
    var title: String? = null,
    var description: String? = null,
    var price: String? = null,
    var type: String? = null
)