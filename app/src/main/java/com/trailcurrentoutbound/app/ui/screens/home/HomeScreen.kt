package com.trailcurrentoutbound.app.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailcurrentoutbound.app.data.model.Light
import com.trailcurrentoutbound.app.data.model.TempHumidState
import com.trailcurrentoutbound.app.data.model.ThermostatState
import com.trailcurrentoutbound.app.data.repository.ApiResult
import com.trailcurrentoutbound.app.data.repository.VehicleRepository
import com.trailcurrentoutbound.app.data.websocket.WebSocketEvent
import com.trailcurrentoutbound.app.data.websocket.WebSocketService
import com.trailcurrentoutbound.app.ui.theme.TrailCurrentOutboundColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val thermostat: ThermostatState? = null,
    val currentTemp: Double? = null,
    val lights: List<Light> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConnected: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val webSocketService: WebSocketService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Suppress WebSocket updates for lights with in-flight commands.
    // The server broadcasts stale state while processing a command, which
    // races with the optimistic UI update and makes the light appear to turn off.
    private val suppressedLightIds = mutableSetOf<Int>()
    private val unsuppressJobs = mutableMapOf<Int, Job>()

    private fun suppressLightUpdates(lightId: Int) {
        suppressedLightIds.add(lightId)
        unsuppressJobs[lightId]?.cancel()
        unsuppressJobs[lightId] = viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            suppressedLightIds.remove(lightId)
            unsuppressJobs.remove(lightId)
        }
    }

    init {
        observeWebSocketEvents()
        loadInitialData()
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            webSocketService.events.collect { event ->
                when (event) {
                    is WebSocketEvent.Thermostat -> {
                        _uiState.value = _uiState.value.copy(thermostat = event.state)
                    }
                    is WebSocketEvent.TempHumid -> {
                        _uiState.value = _uiState.value.copy(currentTemp = event.state.tempInF)
                    }
                    is WebSocketEvent.Light -> {
                        val isSuppressed = event.light.id in suppressedLightIds
                        val updatedLights = _uiState.value.lights.map { light ->
                            if (light.id == event.light.id) {
                                light.copy(
                                    // During suppression, keep optimistic state but accept brightness from PDM
                                    state = if (isSuppressed) light.state else event.light.state,
                                    brightness = event.light.brightness,
                                    // Update name from WebSocket if provided (PWA may rename lights)
                                    name = event.light.name ?: light.name
                                )
                            } else light
                        }
                        _uiState.value = _uiState.value.copy(lights = updatedLights)
                    }
                    is WebSocketEvent.LightsConfig -> {
                        val configMap = event.lights.associateBy { it.id }
                        val updatedLights = _uiState.value.lights.map { light ->
                            val config = configMap[light.id]
                            if (config != null) light.copy(name = config.name) else light
                        }
                        _uiState.value = _uiState.value.copy(lights = updatedLights)
                    }
                    is WebSocketEvent.ConnectionStatus -> {
                        _uiState.value = _uiState.value.copy(isConnected = event.connected)
                    }
                    else -> {}
                }
            }
        }

        viewModelScope.launch {
            webSocketService.connectionState.collect { connected ->
                _uiState.value = _uiState.value.copy(isConnected = connected)
            }
        }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Load thermostat
            when (val result = vehicleRepository.getThermostat()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(thermostat = result.data)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
            }

            // Load lights
            when (val result = vehicleRepository.getLights()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(lights = result.data)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun setThermostatTemp(temp: Int) {
        viewModelScope.launch {
            // Always use auto mode when setting temperature
            vehicleRepository.updateThermostat(targetTemp = temp, mode = "auto")
        }
    }

    fun toggleThermostat() {
        viewModelScope.launch {
            val currentMode = _uiState.value.thermostat?.mode
            val newMode = if (currentMode == "off") "auto" else "off"
            vehicleRepository.updateThermostat(targetTemp = null, mode = newMode)
        }
    }

    fun toggleLight(light: Light) {
        val newState = if (light.state == 1) 0 else 1

        // Suppress WebSocket updates and optimistically update UI
        suppressLightUpdates(light.id)
        val updatedLights = _uiState.value.lights.map { l ->
            if (l.id == light.id) l.copy(state = newState) else l
        }
        _uiState.value = _uiState.value.copy(lights = updatedLights)

        viewModelScope.launch {
            when (vehicleRepository.updateLight(light.id, newState, null)) {
                is ApiResult.Success -> { /* UI already updated optimistically */ }
                is ApiResult.Error -> { /* suppress — WebSocket will restore correct state after timeout */ }
            }
        }
    }

    fun setLightBrightness(light: Light, brightnessPercent: Int) {
        val state = if (brightnessPercent > 0) 1 else 0
        val actualBrightness = if (brightnessPercent > 0) (brightnessPercent * 255 / 100) else 0

        // Suppress WebSocket updates and optimistically update UI
        suppressLightUpdates(light.id)
        val updatedLights = _uiState.value.lights.map { l ->
            if (l.id == light.id) l.copy(state = state, brightness = actualBrightness) else l
        }
        _uiState.value = _uiState.value.copy(lights = updatedLights)

        // Send command to server (response may contain stale data, so ignore it)
        viewModelScope.launch {
            when (vehicleRepository.updateLight(light.id, state, actualBrightness)) {
                is ApiResult.Success -> { /* UI already updated optimistically */ }
                is ApiResult.Error -> { /* suppress — WebSocket will restore correct state after timeout */ }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Store only the light ID to avoid stale references
    var selectedLightId by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Connection status
        if (!uiState.isConnected) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Connecting...",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Climate Control - Nest-style dial
        ThermostatDial(
            thermostat = uiState.thermostat,
            currentTemp = uiState.currentTemp,
            onTempChange = viewModel::setThermostatTemp,
            onToggle = viewModel::toggleThermostat
        )

        // Lights Section
        Text(
            text = "Lights",
            style = MaterialTheme.typography.titleLarge
        )

        if (uiState.lights.isEmpty() && uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(400.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.lights) { light ->
                    LightCard(
                        light = light,
                        onToggle = { viewModel.toggleLight(light) },
                        onLongPress = { selectedLightId = light.id }
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

    // Brightness Dialog - look up current light state by ID
    selectedLightId?.let { lightId ->
        val currentLight = uiState.lights.find { it.id == lightId }
        if (currentLight != null) {
            BrightnessDialog(
                light = currentLight,
                onDismiss = { selectedLightId = null },
                onBrightnessChange = { brightness ->
                    viewModel.setLightBrightness(currentLight, brightness)
                }
            )
        } else {
            // Light no longer exists, close dialog
            selectedLightId = null
        }
    }
}

@Composable
fun ThermostatDial(
    thermostat: ThermostatState?,
    currentTemp: Double?,
    onTempChange: (Int) -> Unit,
    onToggle: () -> Unit
) {
    val isOn = thermostat?.mode != "off"
    val targetTemp = thermostat?.targetTemp ?: 70
    val current = currentTemp?.toInt()

    // Determine if heating or cooling based on current vs target
    val isHeating = isOn && current != null && current < targetTemp
    val isCooling = isOn && current != null && current > targetTemp

    // Colors
    val activeColor = when {
        !isOn -> Color(0xFF666666)
        isHeating -> TrailCurrentOutboundColors.heating
        isCooling -> TrailCurrentOutboundColors.cooling
        else -> MaterialTheme.colorScheme.primary
    }

    // Temperature range
    val minTemp = 50
    val maxTemp = 90
    val tempRange = maxTemp - minTemp

    // Track drag state - accumulate angle for smooth updates
    var isDragging by remember { mutableStateOf(false) }
    var dragStartTemp by remember { mutableStateOf(targetTemp) }
    var accumulatedAngle by remember { mutableStateOf(0f) }

    // Track the last set temperature to avoid jumping back during API delay
    var lastSetTemp by remember { mutableStateOf<Int?>(null) }
    var isSettling by remember { mutableStateOf(false) }
    var previousServerTemp by remember { mutableStateOf(targetTemp) }

    // Clear settling state when server value changes (acknowledges our change or external change)
    LaunchedEffect(targetTemp) {
        if (isSettling && targetTemp != previousServerTemp) {
            // Server sent a new value - either our change was acknowledged or another device changed it
            isSettling = false
            lastSetTemp = null
        }
        previousServerTemp = targetTemp
    }

    // Fallback: clear settling state after timeout in case server doesn't respond
    LaunchedEffect(isSettling) {
        if (isSettling) {
            kotlinx.coroutines.delay(5000) // 5 second fallback timeout
            isSettling = false
            lastSetTemp = null
        }
    }

    val dragTemp = if (isDragging) {
        // Map accumulated angle to temperature offset
        val tempOffset = (accumulatedAngle / PI * tempRange / 2).toInt()
        (dragStartTemp + tempOffset).coerceIn(minTemp, maxTemp)
    } else targetTemp

    // Show lastSetTemp during settling, otherwise show current drag or server temp
    val displayTemp = when {
        isDragging -> dragTemp
        isSettling && lastSetTemp != null -> lastSetTemp!!
        else -> targetTemp
    }

    // Colors for depth effects
    val bezelColorOuter = Color(0xFF2A2A2A)
    val bezelColorInner = Color(0xFF3D3D3D)
    val dialBackground = Color(0xFF1A1A1A)
    val glossHighlight = Color.White.copy(alpha = 0.1f)
    val centerColor = Color(0xFF252525)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Power toggle button - top right
            IconButton(
                onClick = onToggle,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(40.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isOn)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = if (isOn) "Turn Off" else "Turn On",
                    tint = if (isOn)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Dial with depth - centered
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(220.dp)
                    .pointerInput(isOn, targetTemp) {
                        if (!isOn) return@pointerInput

                        var lastAngle = 0f

                        detectDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                dragStartTemp = targetTemp
                                accumulatedAngle = 0f
                                // Calculate initial angle
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                lastAngle = atan2(offset.y - centerY, offset.x - centerX)
                            },
                            onDragEnd = {
                                isDragging = false
                                val finalTemp = (dragStartTemp + (accumulatedAngle / PI * tempRange / 2).toInt())
                                    .coerceIn(minTemp, maxTemp)
                                if (finalTemp != targetTemp) {
                                    // Set settling state to keep displaying user's choice
                                    // while waiting for server response
                                    lastSetTemp = finalTemp
                                    isSettling = true
                                    onTempChange(finalTemp)
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                                accumulatedAngle = 0f
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f

                                val currentAngle = atan2(
                                    change.position.y - centerY,
                                    change.position.x - centerX
                                )

                                // Calculate angle delta
                                var angleDelta = currentAngle - lastAngle

                                // Handle wrap-around at ±π
                                if (angleDelta > PI) angleDelta -= (2 * PI).toFloat()
                                if (angleDelta < -PI) angleDelta += (2 * PI).toFloat()

                                // Accumulate the angle change
                                accumulatedAngle += angleDelta
                                lastAngle = currentAngle
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val outerRadius = size.minDimension / 2

                    // Layer 1: Multi-layer soft shadow for raised/floating effect
                    // Outermost shadow layer - very soft and spread out
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.0f),
                                Color.Black.copy(alpha = 0.05f),
                                Color.Black.copy(alpha = 0.15f),
                                Color.Black.copy(alpha = 0.0f)
                            ),
                            center = center,
                            radius = outerRadius + 16.dp.toPx()
                        ),
                        radius = outerRadius + 16.dp.toPx(),
                        center = center
                    )
                    // Middle shadow layer
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.15f),
                                Color.Black.copy(alpha = 0.0f)
                            ),
                            center = center,
                            radius = outerRadius + 8.dp.toPx()
                        ),
                        radius = outerRadius + 8.dp.toPx(),
                        center = center
                    )
                    // Inner shadow layer - tighter ambient occlusion
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.0f)
                            ),
                            center = center,
                            radius = outerRadius + 2.dp.toPx()
                        ),
                        radius = outerRadius + 2.dp.toPx(),
                        center = center
                    )

                    // Layer 2: Outer bezel ring (dark metallic)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(bezelColorInner, bezelColorOuter),
                            center = Offset(center.x - 20.dp.toPx(), center.y - 20.dp.toPx()),
                            radius = outerRadius * 1.5f
                        ),
                        radius = outerRadius - 4.dp.toPx(),
                        center = center
                    )

                    // Layer 3: Inner dial background
                    val dialRadius = outerRadius - 16.dp.toPx()
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF2D2D2D), dialBackground),
                            center = center,
                            radius = dialRadius
                        ),
                        radius = dialRadius,
                        center = center
                    )

                    // Layer 4: Temperature arc track (subtle)
                    val arcRadius = dialRadius - 20.dp.toPx()
                    val arcStrokeWidth = 12.dp.toPx()
                    drawArc(
                        color = Color(0xFF333333),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                        size = Size(arcRadius * 2, arcRadius * 2),
                        style = Stroke(width = arcStrokeWidth, cap = StrokeCap.Round)
                    )

                    // Layer 5: Temperature arc (colored)
                    val tempProgress = (displayTemp - minTemp).toFloat() / tempRange
                    val sweepAngle = tempProgress * 270f

                    if (sweepAngle > 0) {
                        // Glow effect behind arc
                        drawArc(
                            color = activeColor.copy(alpha = 0.3f),
                            startAngle = 135f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                            size = Size(arcRadius * 2, arcRadius * 2),
                            style = Stroke(width = arcStrokeWidth + 8.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Main arc
                        drawArc(
                            color = activeColor,
                            startAngle = 135f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                            size = Size(arcRadius * 2, arcRadius * 2),
                            style = Stroke(width = arcStrokeWidth, cap = StrokeCap.Round)
                        )

                        // Indicator knob at current position
                        val indicatorAngle = (135f + sweepAngle) * (PI.toFloat() / 180f)
                        val indicatorX = center.x + arcRadius * cos(indicatorAngle)
                        val indicatorY = center.y + arcRadius * sin(indicatorAngle)

                        // Knob shadow
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.4f),
                            radius = 10.dp.toPx(),
                            center = Offset(indicatorX + 2.dp.toPx(), indicatorY + 2.dp.toPx())
                        )
                        // Knob
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(activeColor, activeColor.copy(alpha = 0.8f)),
                                center = Offset(indicatorX - 2.dp.toPx(), indicatorY - 2.dp.toPx()),
                                radius = 10.dp.toPx()
                            ),
                            radius = 8.dp.toPx(),
                            center = Offset(indicatorX, indicatorY)
                        )
                        // Knob highlight
                        drawCircle(
                            color = Color.White.copy(alpha = 0.3f),
                            radius = 4.dp.toPx(),
                            center = Offset(indicatorX - 2.dp.toPx(), indicatorY - 2.dp.toPx())
                        )
                    }

                    // Layer 6: Center circle (display area)
                    val centerRadius = dialRadius * 0.55f
                    // Center shadow
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.3f),
                        radius = centerRadius,
                        center = Offset(center.x + 2.dp.toPx(), center.y + 2.dp.toPx())
                    )
                    // Center fill
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF303030), centerColor),
                            center = Offset(center.x - 10.dp.toPx(), center.y - 10.dp.toPx()),
                            radius = centerRadius
                        ),
                        radius = centerRadius,
                        center = center
                    )

                    // Layer 7: Gloss overlay (top highlight)
                    drawArc(
                        brush = Brush.verticalGradient(
                            colors = listOf(glossHighlight, Color.Transparent),
                            startY = center.y - outerRadius,
                            endY = center.y
                        ),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(center.x - outerRadius + 8.dp.toPx(), center.y - outerRadius + 8.dp.toPx()),
                        size = Size((outerRadius - 8.dp.toPx()) * 2, (outerRadius - 8.dp.toPx()) * 2)
                    )

                    // Tick marks
                    val tickCount = 8
                    for (i in 0..tickCount) {
                        val tickAngle = (135f + (270f / tickCount) * i) * (PI.toFloat() / 180f)
                        val tickInner = arcRadius - arcStrokeWidth / 2 - 6.dp.toPx()
                        val tickOuter = arcRadius - arcStrokeWidth / 2 - 2.dp.toPx()

                        val startX = center.x + tickInner * cos(tickAngle)
                        val startY = center.y + tickInner * sin(tickAngle)
                        val endX = center.x + tickOuter * cos(tickAngle)
                        val endY = center.y + tickOuter * sin(tickAngle)

                        drawLine(
                            color = Color(0xFF555555),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Center content (text overlay)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Target temperature (large)
                    Text(
                        text = if (isOn) "$displayTemp°" else "OFF",
                        style = MaterialTheme.typography.displayMedium,
                        color = if (isOn) Color.White else Color.Gray
                    )

                    // Current temperature (smaller, below)
                    if (isOn && current != null) {
                        Text(
                            text = "Currently ${current}°",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    // Status indicator
                    if (isOn && (isHeating || isCooling)) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isHeating) {
                                Icon(
                                    Icons.Default.Whatshot,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = TrailCurrentOutboundColors.heating
                                )
                                Text(
                                    text = "Heating",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TrailCurrentOutboundColors.heating
                                )
                            } else {
                                Icon(
                                    Icons.Default.AcUnit,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = TrailCurrentOutboundColors.cooling
                                )
                                Text(
                                    text = "Cooling",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TrailCurrentOutboundColors.cooling
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LightCard(
    light: Light,
    onToggle: () -> Unit,
    onLongPress: () -> Unit
) {
    val isOn = light.state == 1
    // Convert 0-255 to 0-100%
    val brightnessPercent = (light.brightness * 100 / 255).coerceIn(0, 100)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isOn)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .height(100.dp)
            .combinedClickable(
                onClick = onToggle,
                onLongClick = onLongPress
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = if (isOn) Icons.Default.LightMode else Icons.Default.Lightbulb,
                contentDescription = null,
                tint = if (isOn)
                    Color(0xFFFFC107)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = light.name ?: "Light ${light.id}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isOn)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (isOn) "$brightnessPercent%" else "Off",
                style = MaterialTheme.typography.labelSmall,
                color = if (isOn)
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun BrightnessDialog(
    light: Light,
    onDismiss: () -> Unit,
    onBrightnessChange: (Int) -> Unit
) {
    // Convert 0-255 server brightness to 10-100% slider range
    val serverPercent = (light.brightness * 100 / 255).coerceIn(10, 100)

    // Local slider state — initialized once when dialog opens, then user-controlled
    var sliderPercent by remember { mutableFloatStateOf(serverPercent.toFloat()) }

    // Track the last value we actually sent to avoid duplicate API calls.
    // Each API call is a CAN toggle, so duplicates cause on/off flicker.
    var lastSentPercent by remember { mutableStateOf(serverPercent) }

    val currentOnBrightnessChange by rememberUpdatedState(onBrightnessChange)

    // Debounced send: fires 300ms after user stops moving the slider
    val roundedPercent = sliderPercent.roundToInt()
    LaunchedEffect(roundedPercent) {
        if (roundedPercent != lastSentPercent) {
            kotlinx.coroutines.delay(300)
            lastSentPercent = roundedPercent
            currentOnBrightnessChange(roundedPercent)
        }
    }

    AlertDialog(
        onDismissRequest = {
            // Only send if the current value hasn't been sent yet
            val current = sliderPercent.roundToInt()
            if (current != lastSentPercent) {
                onBrightnessChange(current)
            }
            onDismiss()
        },
        title = { Text("${light.name ?: "Light ${light.id}"} Brightness") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "$roundedPercent%",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Slider(
                    value = sliderPercent,
                    onValueChange = { sliderPercent = it },
                    valueRange = 10f..100f
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Only send if the current value hasn't been sent yet
                val current = sliderPercent.roundToInt()
                if (current != lastSentPercent) {
                    onBrightnessChange(current)
                }
                onDismiss()
            }) {
                Text("Done")
            }
        }
    )
}
