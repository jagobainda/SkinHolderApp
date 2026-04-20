package dev.jagoba.skinholder.models.items

import com.google.gson.annotations.SerializedName

data class Item(
    @SerializedName("itemId")
    val itemId: Int = 0,
    @SerializedName("nombre")
    val nombre: String = "",
    @SerializedName("hashNameSteam")
    val hashNameSteam: String = "",
    @SerializedName("gamerPayNombre")
    val gamerPayNombre: String = ""
)
