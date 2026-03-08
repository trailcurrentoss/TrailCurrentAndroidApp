package com.trailcurrentoutbound.app.data.api

import com.trailcurrentoutbound.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Thermostat
    @GET("api/thermostat")
    suspend fun getThermostat(): Response<ThermostatState>

    @PUT("api/thermostat")
    suspend fun updateThermostat(@Body request: ThermostatUpdateRequest): Response<ThermostatResponse>

    // Lights
    @GET("api/lights")
    suspend fun getLights(): Response<List<Light>>

    @PUT("api/lights/{id}")
    suspend fun updateLight(
        @Path("id") id: Int,
        @Body request: LightUpdateRequest
    ): Response<Light>

    @PUT("api/lights")
    suspend fun updateLightsBulk(@Body request: BulkLightUpdateRequest): Response<List<Light>>

    // Trailer Level
    @GET("api/trailer/level")
    suspend fun getTrailerLevel(): Response<TrailerLevel>

    // Energy
    @GET("api/energy")
    suspend fun getEnergy(): Response<EnergyState>

    // Water
    @GET("api/water")
    suspend fun getWater(): Response<WaterState>

    // Air Quality
    @GET("api/airquality")
    suspend fun getAirQuality(): Response<AirQualityState>

    // Settings
    @GET("api/settings")
    suspend fun getSettings(): Response<AppSettings>

    @PUT("api/settings")
    suspend fun updateSettings(@Body request: SettingsUpdateRequest): Response<AppSettings>

    // Health Check
    @GET("api/health")
    suspend fun healthCheck(): Response<HealthResponse>
}
