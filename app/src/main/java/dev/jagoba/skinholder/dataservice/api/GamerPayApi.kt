package dev.jagoba.skinholder.dataservice.api

import android.util.Log
import dev.jagoba.skinholder.models.steam.GamerPayItemInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class GamerPayApi @Inject constructor() {

    private companion object {
        const val TAG = "GamerPayApi"
        const val ENDPOINT = "https://api.gamerpay.gg/prices"
        // Real Chrome/Android UA so Cloudflare does not cut the stream short
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .build()

    /**
     * Consulta directamente la API pública de GamerPay (igual que el cliente WPF) y
     * devuelve la lista de items con su precio. Devuelve lista vacía si la respuesta
     * no es válida o se produce un error de red. Usa un OkHttpClient propio sin el
     * AuthInterceptor para no enviar el token Bearer a un servicio externo y evitar
     * que un 401/4xx propague una sesión expirada falsa.
     */
    suspend fun fetchPrices(): List<GamerPayItemInfo> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(ENDPOINT)
            .get()
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Cache-Control", "no-cache")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "GamerPay HTTP ${response.code}: ${response.message}")
                    return@withContext emptyList()
                }

                val body = response.body?.string().orEmpty()
                if (body.isBlank()) {
                    Log.w(TAG, "GamerPay returned empty body")
                    return@withContext emptyList()
                }

                val parsed = parsePrices(body)
                Log.d(TAG, "GamerPay parsed ${parsed.size} items (body=${body.length} bytes)")
                parsed
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching GamerPay prices", e)
            emptyList()
        }
    }

    private fun parsePrices(body: String): List<GamerPayItemInfo> {
        return try {
            val array = JSONArray(body)
            val result = ArrayList<GamerPayItemInfo>(array.length())
            for (i in 0 until array.length()) {
                val element = array.optJSONObject(i) ?: continue
                val name = element.optString("item", "")
                val price = element.optDouble("price", 0.0)
                if (name.isNotBlank()) {
                    result.add(GamerPayItemInfo(name = name, price = price))
                }
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing GamerPay JSON (first 200 chars: ${body.take(200)})", e)
            emptyList()
        }
    }
}
