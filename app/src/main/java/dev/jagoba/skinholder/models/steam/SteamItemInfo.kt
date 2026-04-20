package dev.jagoba.skinholder.models.steam

data class SteamItemInfo(
    val hashName: String = "",
    val price: Double = 0.0,
    val isError: Boolean = false,
    val isWarning: Boolean = false
)
