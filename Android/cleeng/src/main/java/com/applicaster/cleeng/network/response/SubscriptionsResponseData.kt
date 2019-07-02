package com.applicaster.cleeng.network.response


import com.google.gson.annotations.SerializedName

data class SubscriptionsResponseData(
    @SerializedName("accessGranted")
    val accessGranted: Boolean?,
    @SerializedName("accessToTags")
    val accessToTags: List<String?>?,
    @SerializedName("active")
    val active: Boolean?,
    @SerializedName("androidProductId")
    val androidProductId: String?,
    @SerializedName("appleProductId")
    val appleProductId: String?,
    @SerializedName("applicableTaxRate")
    val applicableTaxRate: Int?,
    @SerializedName("authId")
    val authId: String?,
    @SerializedName("averageRating")
    val averageRating: Int?,
    @SerializedName("contentType")
    val contentType: Any?,
    @SerializedName("country")
    val country: String?,
    @SerializedName("createdAt")
    val createdAt: Int?,
    @SerializedName("currency")
    val currency: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("expiresAt")
    val expiresAt: Any?,
    @SerializedName("freeDays")
    val freeDays: String?,
    @SerializedName("freePeriods")
    val freePeriods: String?,
    @SerializedName("geoRestrictionCountries")
    val geoRestrictionCountries: List<Any?>?,
    @SerializedName("geoRestrictionEnabled")
    val geoRestrictionEnabled: Boolean?,
    @SerializedName("geoRestrictionType")
    val geoRestrictionType: Any?,
    @SerializedName("id")
    val id: String?,
    @SerializedName("period")
    val period: String?,
    @SerializedName("price")
    val price: Double?,
    @SerializedName("publisherEmail")
    val publisherEmail: String?,
    @SerializedName("socialCommissionRate")
    val socialCommissionRate: Int?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("updatedAt")
    val updatedAt: Int?,
    @SerializedName("url")
    val url: String?
)