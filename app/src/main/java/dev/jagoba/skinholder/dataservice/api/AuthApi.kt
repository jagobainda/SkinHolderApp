package dev.jagoba.skinholder.dataservice.api

import dev.jagoba.skinholder.models.auth.LoginRequest
import dev.jagoba.skinholder.models.auth.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    @POST("Auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("Auth/validate")
    suspend fun validateToken(): Response<Unit>
}
