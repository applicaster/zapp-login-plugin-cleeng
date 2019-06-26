package com.applicaster.cleeng.network.response


import com.google.gson.annotations.SerializedName

data class ResetPasswordResponseData(
    @SerializedName("success")
    val success: Boolean?
)