package dev.jagoba.skinholder.models.items

import com.google.gson.annotations.SerializedName

data class ItemPrecio(
    @SerializedName("itemprecioid")
    val itemPrecioId: Long = 0,
    @SerializedName("preciosteam")
    val precioSteam: Double = 0.0,
    @SerializedName("preciogamerpay")
    val precioGamerPay: Double = 0.0,
    @SerializedName("preciocsfloat")
    val precioCsFloat: Double = 0.0,
    @SerializedName("useritemid")
    val userItemId: Long = 0,
    @SerializedName("registroid")
    val registroId: Long = 0
)
