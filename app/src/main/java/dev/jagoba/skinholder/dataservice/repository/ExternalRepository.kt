package dev.jagoba.skinholder.dataservice.repository

import dev.jagoba.skinholder.dataservice.api.ExternalApiService
import dev.jagoba.skinholder.models.steam.GamerPayItemInfo
import dev.jagoba.skinholder.models.steam.SteamItemInfo
import dev.jagoba.skinholder.models.steam.SteamPriceQueueResponse
import dev.jagoba.skinholder.models.steam.SteamPriceTaskStatus
import kotlinx.coroutines.delay
import javax.inject.Inject

class ExternalRepository @Inject constructor(
    private val api: ExternalApiService
) {

    private companion object {
        const val POLLING_INTERVAL_MS = 500L
        const val MAX_POLLING_ATTEMPTS = 60
    }

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

    suspend fun getSteamPrice(steamHashName: String): Result<SteamPriceQueueResponse> {
        return try {
            val response = api.getSteamPrice(steamHashName)
            if (response.isSuccessful) {
                Result.success(response.body() ?: SteamPriceQueueResponse())
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSteamPriceStatus(taskId: String): Result<SteamPriceTaskStatus> {
        return try {
            val response = api.getSteamPriceStatus(taskId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: SteamPriceTaskStatus())
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

    /**
     * Encola una consulta de precio en Steam y hace polling hasta obtener el resultado.
     * Devuelve un [SteamItemInfo] con `price = -1` y `isError = true` si la consulta falla
     * o expira. Marca `isWarning = true` cuando la consulta termina sin un precio válido
     * pero sin un error explícito.
     */
    suspend fun getSteamPriceWithPolling(steamHashName: String): SteamItemInfo {
        val errorResponse = SteamItemInfo(
            hashName = steamHashName,
            price = -1.0,
            isError = true,
            isWarning = false
        )

        return try {
            val queueResponse = api.getSteamPrice(steamHashName)
            // El controlador devuelve 202 Accepted (no 2xx "OK"), así que isSuccessful es true (200..299).
            if (!queueResponse.isSuccessful) return errorResponse

            val taskId = queueResponse.body()?.taskId?.takeIf { it.isNotBlank() }
                ?: return errorResponse

            var attempts = 0
            while (attempts < MAX_POLLING_ATTEMPTS) {
                delay(POLLING_INTERVAL_MS)

                val statusResponse = api.getSteamPriceStatus(taskId)
                if (!statusResponse.isSuccessful) return errorResponse

                val body = statusResponse.body() ?: return errorResponse

                when (body.status) {
                    "completed" -> {
                        val price = body.price ?: -1.0
                        return SteamItemInfo(
                            hashName = steamHashName,
                            price = price,
                            isError = false,
                            isWarning = price <= 0.0
                        )
                    }
                    "failed" -> return errorResponse
                }

                attempts++
            }
            errorResponse
        } catch (_: Exception) {
            errorResponse
        }
    }
}
