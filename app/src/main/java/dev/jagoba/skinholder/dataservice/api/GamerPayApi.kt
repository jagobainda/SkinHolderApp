package dev.jagoba.skinholder.dataservice.api

import dev.jagoba.skinholder.models.steam.GamerPayItemInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

class GamerPayApi @Inject constructor() {

    private val endpoint: String = "https://api.gamerpay.gg/prices"
    private val timeoutMs: Int = 30_000

    /**
     * Consulta directamente la API pública de GamerPay (igual que el cliente WPF) y
     * devuelve la lista de items con su precio. Devuelve lista vacía si la respuesta
     * no es válida o se produce un error de red. No usa el cliente HTTP autenticado
     * para evitar añadir el token Bearer a un servicio externo.
     */
    suspend fun fetchPrices(): List<GamerPayItemInfo> = withContext(Dispatchers.IO) {
        val connection = (URL(endpoint).openConnection() as HttpsURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
        }

        try {
            if (connection.responseCode !in 200..299) return@withContext emptyList()

            val body = connection.inputStream.reader().use { it.readText() }
            parsePrices(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parsePrices(body: String): List<GamerPayItemInfo> {
        if (body.isBlank()) return emptyList()

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
        return result
    }
}
