package dev.jagoba.skinholder.dataservice.repository

import dev.jagoba.skinholder.dataservice.api.UserItemApiService
import dev.jagoba.skinholder.models.items.UserItem
import javax.inject.Inject

class UserItemRepository @Inject constructor(
    private val api: UserItemApiService
) {

    suspend fun getUserItems(): Result<List<UserItem>> {
        return try {
            val response = api.getUserItems()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addUserItem(userItem: UserItem): Result<Unit> {
        return try {
            val response = api.addUserItem(userItem)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserItem(userItem: UserItem): Result<Unit> {
        return try {
            val response = api.updateUserItem(userItem)
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
