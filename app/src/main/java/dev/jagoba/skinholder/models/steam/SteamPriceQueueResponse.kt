package dev.jagoba.skinholder.models.steam

import com.google.gson.annotations.SerializedName

data class SteamPriceQueueResponse(
    @SerializedName("taskId")
    val taskId: String = "",
    @SerializedName("status")
    val status: String = "",
    @SerializedName("message")
    val message: String = ""
)
