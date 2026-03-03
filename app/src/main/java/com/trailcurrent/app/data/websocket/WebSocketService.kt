package com.trailcurrent.app.data.websocket

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.trailcurrent.app.data.model.*
import com.trailcurrent.app.util.PreferencesManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class WebSocketEvent {
    data class Thermostat(val state: ThermostatState) : WebSocketEvent()
    data class Light(val light: com.trailcurrent.app.data.model.Light) : WebSocketEvent()
    data class Energy(val state: EnergyState) : WebSocketEvent()
    data class Water(val state: WaterState) : WebSocketEvent()
    data class AirQuality(val state: AirQualityState) : WebSocketEvent()
    data class TempHumid(val state: TempHumidState) : WebSocketEvent()
    data class LatLon(val position: GpsLatLon) : WebSocketEvent()
    data class Altitude(val altitude: GpsAltitude) : WebSocketEvent()
    data class GnssDetails(val details: com.trailcurrent.app.data.model.GnssDetails) : WebSocketEvent()
    data class Level(val level: TrailerLevel) : WebSocketEvent()
    data class ConnectionStatus(val connected: Boolean, val error: String? = null) : WebSocketEvent()
}

@Singleton
class WebSocketService @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "WebSocketService"
        private const val MAX_RECONNECT_ATTEMPTS = 10
        private const val INITIAL_RECONNECT_DELAY_MS = 1000L
        private const val MAX_RECONNECT_DELAY_MS = 30000L
    }

    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private var hasConnected = false

    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<WebSocketEvent> = _events

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun connect(token: String?) {
        hasConnected = true
        scope.launch {
            doConnect()
        }
    }

    private suspend fun doConnect() {
        disconnect()

        val wsUrl = preferencesManager.getWebSocketUrl()
        val apiKey = preferencesManager.getApiKey()
        Log.d(TAG, "Connecting to WebSocket: $wsUrl")

        if (wsUrl.isBlank()) {
            Log.w(TAG, "WebSocket URL is blank, skipping connection")
            return
        }

        // Use the injected client with proper SSL config, but add WebSocket-specific settings
        val wsClient = try {
            okHttpClient.newBuilder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(30, TimeUnit.SECONDS)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build WebSocket client: ${e.message}", e)
            return
        }

        val requestBuilder = try {
            Request.Builder().url(wsUrl)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid WebSocket URL: $wsUrl", e)
            return
        }
        if (!apiKey.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", apiKey)
        }

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                reconnectAttempts = 0
                _connectionState.value = true
                scope.launch {
                    _events.emit(WebSocketEvent.ConnectionStatus(true))
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WebSocket message: $text")
                parseAndEmitMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code - $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code - $reason")
                _connectionState.value = false
                scope.launch {
                    _events.emit(WebSocketEvent.ConnectionStatus(false))
                }
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}", t)
                _connectionState.value = false
                scope.launch {
                    _events.emit(WebSocketEvent.ConnectionStatus(false, t.message))
                }
                scheduleReconnect()
            }
        }

        webSocket = wsClient.newWebSocket(requestBuilder.build(), listener)
    }

    private fun parseAndEmitMessage(text: String) {
        try {
            val json = gson.fromJson(text, JsonObject::class.java)
            val type = json.get("type")?.asString ?: return
            val data = json.get("data")

            val event: WebSocketEvent? = when (type) {
                "thermostat" -> {
                    val state = gson.fromJson(data, ThermostatState::class.java)
                    WebSocketEvent.Thermostat(state)
                }
                "light" -> {
                    val light = gson.fromJson(data, com.trailcurrent.app.data.model.Light::class.java)
                    WebSocketEvent.Light(light)
                }
                "energy" -> {
                    val state = gson.fromJson(data, EnergyState::class.java)
                    WebSocketEvent.Energy(state)
                }
                "water" -> {
                    val state = gson.fromJson(data, WaterState::class.java)
                    WebSocketEvent.Water(state)
                }
                "airquality" -> {
                    val state = gson.fromJson(data, AirQualityState::class.java)
                    WebSocketEvent.AirQuality(state)
                }
                "temphumid" -> {
                    val state = gson.fromJson(data, TempHumidState::class.java)
                    WebSocketEvent.TempHumid(state)
                }
                "latlon" -> {
                    val position = gson.fromJson(data, GpsLatLon::class.java)
                    WebSocketEvent.LatLon(position)
                }
                "alt" -> {
                    val altitude = gson.fromJson(data, GpsAltitude::class.java)
                    WebSocketEvent.Altitude(altitude)
                }
                "gnss_details" -> {
                    val details = gson.fromJson(data, com.trailcurrent.app.data.model.GnssDetails::class.java)
                    WebSocketEvent.GnssDetails(details)
                }
                "level" -> {
                    val level = gson.fromJson(data, TrailerLevel::class.java)
                    WebSocketEvent.Level(level)
                }
                else -> {
                    Log.w(TAG, "Unknown WebSocket message type: $type")
                    null
                }
            }

            event?.let {
                scope.launch {
                    _events.emit(it)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing WebSocket message: ${e.message}", e)
        }
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnect attempts reached")
            return
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delayMs = minOf(
                INITIAL_RECONNECT_DELAY_MS * (1 shl reconnectAttempts),
                MAX_RECONNECT_DELAY_MS
            )
            Log.d(TAG, "Scheduling reconnect in ${delayMs}ms (attempt ${reconnectAttempts + 1})")
            delay(delayMs)
            reconnectAttempts++
            doConnect()
        }
    }

    fun reconnectIfNeeded() {
        if (_connectionState.value || !hasConnected) return
        Log.d(TAG, "App resumed — resetting reconnect counter and reconnecting")
        reconnectAttempts = 0
        reconnectJob?.cancel()
        reconnectJob = null
        scope.launch {
            doConnect()
        }
    }

    fun disconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _connectionState.value = false
    }

    fun destroy() {
        disconnect()
        scope.cancel()
    }
}
