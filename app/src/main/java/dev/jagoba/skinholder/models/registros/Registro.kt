package dev.jagoba.skinholder.models.registros

import com.google.gson.annotations.SerializedName

data class Registro(
    @SerializedName("registroid")
    val registroId: Long = 0,
    @SerializedName("fechahora")
    val fechaHora: String = "",
    @SerializedName("totalsteam")
    val totalSteam: Double = 0.0,
    @SerializedName("totalgamerpay")
    val totalGamerPay: Double = 0.0,
    @SerializedName("totalcsfloat")
    val totalCsFloat: Double = 0.0,
    @SerializedName("userid")
    val userId: Int = 0
)
