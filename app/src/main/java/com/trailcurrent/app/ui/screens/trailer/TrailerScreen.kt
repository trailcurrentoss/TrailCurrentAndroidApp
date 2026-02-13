package com.trailcurrent.app.ui.screens.trailer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailcurrent.app.data.model.GnssDetails
import com.trailcurrent.app.data.model.GpsLatLon
import com.trailcurrent.app.data.model.TrailerLevel
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
import kotlin.math.abs
import kotlin.math.tan

data class TrailerUiState(
    val level: TrailerLevel? = null,
    val gnssDetails: GnssDetails? = null,
    val position: GpsLatLon? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TrailerViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val webSocketService: WebSocketService
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrailerUiState())
    val uiState: StateFlow<TrailerUiState> = _uiState.asStateFlow()

    init {
        observeWebSocketEvents()
        loadInitialData()
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            webSocketService.events.collect { event ->
                when (event) {
                    is WebSocketEvent.Level -> {
                        _uiState.value = _uiState.value.copy(level = event.level)
                    }
                    is WebSocketEvent.GnssDetails -> {
                        _uiState.value = _uiState.value.copy(gnssDetails = event.details)
                    }
                    is WebSocketEvent.LatLon -> {
                        _uiState.value = _uiState.value.copy(position = event.position)
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = vehicleRepository.getTrailerLevel()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(level = result.data)
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
fun TrailerScreen(
    viewModel: TrailerViewModel = hiltViewModel()
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
            text = "Trailer Status",
            style = MaterialTheme.typography.headlineMedium
        )

        // Level Indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LevelIndicatorCard(
                title = "Front/Back",
                degrees = uiState.level?.frontBack ?: 0.0,
                isVertical = true,
                modifier = Modifier.weight(1f)
            )

            LevelIndicatorCard(
                title = "Side/Side",
                degrees = uiState.level?.sideToSide ?: 0.0,
                isVertical = false,
                modifier = Modifier.weight(1f)
            )
        }

        // GNSS Details Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Satellite, contentDescription = null)
                    Text(
                        text = "GNSS Details",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // 2x2 Grid layout with equal-width cells
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GnssDetailItem(
                        icon = Icons.Default.SatelliteAlt,
                        label = "Satellites",
                        value = uiState.gnssDetails?.numberOfSatellites?.toString() ?: "--",
                        modifier = Modifier.weight(1f)
                    )
                    GnssDetailItem(
                        icon = Icons.Default.Speed,
                        label = "Speed",
                        value = uiState.gnssDetails?.speedOverGround?.let {
                            String.format("%.1f mph", it)
                        } ?: "-- mph",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GnssDetailItem(
                        icon = Icons.Default.Explore,
                        label = "Course",
                        value = uiState.gnssDetails?.courseOverGround?.let {
                            String.format("%.1f°", it)
                        } ?: "--°",
                        modifier = Modifier.weight(1f)
                    )
                    GnssDetailItem(
                        icon = Icons.Default.GpsFixed,
                        label = "Mode",
                        value = uiState.gnssDetails?.gnssMode?.let {
                            when (it) {
                                0 -> "No Fix"
                                1 -> "GPS"
                                2 -> "Beidou"
                                3 -> "GPS + Beidou"
                                4 -> "Glonass"
                                5 -> "GPS + Glonass"
                                6 -> "Beidou + Glonass"
                                7 -> "GPS + Beidou + Glonass"
                                else -> "No Fix"
                            }
                        } ?: "--",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Position if available
                uiState.position?.let { pos ->
                    Divider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Latitude",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%.6f", pos.latitude),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Longitude",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%.6f", pos.longitude),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
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
fun LevelIndicatorCard(
    title: String,
    degrees: Double,
    isVertical: Boolean,
    modifier: Modifier = Modifier
) {
    val absDegrees = abs(degrees)
    val statusColor = when {
        absDegrees < 1 -> TrailCurrentColors.statusGood
        absDegrees < 3 -> TrailCurrentColors.statusWarning
        else -> TrailCurrentColors.statusCritical
    }

    // Calculate offset in inches (assuming 10ft trailer width/length)
    val inchesOffset = (tan(Math.toRadians(degrees)) * 60).let { abs(it) }

    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )

            // Bubble level visualization
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val outerRadius = size.minDimension / 2 - 4

                    // Outer circle
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.3f),
                        radius = outerRadius,
                        center = center,
                        style = Stroke(width = 2f)
                    )

                    // Inner target circle
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.2f),
                        radius = outerRadius / 3,
                        center = center,
                        style = Stroke(width = 1f)
                    )

                    // Calculate bubble position
                    val maxOffset = outerRadius - 15
                    val normalizedOffset = (degrees / 15.0).coerceIn(-1.0, 1.0)
                    val bubbleOffset = if (isVertical) {
                        Offset(0f, (normalizedOffset * maxOffset).toFloat())
                    } else {
                        Offset((normalizedOffset * maxOffset).toFloat(), 0f)
                    }

                    // Bubble
                    drawCircle(
                        color = statusColor,
                        radius = 12f,
                        center = center + bubbleOffset
                    )
                }
            }

            // Readings
            Text(
                text = String.format("%.1f°", degrees),
                style = MaterialTheme.typography.headlineSmall,
                color = statusColor
            )

            Text(
                text = String.format("%.1f in", inchesOffset),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Direction indicator
            val direction = when {
                isVertical && degrees > 0.5 -> "Front High"
                isVertical && degrees < -0.5 -> "Back High"
                !isVertical && degrees > 0.5 -> "Left High"
                !isVertical && degrees < -0.5 -> "Right High"
                else -> "Level"
            }
            Text(
                text = direction,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor
            )
        }
    }
}

@Composable
fun GnssDetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
            }
        }
    }
}
