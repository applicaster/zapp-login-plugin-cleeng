package com.applicaster.screen_metadata


import com.google.gson.annotations.SerializedName

data class ScreenData(
    @SerializedName("general")
    val general: Any?,

    @SerializedName("type")
    val type: String?
)