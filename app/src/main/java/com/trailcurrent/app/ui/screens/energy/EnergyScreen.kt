package com.trailcurrent.app.ui.screens.energy

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
import com.trailcurrent.app.data.model.EnergyState
import com.trailcurrent.app.data.repository.ApiResult
import com.trailcurrent.app.data.repository.VehicleRepository
import com.trailcurrent.app.data.websocket.WebSocketEvent
import com.trailcurrent.app.data.websocket.WebSocketService
import com.trailcurrent.app.ui.theme.TrailCurrentColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EnergyUiState(
    val energy: EnergyState? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EnergyViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val webSocketService: WebSocketService
) : ViewModel() {

    private val _uiState = MutableStateFlow(EnergyUiState())
    val uiState: StateFlow<EnergyUiState> = _uiState.asStateFlow()

    init {
        observeWebSocketEvents()
        loadInitialData()
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            webSocketService.events.collect { event ->
                when (event) {
                    is WebSocketEvent.Energy -> {
                        _uiState.value = _uiState.value.copy(energy = event.state)
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = vehicleRepository.getEnergy()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(energy = result.data)
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
fun EnergyScreen(
    viewModel: EnergyViewModel = hiltViewModel()
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
            text = "Energy",
            style = MaterialTheme.typography.headlineMedium
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Solar Input
            EnergyCard(
                icon = Icons.Default.WbSunny,
                title = "Solar Input",
                value = uiState.energy?.solarWatts?.let { "${it.toInt()} W" } ?: "-- W",
                iconTint = TrailCurrentColors.solar,
                subtitle = "Power from panels"
            )

            // Battery Level
            val batteryPercent = uiState.energy?.batteryPercent ?: 0.0
            val batteryColor = when {
                batteryPercent >= 50 -> TrailCurrentColors.batteryGood
                batteryPercent >= 20 -> TrailCurrentColors.statusWarning
                else -> TrailCurrentColors.batteryLow
            }
            EnergyCard(
                icon = when {
                    batteryPercent >= 90 -> Icons.Default.BatteryFull
                    batteryPercent >= 50 -> Icons.Default.Battery5Bar
                    batteryPercent >= 20 -> Icons.Default.Battery2Bar
                    else -> Icons.Default.Battery0Bar
                },
                title = "Battery Level",
                value = "${batteryPercent.toInt()}%",
                iconTint = batteryColor,
                progress = (batteryPercent / 100).toFloat(),
                progressColor = batteryColor
            )

            // Battery Voltage
            EnergyCard(
                icon = Icons.Default.Bolt,
                title = "Battery Voltage",
                value = uiState.energy?.batteryVoltage?.let {
                    String.format("%.1f V", it)
                } ?: "-- V",
                iconTint = MaterialTheme.colorScheme.primary,
                subtitle = "Current voltage"
            )

            // Charge Status
            val chargeType = uiState.energy?.chargeType ?: "unknown"
            val chargeInfo = when (chargeType.lowercase()) {
                "float" -> Pair("Float Charging", "Battery fully charged, maintaining")
                "bulk" -> Pair("Bulk Charging", "Rapid charging in progress")
                "absorption" -> Pair("Absorption", "Topping off the battery")
                "equalize" -> Pair("Equalizing", "Balancing battery cells")
                else -> Pair("Unknown", "Status unavailable")
            }
            EnergyCard(
                icon = Icons.Default.BatteryChargingFull,
                title = "Charge Status",
                value = chargeInfo.first,
                iconTint = TrailCurrentColors.statusGood,
                subtitle = chargeInfo.second
            )

            // Time Remaining
            uiState.energy?.timeRemainingMinutes?.let { minutes ->
                if (minutes > 0) {
                    val hours = (minutes / 60).toInt()
                    val mins = (minutes % 60).toInt()
                    EnergyCard(
                        icon = Icons.Default.Timer,
                        title = "Time Remaining",
                        value = if (hours > 0) "${hours}h ${mins}m" else "${mins}m",
                        iconTint = MaterialTheme.colorScheme.secondary,
                        subtitle = "Until fully charged"
                    )
                }
            }
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
fun EnergyCard(
    icon: ImageVector,
    title: String,
    value: String,
    iconTint: Color,
    subtitle: String? = null,
    progress: Float? = null,
    progressColor: Color = MaterialTheme.colorScheme.primary
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = iconTint
                )

                Spacer(Modifier.width(12.dp))

                // Title and subtitle - takes available space
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Value - right aligned, doesn't wrap
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1
                )
            }

            progress?.let {
                LinearProgressIndicator(
                    progress = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = progressColor,
                    trackColor = progressColor.copy(alpha = 0.2f)
                )
            }
        }
    }
}
