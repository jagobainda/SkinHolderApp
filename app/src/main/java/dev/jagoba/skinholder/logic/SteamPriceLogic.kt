package dev.jagoba.skinholder.logic

import dev.jagoba.skinholder.dataservice.api.SteamPriceApi
import dev.jagoba.skinholder.models.SteamPriceResponse
import javax.inject.Inject

class SteamPriceLogic @Inject constructor(private val steamPriceApi: SteamPriceApi) {

    /**
     * Ejecuta consultas de precio para una lista de artículos del mercado de Steam.
     *
     * @param country el país para la consulta
     * @param currency la moneda (código numérico de Steam)
     * @param appId el ID de la aplicación de Steam
     * @param marketHashNames lista de nombres de artículos
     * @return lista de respuestas de precio
     */
    suspend fun fetchPrices(
        country: String,
        currency: Int,
        appId: Int,
        marketHashNames: List<String>
    ): List<SteamPriceResponse> {
        return marketHashNames.map { hashName ->
            steamPriceApi.fetchPrice(country, currency, appId, hashName)
        }
    }
}
