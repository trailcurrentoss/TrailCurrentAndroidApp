package com.trailcurrentoutbound.app.ui.screens.airquality

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailcurrentoutbound.app.data.model.AirQualityState
import com.trailcurrentoutbound.app.data.model.TempHumidState
import com.trailcurrentoutbound.app.data.repository.ApiResult
import com.trailcurrentoutbound.app.data.repository.VehicleRepository
import com.trailcurrentoutbound.app.data.websocket.WebSocketEvent
import com.trailcurrentoutbound.app.data.websocket.WebSocketService
import com.trailcurrentoutbound.app.ui.theme.TrailCurrentOutboundColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AirQualityUiState(
    val airQuality: AirQualityState? = null,
    val tempHumid: TempHumidState? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AirQualityViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val webSocketService: WebSocketService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AirQualityUiState())
    val uiState: StateFlow<AirQualityUiState> = _uiState.asStateFlow()

    init {
        observeWebSocketEvents()
        loadInitialData()
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            webSocketService.events.collect { event ->
                when (event) {
                    is WebSocketEvent.AirQuality -> {
                        _uiState.value = _uiState.value.copy(airQuality = event.state)
                    }
                    is WebSocketEvent.TempHumid -> {
                        _uiState.value = _uiState.value.copy(tempHumid = event.state)
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = vehicleRepository.getAirQuality()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(airQuality = result.data)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}

@Composable
fun AirQualityScreen(
    viewModel: AirQualityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Air Quality",
            style = MaterialTheme.typography.headlineMedium
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Temperature
            val tempF = uiState.tempHumid?.tempInF
            val tempC = uiState.tempHumid?.tempInC
            AirQualityCard(
                icon = Icons.Default.Thermostat,
                title = "Temperature",
                value = tempF?.let { String.format("%.1f°F", it) } ?: "--°F",
                subtitle = tempC?.let { String.format("%.1f°C", it) },
                iconTint = MaterialTheme.colorScheme.primary
            )

            // Humidity
            val humidity = uiState.tempHumid?.humidity
            val humidityStatus = when {
                humidity == null -> Pair(Color.Gray, "Unknown")
                humidity < 30 -> Pair(TrailCurrentOutboundColors.statusWarning, "Too Dry")
                humidity > 60 -> Pair(TrailCurrentOutboundColors.statusWarning, "Too Humid")
                else -> Pair(TrailCurrentOutboundColors.statusGood, "Comfortable")
            }
            AirQualityCard(
                icon = Icons.Default.WaterDrop,
                title = "Humidity",
                value = humidity?.let { String.format("%.0f%%", it) } ?: "--%",
                subtitle = humidityStatus.second,
                iconTint = Color(0xFF03A9F4),
                statusColor = humidityStatus.first
            )

            // IAQ Index
            val iaq = uiState.airQuality?.iaqIndex
            val iaqStatus = when {
                iaq == null -> Triple(Color.Gray, "Unknown", "No data available")
                iaq <= 50 -> Triple(TrailCurrentOutboundColors.statusGood, "Good", "Air quality is satisfactory")
                iaq <= 100 -> Triple(TrailCurrentOutboundColors.statusWarning, "Moderate", "Acceptable for most people")
                iaq <= 150 -> Triple(Color(0xFFFF9800), "Sensitive", "May affect sensitive groups")
                iaq <= 200 -> Triple(TrailCurrentOutboundColors.statusCritical, "Unhealthy", "Everyone may be affected")
                else -> Triple(Color(0xFF9C27B0), "Hazardous", "Health warnings")
            }
            AirQualityCard(
                icon = Icons.Default.Air,
                title = "IAQ Index",
                value = iaq?.toString() ?: "--",
                subtitle = iaqStatus.third,
                iconTint = iaqStatus.first,
                statusColor = iaqStatus.first,
                badge = iaqStatus.second
            )

            // CO2 Level
            val co2 = uiState.airQuality?.co2Ppm
            val co2Status = when {
                co2 == null -> Triple(Color.Gray, "Unknown", "No data available")
                co2 < 800 -> Triple(TrailCurrentOutboundColors.statusGood, "Good", "Fresh air")
                co2 < 1000 -> Triple(TrailCurrentOutboundColors.statusWarning, "Moderate", "Consider ventilation")
                co2 < 1500 -> Triple(Color(0xFFFF9800), "Poor", "Ventilation recommended")
                else -> Triple(TrailCurrentOutboundColors.statusCritical, "Unhealthy", "Open windows immediately")
            }
            AirQualityCard(
                icon = Icons.Default.Co2,
                title = "CO₂ Level",
                value = co2?.let { "$it ppm" } ?: "-- ppm",
                subtitle = co2Status.third,
                iconTint = co2Status.first,
                statusColor = co2Status.first,
                badge = co2Status.second
            )
        }

        // Error message
        uiState.error?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
fun AirQualityCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String? = null,
    iconTint: Color,
    statusColor: Color? = null,
    badge: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = iconTint
                    )
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        subtitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineMedium,
                        color = statusColor ?: MaterialTheme.colorScheme.onSurface
                    )

                    badge?.let {
                        Surface(
                            color = (statusColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor ?: MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
