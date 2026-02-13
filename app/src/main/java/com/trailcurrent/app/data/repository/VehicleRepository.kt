package com.trailcurrent.app.data.repository

import com.trailcurrent.app.data.api.ApiService
import com.trailcurrent.app.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}

@Singleton
class VehicleRepository @Inject constructor() {
    private var apiService: ApiService? = null

    fun setApiService(service: ApiService) {
        this.apiService = service
    }

    // Thermostat
    suspend fun getThermostat(): ApiResult<ThermostatState> {
        val service = apiService ?: return ApiResult.Error("API service not initialized")

        return try {
            val response = service.getThermostat()
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.errorBody()?.string() ?: "Failed to get thermostat")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun updateThermostat(targetTemp: Int?, mode: String?): ApiResult<ThermostatResponse> {
        val service = apiService ?: return ApiResult.Error("API service not initialized")

        return try {
            val response = service.updateThermostat(ThermostatUpdateRequest(targetTemp, mode))
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.errorBody()?.string() ?: "Failed to update thermostat")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    // Lights
    suspend fun getLights(): ApiResult<List<Light>> {
        val service = apiService ?: return ApiResult.Error("API service not initialized")

        return try {
            val response = service.getLights()
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.errorBody()?.string() ?: "Failed to get lights")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun updateLight(id: Int, state: Int, brightness: Int? = null): ApiResult<Light> {
        val service = apiService ?: return ApiResult.Error("API service not initialized")

        return try {
            val response = service.updateLight(id, LightUpdateRequest(state, brightness))
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.errorBody()?.string() ?: "Failed to update light")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    // Trailer Level
    suspend fun getTrailerLevel(): ApiResult<TrailerLevel> {
        val service = apiService ?: return ApiResult.Error("API service not initialized")

        return try {
            val response = service.getTrailerLevel()
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.errorBody()?.string() ?: "Failed to get trailer level")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    // Energy
    suspend fun getEnergy(): ApiResult<EnergyState> {
        val service = apiService ?: return ApiResult.Error("API service not initialized")

        return try {
            val response = service.getEnergy()
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.errorBody()?.string() ?: "Failed to get energy")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    // Water
    suspend fun getWater(): ApiResult<WaterState> {
        val service = apiService ?: return ApiResult.Error("API service not initialized")

        return try {
            val response = service.getWater()
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.errorBody()?.string() ?: "Failed to get water levels")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    // Air Quality
    suspend fun getAirQuality(): ApiResult<AirQualityState> {
        val service = apiService ?: return ApiResult.Error("API service not initialized")

        return try {
            val response = service.getAirQuality()
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.errorBody()?.string() ?: "Failed to get air quality")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    // Settings
    suspend fun getSettings(): ApiResult<AppSettings> {
        val service = apiService ?: return ApiResult.Error("API service not initialized")

        return try {
            val response = service.getSettings()
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.errorBody()?.string() ?: "Failed to get settings")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun updateSettings(theme: String?, timezone: String?, clockFormat: String?): ApiResult<AppSettings> {
        val service = apiService ?: return ApiResult.Error("API service not initialized")

        return try {
            val response = service.updateSettings(SettingsUpdateRequest(theme, timezone, clockFormat))
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.errorBody()?.string() ?: "Failed to update settings")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    // Health Check
    suspend fun healthCheck(): ApiResult<HealthResponse> {
        val service = apiService ?: return ApiResult.Error("API service not initialized")

        return try {
            val response = service.healthCheck()
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.errorBody()?.string() ?: "Health check failed")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}
