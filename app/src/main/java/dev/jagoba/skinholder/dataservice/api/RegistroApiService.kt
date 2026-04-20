package dev.jagoba.skinholder.dataservice.api

import dev.jagoba.skinholder.models.dashboard.VarianceStats
import dev.jagoba.skinholder.models.registros.Registro
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface RegistroApiService {

    @GET("Registros/GetLastRegistro")
    suspend fun getLastRegistro(): Response<Registro>

    @GET("Registros/GetVarianceStats")
    suspend fun getVarianceStats(): Response<VarianceStats>

    @GET("Registros")
    suspend fun getRegistros(): Response<List<Registro>>

    @POST("Registros")
    suspend fun createRegistro(@Body registro: Registro): Response<Long>

    @DELETE("Registros")
    suspend fun deleteRegistro(@Query("registroId") registroId: Long): Response<Unit>
}
