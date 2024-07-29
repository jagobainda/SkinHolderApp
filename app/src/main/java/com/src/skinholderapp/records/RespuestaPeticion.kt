package com.src.skinholderapp.records

/**
 * Clase que representa la respuesta a una petición.
 *
 * @param RESPUESTA La respuesta de la petición.
 * @param FALLO Indica si la petición falló o no.
 */
data class RespuestaPeticion(val RESPUESTA: Float, val FALLO: Boolean)
