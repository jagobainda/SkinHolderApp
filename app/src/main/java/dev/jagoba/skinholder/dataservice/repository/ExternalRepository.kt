package dev.jagoba.skinholder.dataservice.repository

import dev.jagoba.skinholder.dataservice.api.ExternalApiService
import dev.jagoba.skinholder.models.steam.GamerPayItemInfo
import javax.inject.Inject

class ExternalRepository @Inject constructor(
    private val api: ExternalApiService
) {

    suspend fun getGamerPayPrices(): Result<List<GamerPayItemInfo>> {
        return try {
            val response = api.getGamerPayPrices()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSteamPrice(steamHashName: String): Result<String> {
        return try {
            val response = api.getSteamPrice(steamHashName)
            if (response.isSuccessful) {
                Result.success(response.body() ?: "")
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSteamPriceStatus(taskId: String): Result<String> {
        return try {
            val response = api.getSteamPriceStatus(taskId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: "")
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCSFloatPrices(itemNames: List<String>): Result<Map<String, Double>> {
        return try {
            val response = api.getCSFloatPrices(itemNames)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyMap())
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
