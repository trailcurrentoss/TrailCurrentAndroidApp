package com.trailcurrent.app.ui.screens.water

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailcurrent.app.data.model.WaterState
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

data class WaterUiState(
    val water: WaterState? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WaterViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val webSocketService: WebSocketService
) : ViewModel() {

    private val _uiState = MutableStateFlow(WaterUiState())
    val uiState: StateFlow<WaterUiState> = _uiState.asStateFlow()

    init {
        observeWebSocketEvents()
        loadInitialData()
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            webSocketService.events.collect { event ->
                when (event) {
                    is WebSocketEvent.Water -> {
                        _uiState.value = _uiState.value.copy(water = event.state)
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = vehicleRepository.getWater()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(water = result.data)
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
fun WaterScreen(
    viewModel: WaterViewModel = hiltViewModel()
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
            text = "Water Tanks",
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
            // Fresh Water Tank
            WaterTankCard(
                title = "Fresh Water",
                level = uiState.water?.fresh ?: 0,
                tankColor = TrailCurrentColors.freshWater,
                icon = Icons.Default.WaterDrop,
                warningThreshold = 25,
                criticalThreshold = 10,
                isWaste = false
            )

            // Grey Water Tank
            WaterTankCard(
                title = "Grey Water",
                level = uiState.water?.grey ?: 0,
                tankColor = TrailCurrentColors.greyWater,
                icon = Icons.Default.Opacity,
                warningThreshold = 75,
                criticalThreshold = 90,
                isWaste = true
            )

            // Black Water Tank
            WaterTankCard(
                title = "Black Water",
                level = uiState.water?.black ?: 0,
                tankColor = TrailCurrentColors.blackWater,
                icon = Icons.Default.Delete,
                warningThreshold = 75,
                criticalThreshold = 90,
                isWaste = true
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
fun WaterTankCard(
    title: String,
    level: Int,
    tankColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    warningThreshold: Int,
    criticalThreshold: Int,
    isWaste: Boolean
) {
    // For waste tanks, high is bad. For fresh, low is bad.
    val statusColor = if (isWaste) {
        when {
            level >= criticalThreshold -> TrailCurrentColors.statusCritical
            level >= warningThreshold -> TrailCurrentColors.statusWarning
            else -> TrailCurrentColors.statusGood
        }
    } else {
        when {
            level <= criticalThreshold -> TrailCurrentColors.statusCritical
            level <= warningThreshold -> TrailCurrentColors.statusWarning
            else -> TrailCurrentColors.statusGood
        }
    }

    val statusText = if (isWaste) {
        when {
            level >= criticalThreshold -> "Needs Emptying!"
            level >= warningThreshold -> "Getting Full"
            else -> "Good"
        }
    } else {
        when {
            level <= criticalThreshold -> "Nearly Empty!"
            level <= warningThreshold -> "Getting Low"
            else -> "Good"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = tankColor
                    )
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Circle,
                                contentDescription = null,
                                modifier = Modifier.size(8.dp),
                                tint = statusColor
                            )
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = statusColor
                            )
                        }
                    }
                }

                Text(
                    text = "$level%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = statusColor
                )
            }

            // Tank visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tankColor.copy(alpha = 0.1f))
            ) {
                // Level markers
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(0, 25, 50, 75, 100).forEach { marker ->
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(tankColor.copy(alpha = 0.3f))
                        )
                    }
                }

                // Fill level
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(level / 100f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    tankColor.copy(alpha = 0.5f),
                                    tankColor.copy(alpha = 0.8f)
                                )
                            )
                        )
                )
            }

            // Level markers labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("E", "1/4", "1/2", "3/4", "F").forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
