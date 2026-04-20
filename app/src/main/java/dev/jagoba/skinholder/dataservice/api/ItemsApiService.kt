package dev.jagoba.skinholder.dataservice.api

import dev.jagoba.skinholder.models.items.Item
import retrofit2.Response
import retrofit2.http.GET

interface ItemsApiService {

    @GET("Items")
    suspend fun getItems(): Response<List<Item>>
}
