package dev.jagoba.skinholder.dataservice.repository

import dev.jagoba.skinholder.dataservice.api.UserSettingsApiService
import dev.jagoba.skinholder.models.UserInfoResponse
import dev.jagoba.skinholder.models.auth.ChangePasswordRequest
import dev.jagoba.skinholder.models.auth.DeleteAccountRequest
import javax.inject.Inject

class UserSettingsRepository @Inject constructor(
    private val api: UserSettingsApiService
) {

    suspend fun getUserInfo(): Result<UserInfoResponse> {
        return try {
            val response = api.getUserInfo()
            if (response.isSuccessful) {
                Result.success(response.body() ?: UserInfoResponse())
            } else {
                val errorBody = response.errorBody()?.string() ?: "Error ${response.code()}"
                Result.failure(Exception(errorBody))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<String> {
        return try {
            val response = api.changePassword(ChangePasswordRequest(currentPassword, newPassword))
            if (response.isSuccessful) {
                Result.success(response.body() ?: "Contraseña actualizada correctamente.")
            } else {
                val errorBody = response.errorBody()?.string() ?: "Error ${response.code()}"
                Result.failure(Exception(errorBody))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(currentPassword: String): Result<String> {
        return try {
            val response = api.deleteAccount(DeleteAccountRequest(currentPassword))
            if (response.isSuccessful) {
                Result.success(response.body() ?: "Cuenta eliminada correctamente.")
            } else {
                val errorBody = response.errorBody()?.string() ?: "Error ${response.code()}"
                Result.failure(Exception(errorBody))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
