package dev.jagoba.skinholder.dataservice.repository

import dev.jagoba.skinholder.dataservice.api.ItemPrecioApiService
import dev.jagoba.skinholder.models.items.ItemPrecio
import javax.inject.Inject

class ItemPrecioRepository @Inject constructor(
    private val api: ItemPrecioApiService
) {

    suspend fun getItemPrecios(registroId: Long): Result<List<ItemPrecio>> {
        return try {
            val response = api.getItemPrecios(registroId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createItemPrecios(itemPrecios: List<ItemPrecio>): Result<Unit> {
        return try {
            val response = api.createItemPrecios(itemPrecios)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteItemPrecios(registroId: Long): Result<Unit> {
        return try {
            val response = api.deleteItemPrecios(registroId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
