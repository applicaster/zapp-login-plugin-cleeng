package com.applicaster.cleeng.network.response


import com.google.gson.annotations.SerializedName

data class ResetPasswordResponse(
    @SerializedName("success")
    val success: Boolean?
)