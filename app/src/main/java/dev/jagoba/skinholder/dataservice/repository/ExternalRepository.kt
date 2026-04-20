package dev.jagoba.skinholder.dataservice.repository

import dev.jagoba.skinholder.dataservice.api.ExternalApiService
import dev.jagoba.skinholder.models.steam.GamerPayItemInfo
import dev.jagoba.skinholder.models.steam.SteamItemInfo
import kotlinx.coroutines.delay
import org.json.JSONObject
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
            if (!queueResponse.isSuccessful) return errorResponse

            val queueBody = queueResponse.body().orEmpty()
            val taskId = runCatching { JSONObject(queueBody).optString("taskId") }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: return errorResponse

            var attempts = 0
            while (attempts < MAX_POLLING_ATTEMPTS) {
                delay(POLLING_INTERVAL_MS)

                val statusResponse = api.getSteamPriceStatus(taskId)
                if (!statusResponse.isSuccessful) return errorResponse

                val body = statusResponse.body().orEmpty()
                val json = runCatching { JSONObject(body) }.getOrNull() ?: return errorResponse
                val status = json.optString("status")

                when (status) {
                    "completed" -> {
                        val price = if (json.isNull("price")) -1.0 else json.optDouble("price", -1.0)
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
