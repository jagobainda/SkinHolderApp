package dev.jagoba.skinholder.dataservice.api

import dev.jagoba.skinholder.models.Logger
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface LogApiService {

    @POST("Log/AddLog")
    suspend fun addLog(@Body logger: Logger): Response<Unit>
}
