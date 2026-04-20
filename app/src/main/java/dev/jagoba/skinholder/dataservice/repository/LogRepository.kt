package dev.jagoba.skinholder.dataservice.repository

import dev.jagoba.skinholder.dataservice.api.LogApiService
import dev.jagoba.skinholder.models.Logger
import javax.inject.Inject

class LogRepository @Inject constructor(
    private val api: LogApiService
) {

    suspend fun addLog(logger: Logger): Result<Unit> {
        return try {
            val response = api.addLog(logger)
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
