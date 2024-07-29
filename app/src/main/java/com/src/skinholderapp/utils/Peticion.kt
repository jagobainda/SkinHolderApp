package com.src.skinholderapp.utils


import com.src.skinholderapp.records.RespuestaPeticion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class Peticion {
    private val numeroMaximoIntentos: Int = 5

    private val enlaceBase: String = "https://steamcommunity.com/market/priceoverview/?country=%s&currency=%s&appid=%s&market_hash_name=%s"

    /**
     * Realiza una petición HTTP GET a una API de Steam con los parámetros especificados.
     *
     * @param country el país en el que se desea realizar la petición
     * @param currency la moneda en la que se desea realizar la petición
     * @param appId el ID de la aplicación de Steam
     * @param marketHashName el nombre del artículo en el mercado de Steam
     * @return la respuesta de la API de Steam en un objeto RespuestaPeticion
     */
    suspend fun hacerPeticion(country: String, currency: Int, appId: Int, marketHashName: String): RespuestaPeticion = coroutineScope {
        val enlace = String.format(enlaceBase, country, currency, appId, marketHashName)

        var c = 0
        var respuesta: String? = null
        var fallo = false
        while (c <= numeroMaximoIntentos) {
            try {
                val resultado = async(Dispatchers.IO) {
                    val steamUrl = URL(enlace)
                    val con = steamUrl.openConnection() as HttpsURLConnection
                    con.requestMethod = "GET"

                    try {
                        val inStream = con.inputStream
                        val response = StringBuilder()
                        response.append(inStream.reader().readText())
                        response.toString()
                    } finally {
                        con.disconnect()
                    }
                }
                respuesta = resultado.await()
                break
            } catch (e: IOException) {
                if (c == numeroMaximoIntentos) {
                    respuesta = ""
                }
                c++
                fallo = true
                delay(3000)
            }
        }
        RespuestaPeticion(extraerPrecioDeJSON(respuesta), fallo)
    }

    /**
     * Extrae el precio más bajo de un objeto JSON.
     *
     * @param input Cadena de texto que contiene el objeto JSON.
     * @return El precio más bajo como un número de punto flotante. Si la cadena de entrada está vacía, devuelve -1.
     */
    private fun extraerPrecioDeJSON(input: String?): Float {
        return if (input!!.isEmpty()) {
            -1f
        } else {
            val json = JSONObject(input)
            val priceString = json.getString("lowest_price").replace(",", ".").replace("-", "0").replace("€", "")
            priceString.replace("â‚¬", "").replace(" ", "").toFloatOrNull() ?: -1f
        }
    }
}
