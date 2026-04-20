package dev.jagoba.skinholder.dataservice.api

import dev.jagoba.skinholder.models.items.UserItem
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface UserItemApiService {

    @GET("UserItems")
    suspend fun getUserItems(): Response<List<UserItem>>

    @POST("UserItems")
    suspend fun addUserItem(@Body userItem: UserItem): Response<Unit>

    @PUT("UserItems")
    suspend fun updateUserItem(@Body userItem: UserItem): Response<Unit>
}
