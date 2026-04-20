package dev.jagoba.skinholder.models.items

import com.google.gson.annotations.SerializedName

data class UserItem(
    @SerializedName("useritemid")
    val userItemId: Long = 0,
    @SerializedName("cantidad")
    val cantidad: Int = 0,
    @SerializedName("itemid")
    val itemId: Int = 0,
    @SerializedName("userid")
    val userId: Int = 0,
    @SerializedName("itemName")
    val itemName: String = "",
    @SerializedName("steamHashName")
    val steamHashName: String = "",
    @SerializedName("gamerPayName")
    val gamerPayName: String = "",
    @SerializedName("csFloatMarketHashName")
    val csFloatMarketHashName: String = ""
)
