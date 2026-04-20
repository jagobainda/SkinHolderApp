package dev.jagoba.skinholder.models

/**
 * Representa la respuesta de una consulta de precio al mercado de Steam.
 *
 * @param price El precio más bajo encontrado. -1 si no se pudo obtener.
 * @param failed Indica si la petición falló.
 */
data class SteamPriceResponse(val price: Float, val failed: Boolean)
