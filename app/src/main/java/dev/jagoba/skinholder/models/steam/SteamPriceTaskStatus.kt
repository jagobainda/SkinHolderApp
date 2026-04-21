package dev.jagoba.skinholder.models.steam

import com.google.gson.annotations.SerializedName

data class SteamPriceTaskStatus(
    @SerializedName("status")
    val status: String = "",
    @SerializedName("marketHashName")
    val marketHashName: String? = null,
    @SerializedName("result")
    val result: String? = null,
    @SerializedName("price")
    val price: Double? = null,
    @SerializedName("error")
    val error: String? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("completedAt")
    val completedAt: String? = null
)
