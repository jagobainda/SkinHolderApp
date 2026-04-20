package dev.jagoba.skinholder.dataservice.api

import dev.jagoba.skinholder.models.SteamPriceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

class SteamPriceApi @Inject constructor() {
    private val maxRetries: Int = 5

    private val baseUrl: String =
        "https://steamcommunity.com/market/priceoverview/?country=%s&currency=%s&appid=%s&market_hash_name=%s"

    /**
     * Realiza una petición HTTP GET a la API de precios del mercado de Steam.
     *
     * @param country el país para la consulta
     * @param currency la moneda (código numérico de Steam)
     * @param appId el ID de la aplicación de Steam
     * @param marketHashName el nombre del artículo en el mercado de Steam
     * @return la respuesta en un objeto SteamPriceResponse
     */
    suspend fun fetchPrice(
        country: String,
        currency: Int,
        appId: Int,
        marketHashName: String
    ): SteamPriceResponse = coroutineScope {
        val url = String.format(baseUrl, country, currency, appId, marketHashName)

        var attempt = 0
        var response: String? = null
        var failed = false
        while (attempt <= maxRetries) {
            try {
                val result = async(Dispatchers.IO) {
                    val steamUrl = URL(url)
                    val con = steamUrl.openConnection() as HttpsURLConnection
                    con.requestMethod = "GET"

                    try {
                        con.inputStream.reader().readText()
                    } finally {
                        con.disconnect()
                    }
                }
                response = result.await()
                break
            } catch (e: IOException) {
                if (attempt == maxRetries) {
                    response = ""
                }
                attempt++
                failed = true
                delay(3000)
            }
        }
        SteamPriceResponse(extractPriceFromJson(response), failed)
    }

    /**
     * Extrae el precio más bajo de la respuesta JSON de Steam.
     *
     * @param input Cadena JSON de respuesta.
     * @return El precio como Float. -1 si la entrada es vacía o nula.
     */
    private fun extractPriceFromJson(input: String?): Float {
        if (input.isNullOrEmpty()) return -1f

        val json = JSONObject(input)
        val priceString = json.getString("lowest_price")
            .replace(",", ".")
            .replace("-", "0")
            .replace("€", "")
            .replace("â\u0082¬", "")
            .replace(" ", "")
        return priceString.toFloatOrNull() ?: -1f
    }
}
