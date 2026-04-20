package dev.jagoba.skinholder.dataservice.repository

import dev.jagoba.skinholder.core.AuthSessionManager
import dev.jagoba.skinholder.models.dashboard.ConnectionStats
import dev.jagoba.skinholder.models.dashboard.DashboardStats
import dev.jagoba.skinholder.models.dashboard.LastRegistryStats
import dev.jagoba.skinholder.models.dashboard.LatencyStats
import dev.jagoba.skinholder.models.dashboard.VarianceStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

/**
 * Aggregates the four data sources rendered on the Home dashboard:
 *  1. Connection status against the backend API (status + ping + uptime).
 *  2. Last [Registro] totals.
 *  3. External latency (steamcommunity, gamerpay, csfloat) measured with HEAD requests.
 *  4. Variance percentages from the backend (`/Registros/GetVarianceStats`).
 *
 * All four are fetched in parallel, equivalent to the `Promise.all` in
 * `useDashboard.ts` of `SkinHolderWeb`.
 */
@Singleton
class DashboardRepository @Inject constructor(
    private val registroRepository: RegistroRepository,
    private val authSessionManager: AuthSessionManager
) {

    private val pingClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(PING_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(PING_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .callTimeout(PING_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(false)
        .build()

    suspend fun getDashboardStats(): DashboardStats = coroutineScope {
        val lastRegistroDeferred = async { registroRepository.getLastRegistro() }
        val varianceDeferred = async { registroRepository.getVarianceStats() }
        val dbPingDeferred = async { measureDatabaseLatency() }
        val externalLatenciesDeferred = async { measureAllLatencies() }

        val lastRegistroResult = lastRegistroDeferred.await()
        val varianceResult = varianceDeferred.await()
        val dbPing = dbPingDeferred.await()
        val externalLatencies = externalLatenciesDeferred.await()

        val isActive = dbPing != PING_FAILED && dbPing < ACTIVE_THRESHOLD_MS
        val connection = ConnectionStats(
            status = if (isActive) ConnectionStats.STATUS_ACTIVE else ConnectionStats.STATUS_INACTIVE,
            ping = dbPing,
            uptime = calculateUptimeHours()
        )

        val lastRegistry = lastRegistroResult.getOrNull()?.takeIf { it.registroId > 0L }?.let {
            LastRegistryStats(
                totalSteam = it.totalSteam,
                totalGamepay = it.totalGamerPay,
                totalCsfloat = it.totalCsFloat
            )
        }

        val variance = varianceResult.getOrNull() ?: VarianceStats()

        DashboardStats(
            connection = connection,
            lastRegistry = lastRegistry,
            latency = externalLatencies,
            variance = variance
        )
    }

    private fun calculateUptimeHours(): Long {
        val start = authSessionManager.getOrInitAppStartTime()
        val elapsedMs = System.currentTimeMillis() - start
        return Math.round(elapsedMs / (1000.0 * 60.0 * 60.0))
    }

    private suspend fun measureDatabaseLatency(): Long = withContext(Dispatchers.IO) {
        measureGet(BASE_URL)
    }

    private suspend fun measureAllLatencies(): LatencyStats = coroutineScope {
        val steam = async(Dispatchers.IO) { measureHead(URL_STEAM) }
        val gamerpay = async(Dispatchers.IO) { measureHead(URL_GAMERPAY) }
        val csfloat = async(Dispatchers.IO) { measureHead(URL_CSFLOAT) }
        LatencyStats(
            steam = steam.await(),
            gamerpay = gamerpay.await(),
            csfloat = csfloat.await()
        )
    }

    private fun measureHead(url: String): Long = runCatching {
        var ms = 0L
        val request = Request.Builder().url(url).head().build()
        ms = measureTimeMillis {
            pingClient.newCall(request).execute().use { /* ignore body */ }
        }
        ms
    }.getOrDefault(PING_FAILED)

    private fun measureGet(url: String): Long = runCatching {
        var ms = 0L
        val request = Request.Builder().url(url).get().build()
        ms = measureTimeMillis {
            pingClient.newCall(request).execute().use { /* ignore body */ }
        }
        ms
    }.getOrDefault(PING_FAILED)

    companion object {
        private const val BASE_URL = "https://shapi.jagoba.dev/"
        private const val URL_STEAM = "https://steamcommunity.com"
        private const val URL_GAMERPAY = "https://gamerpay.gg"
        private const val URL_CSFLOAT = "https://csfloat.com"

        private const val PING_TIMEOUT_MS = 5_000L
        private const val ACTIVE_THRESHOLD_MS = 5_000L
        const val PING_FAILED = -1L
    }
}
