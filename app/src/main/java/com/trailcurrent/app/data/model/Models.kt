package com.trailcurrent.app.data.model

import com.google.gson.annotations.SerializedName

// Generic Response Models
data class MessageResponse(
    val message: String
)

// Thermostat Models
data class ThermostatState(
    @SerializedName("target_temp") val targetTemp: Int,
    val mode: String // 'heat', 'cool', 'auto', 'off'
)

data class ThermostatUpdateRequest(
    @SerializedName("target_temp") val targetTemp: Int? = null,
    val mode: String? = null
)

data class ThermostatResponse(
    val success: Boolean,
    @SerializedName("target_temp") val targetTemp: Int,
    val mode: String
)

// Light Models
data class Light(
    val id: Int,
    @SerializedName("_id") val dbId: String?,
    val name: String?,
    val state: Int, // 0 or 1
    val brightness: Int, // 0-100
    @SerializedName("updated_at") val updatedAt: String?
)

data class LightConfig(
    val id: Int,
    val name: String?
)

data class LightUpdateRequest(
    val state: Int,
    val brightness: Int? = null
)

data class BulkLightUpdateRequest(
    val lights: List<LightStateUpdate>
)

data class LightStateUpdate(
    val id: Int,
    val state: Int
)

// Trailer Level Models
data class TrailerLevel(
    @SerializedName("_id") val id: String?,
    @SerializedName("front_back") val frontBack: Double, // degrees
    @SerializedName("side_to_side") val sideToSide: Double, // degrees
    @SerializedName("updated_at") val updatedAt: String?
)

// Energy Models
data class EnergyState(
    @SerializedName("solar_watts") val solarWatts: Double,
    @SerializedName("battery_percent") val batteryPercent: Double,
    @SerializedName("battery_voltage") val batteryVoltage: Double,
    @SerializedName("charge_type") val chargeType: String, // 'float', 'bulk', 'absorption', 'equalize'
    @SerializedName("time_remaining_minutes") val timeRemainingMinutes: Double?
)

// Water Models
data class WaterState(
    val fresh: Int, // 0-100
    val grey: Int,  // 0-100
    val black: Int, // 0-100
    @SerializedName("updated_at") val updatedAt: String?
)

// Air Quality Models
data class AirQualityState(
    @SerializedName("iaq_index") val iaqIndex: Int,
    @SerializedName("co2_ppm") val co2Ppm: Int
)

data class TempHumidState(
    val tempInC: Double,
    val tempInF: Double,
    val humidity: Double
)

// GPS Models
data class GpsLatLon(
    val latitude: Double,
    val longitude: Double
)

data class GpsAltitude(
    val altitudeInMeters: Double,
    val altitudeFeet: Double
)

data class GnssDetails(
    val numberOfSatellites: Int,
    val speedOverGround: Double,
    val courseOverGround: Double,
    val gnssMode: Int? = null
)

// Settings Models
data class AppSettings(
    @SerializedName("_id") val id: String?,
    val theme: String, // 'dark' or 'light'
    val timezone: String,
    @SerializedName("clock_format") val clockFormat: String, // '12h' or '24h'
    @SerializedName("available_timezones") val availableTimezones: List<String>?,
    @SerializedName("updated_at") val updatedAt: String?
)

data class SettingsUpdateRequest(
    val theme: String? = null,
    val timezone: String? = null,
    @SerializedName("clock_format") val clockFormat: String? = null
)

// Health Check
data class HealthResponse(
    val status: String,
    val timestamp: String
)

// WebSocket Message
data class WebSocketMessage(
    val type: String,
    val data: Any?,
    val timestamp: String?
)
