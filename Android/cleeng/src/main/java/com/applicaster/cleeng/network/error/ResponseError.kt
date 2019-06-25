package com.applicaster.cleeng.network.error

import com.google.gson.annotations.SerializedName

data class ResponseError(
    @SerializedName("code")
    val code: String?,
    @SerializedName("message")
    val message: String?
) : Error {
    override fun code(): String = code.orEmpty()

    override fun message(): String = message.orEmpty()
}