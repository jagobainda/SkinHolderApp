package dev.jagoba.skinholder.dataservice.api

import dev.jagoba.skinholder.models.items.ItemPrecio
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ItemPrecioApiService {

    @GET("ItemPrecio/{registroId}")
    suspend fun getItemPrecios(@Path("registroId") registroId: Long): Response<List<ItemPrecio>>

    @POST("ItemPrecio")
    suspend fun createItemPrecios(@Body itemPrecios: List<ItemPrecio>): Response<Unit>

    @DELETE("ItemPrecio/{registroId}")
    suspend fun deleteItemPrecios(@Path("registroId") registroId: Long): Response<Unit>
}
