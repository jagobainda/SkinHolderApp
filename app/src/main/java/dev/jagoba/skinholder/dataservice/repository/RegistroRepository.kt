package dev.jagoba.skinholder.dataservice.repository

import dev.jagoba.skinholder.dataservice.api.RegistroApiService
import dev.jagoba.skinholder.models.registros.Registro
import javax.inject.Inject

class RegistroRepository @Inject constructor(
    private val api: RegistroApiService
) {

    suspend fun getLastRegistro(): Result<Registro> {
        return try {
            val response = api.getLastRegistro()
            if (response.isSuccessful) {
                Result.success(response.body() ?: Registro())
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRegistros(): Result<List<Registro>> {
        return try {
            val response = api.getRegistros()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRegistro(registro: Registro): Result<Long> {
        return try {
            val response = api.createRegistro(registro)
            if (response.isSuccessful) {
                Result.success(response.body() ?: 0L)
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRegistro(registroId: Long): Result<Unit> {
        return try {
            val response = api.deleteRegistro(registroId)
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
