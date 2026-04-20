package dev.jagoba.skinholder.models.auth

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)
