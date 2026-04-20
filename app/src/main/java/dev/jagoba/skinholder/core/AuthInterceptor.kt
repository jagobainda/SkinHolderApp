package dev.jagoba.skinholder.core

import okhttp3.Interceptor
import okhttp3.Response
import java.net.HttpURLConnection
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val authSessionManager: AuthSessionManager,
    private val sessionExpiredNotifier: SessionExpiredNotifier
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = authSessionManager.getToken()

        val outgoingRequest = if (!token.isNullOrEmpty()) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        val response = chain.proceed(outgoingRequest)

        if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED
            && outgoingRequest.header("Authorization") != null
        ) {
            authSessionManager.clearSession()
            sessionExpiredNotifier.notifySessionExpired()
            response.close()
            throw SessionExpiredException()
        }

        return response
    }
}
