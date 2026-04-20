package dev.jagoba.skinholder.dataservice.api

import dev.jagoba.skinholder.models.UserInfoResponse
import dev.jagoba.skinholder.models.auth.ChangePasswordRequest
import dev.jagoba.skinholder.models.auth.DeleteAccountRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PUT

interface UserSettingsApiService {

    @GET("UserSettings")
    suspend fun getUserInfo(): Response<UserInfoResponse>

    @PUT("UserSettings/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<String>

    @HTTP(method = "DELETE", path = "UserSettings/account", hasBody = true)
    suspend fun deleteAccount(@Body request: DeleteAccountRequest): Response<String>
}
