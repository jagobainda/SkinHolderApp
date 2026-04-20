package dev.jagoba.skinholder.models.steam

import com.google.gson.annotations.SerializedName

data class GamerPayItemInfo(
    @SerializedName("name")
    val name: String = "",
    @SerializedName("price")
    val price: Double = 0.0
)
