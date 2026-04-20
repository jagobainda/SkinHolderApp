package dev.jagoba.skinholder.models

import com.google.gson.annotations.SerializedName

data class Logger(
    @SerializedName("logDescription")
    val logDescription: String,
    @SerializedName("logTypeId")
    val logTypeId: Int,
    @SerializedName("logPlaceId")
    val logPlaceId: Int,
    @SerializedName("userId")
    val userId: Int
)
