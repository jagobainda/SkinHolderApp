package dev.jagoba.skinholder.dataservice.api

import dev.jagoba.skinholder.models.steam.GamerPayItemInfo
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ExternalApiService {

    @GET("External/GetGamerPayPrices")
    suspend fun getGamerPayPrices(): Response<List<GamerPayItemInfo>>

    @POST("External/GetSteamPrice")
    suspend fun getSteamPrice(@Body steamHashName: String): Response<String>

    @GET("External/GetSteamPriceStatus/{taskId}")
    suspend fun getSteamPriceStatus(@Path("taskId") taskId: String): Response<String>

    @POST("ApiQuery/GetCSFloatPrices")
    suspend fun getCSFloatPrices(@Body itemNames: List<String>): Response<Map<String, Double>>
}
