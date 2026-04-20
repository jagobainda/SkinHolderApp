package dev.jagoba.skinholder.dataservice.repository

import dev.jagoba.skinholder.dataservice.api.ItemsApiService
import dev.jagoba.skinholder.models.items.Item
import javax.inject.Inject

class ItemsRepository @Inject constructor(
    private val api: ItemsApiService
) {

    suspend fun getItems(): Result<List<Item>> {
        return try {
            val response = api.getItems()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
