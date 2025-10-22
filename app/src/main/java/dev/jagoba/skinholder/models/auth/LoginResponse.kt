package dev.jagoba.skinholder.models.auth

data class LoginResponse(
    val token: String,
    val userName: String,
    val userId: Int
)