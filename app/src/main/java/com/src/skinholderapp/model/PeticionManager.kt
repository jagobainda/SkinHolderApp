package com.src.skinholderapp.model

import com.src.skinholderapp.records.RespuestaPeticion
import com.src.skinholderapp.utils.Peticion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PeticionManager {
    fun ejecutarPeticiones(country: String, currency: Int, appId: Int, marketHashNames: List<String>): List<RespuestaPeticion> {
        val respuestas: MutableList<RespuestaPeticion> = mutableListOf()

        CoroutineScope(Dispatchers.Main).launch { val peticion = Peticion(); marketHashNames.forEach { hashName -> respuestas.add(peticion.hacerPeticion(country, currency, appId, hashName)) } }

        return respuestas
    }
}