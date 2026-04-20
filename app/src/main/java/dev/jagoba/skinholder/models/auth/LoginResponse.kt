package dev.jagoba.skinholder.models.auth

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    val token: String,
    @SerializedName("username")
    val userName: String,
    val userId: Int
)