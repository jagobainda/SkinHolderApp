package dev.jagoba.skinholder.models.items

import com.google.gson.annotations.SerializedName

data class Item(
    @SerializedName(value = "itemId", alternate = ["itemid", "ItemId"])
    val itemId: Int = 0,
    @SerializedName(value = "nombre", alternate = ["Nombre"])
    val nombre: String? = null,
    @SerializedName(value = "hashNameSteam", alternate = ["HashNameSteam", "hashNamesteam"])
    val hashNameSteam: String? = null,
    @SerializedName(value = "gamerPayNombre", alternate = ["GamerPayNombre"])
    val gamerPayNombre: String? = null
)
