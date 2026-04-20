package dev.jagoba.skinholder.dataservice.repository

import dev.jagoba.skinholder.dataservice.api.AuthApi
import dev.jagoba.skinholder.models.auth.LoginRequest
import dev.jagoba.skinholder.models.auth.LoginResponse
import java.net.SocketTimeoutException
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val authApi: AuthApi
) {

    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            val response = authApi.login(LoginRequest(username, password))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.token.isNotBlank()) {
                    Result.success(body)
                } else {
                    Result.failure(AuthException("Respuesta inválida del servidor"))
                }
            } else {
                Result.failure(AuthException("Usuario o contraseña incorrectos"))
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(AuthException("Tiempo de espera agotado al iniciar sesión"))
        } catch (e: java.io.IOException) {
            Result.failure(AuthException("Error de conexión con el servidor"))
        } catch (e: Exception) {
            Result.failure(AuthException("Error inesperado durante el login"))
        }
    }

    suspend fun validateToken(): Result<Boolean> {
        return try {
            val response = authApi.validateToken()
            Result.success(response.isSuccessful)
        } catch (_: Exception) {
            Result.success(false)
        }
    }
}

class AuthException(message: String) : Exception(message)
