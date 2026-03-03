package com.trailcurrent.app.ui.screens.map

import android.view.Gravity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailcurrent.app.data.model.GnssDetails
import com.trailcurrent.app.data.model.GpsAltitude
import com.trailcurrent.app.data.model.GpsLatLon
import com.trailcurrent.app.data.websocket.WebSocketEvent
import com.trailcurrent.app.data.websocket.WebSocketService
import com.trailcurrent.app.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.module.http.HttpRequestUtil
import javax.inject.Inject

data class MapUiState(
    val position: GpsLatLon? = null,
    val altitude: GpsAltitude? = null,
    val gnssDetails: GnssDetails? = null,
    val serverUrl: String = "",
    val apiKey: String? = null,
    val darkMode: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val webSocketService: WebSocketService,
    private val preferencesManager: PreferencesManager,
    val okHttpClient: OkHttpClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeWebSocketEvents()
        observeDarkMode()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                serverUrl = preferencesManager.getServerUrl(),
                apiKey = preferencesManager.getApiKey(),
                darkMode = preferencesManager.getDarkMode()
            )
        }
    }

    private fun observeDarkMode() {
        viewModelScope.launch {
            preferencesManager.darkModeFlow.collect { darkMode ->
                _uiState.value = _uiState.value.copy(darkMode = darkMode)
            }
        }
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            webSocketService.events.collect { event ->
                when (event) {
                    is WebSocketEvent.LatLon -> {
                        _uiState.value = _uiState.value.copy(position = event.position)
                    }
                    is WebSocketEvent.Altitude -> {
                        _uiState.value = _uiState.value.copy(altitude = event.altitude)
                    }
                    is WebSocketEvent.GnssDetails -> {
                        _uiState.value = _uiState.value.copy(gnssDetails = event.details)
                    }
                    else -> {}
                }
            }
        }
    }

    fun getStyleUrl(serverUrl: String, darkMode: Boolean): String {
        val baseUrl = serverUrl.trimEnd('/')
        val styleName = if (darkMode) "3d-dark" else "3d"
        return "$baseUrl/styles/$styleName/style.json"
    }
}

@Composable
fun Compass(
    courseOverGround: Double,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier.size(80.dp)) {
        val canvasSize = size.minDimension
        val center = Offset(size.width / 2, size.height / 2)
        val radius = canvasSize / 2 - 8.dp.toPx()

        // Draw outer circle (compass ring)
        drawCircle(
            color = surfaceColor,
            radius = radius + 4.dp.toPx(),
            center = center
        )
        drawCircle(
            color = onSurfaceColor.copy(alpha = 0.3f),
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )

        // Draw cardinal direction markers
        val cardinals = listOf("N", "E", "S", "W")
        val angles = listOf(0f, 90f, 180f, 270f)

        cardinals.forEachIndexed { index, cardinal ->
            val angle = Math.toRadians((angles[index] - 90).toDouble())
            val markerRadius = radius - 10.dp.toPx()
            val x = center.x + (markerRadius * kotlin.math.cos(angle)).toFloat()
            val y = center.y + (markerRadius * kotlin.math.sin(angle)).toFloat()

            val textStyle = TextStyle(
                fontSize = 10.sp,
                color = if (cardinal == "N") primaryColor else onSurfaceColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            val textLayout = textMeasurer.measure(cardinal, textStyle)
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(
                    x - textLayout.size.width / 2,
                    y - textLayout.size.height / 2
                )
            )
        }

        // Draw heading arrow (rotated by course)
        rotate(degrees = courseOverGround.toFloat(), pivot = center) {
            val arrowPath = Path().apply {
                // Arrow pointing up (north direction)
                moveTo(center.x, center.y - radius + 20.dp.toPx())
                lineTo(center.x - 8.dp.toPx(), center.y)
                lineTo(center.x, center.y - 6.dp.toPx())
                lineTo(center.x + 8.dp.toPx(), center.y)
                close()
            }
            drawPath(arrowPath, color = primaryColor)

            // Draw tail
            val tailPath = Path().apply {
                moveTo(center.x, center.y - 6.dp.toPx())
                lineTo(center.x - 5.dp.toPx(), center.y + radius - 24.dp.toPx())
                lineTo(center.x + 5.dp.toPx(), center.y + radius - 24.dp.toPx())
                close()
            }
            drawPath(tailPath, color = onSurfaceColor.copy(alpha = 0.4f))
        }

        // Draw center dot
        drawCircle(
            color = onSurfaceColor,
            radius = 3.dp.toPx(),
            center = center
        )
    }
}

@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initialize MapLibre synchronously before creating MapView
    remember {
        MapLibre.getInstance(context)
        // Configure OkHttpClient for MapLibre (with SSL and auth support)
        HttpRequestUtil.setOkHttpClient(viewModel.okHttpClient)
    }

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var is3DMode by remember { mutableStateOf(false) }

    // Keep a reference to current position that can be read from callbacks
    val currentPositionState = rememberUpdatedState(uiState.position)

    // Helper function to add marker to map
    fun addMarkerToMap(map: MapLibreMap, position: GpsLatLon) {
        map.annotations.forEach { map.removeAnnotation(it) }
        val latLng = LatLng(position.latitude, position.longitude)
        map.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Vehicle Location")
        )
    }

    // Update style when dark mode changes
    LaunchedEffect(uiState.darkMode, uiState.serverUrl) {
        mapLibreMap?.let { map ->
            if (uiState.serverUrl.isNotBlank() &&
                uiState.serverUrl != PreferencesManager.DEFAULT_SERVER_URL) {
                val styleUrl = viewModel.getStyleUrl(uiState.serverUrl, uiState.darkMode)
                map.setStyle(styleUrl) {
                    // Re-add marker after style loads - read current position from state
                    currentPositionState.value?.let { pos ->
                        addMarkerToMap(map, pos)
                    }
                }
            }
        }
    }

    // Update marker and camera when position changes
    LaunchedEffect(uiState.position) {
        uiState.position?.let { pos ->
            mapLibreMap?.let { map ->
                // Only add marker if style is loaded
                if (map.style != null) {
                    addMarkerToMap(map, pos)
                    // Animate camera to position
                    map.animateCamera(
                        CameraUpdateFactory.newLatLng(LatLng(pos.latitude, pos.longitude))
                    )
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    getMapAsync { map ->
                        mapLibreMap = map

                        // Set initial camera position (center of USA)
                        map.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(39.8283, -98.5795))
                            .zoom(4.0)
                            .build()

                        // Load style when server URL is available
                        if (uiState.serverUrl.isNotBlank() &&
                            uiState.serverUrl != PreferencesManager.DEFAULT_SERVER_URL) {
                            val styleUrl = viewModel.getStyleUrl(uiState.serverUrl, uiState.darkMode)
                            map.setStyle(styleUrl) {
                                // Add marker when style loads (handles initial load and navigation back)
                                currentPositionState.value?.let { pos ->
                                    map.annotations.forEach { map.removeAnnotation(it) }
                                    val latLng = LatLng(pos.latitude, pos.longitude)
                                    map.addMarker(
                                        MarkerOptions()
                                            .position(latLng)
                                            .title("Vehicle Location")
                                    )
                                    // Center on vehicle position
                                    map.animateCamera(
                                        CameraUpdateFactory.newLatLngZoom(latLng, 15.0)
                                    )
                                }
                            }
                        }

                        // Enable location tracking UI
                        map.uiSettings.apply {
                            isCompassEnabled = true
                            isZoomGesturesEnabled = true
                            isScrollGesturesEnabled = true
                            isRotateGesturesEnabled = true
                            isTiltGesturesEnabled = true
                            setCompassGravity(Gravity.TOP or Gravity.END)
                        }
                    }
                    mapView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { _ ->
                // Style is set in factory, no need to update on every recomposition
            }
        )

        // Map controls overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Center on vehicle button
            FloatingActionButton(
                onClick = {
                    uiState.position?.let { pos ->
                        mapLibreMap?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(pos.latitude, pos.longitude),
                                15.0
                            )
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Center on vehicle"
                )
            }

            // Zoom in
            FloatingActionButton(
                onClick = {
                    mapLibreMap?.let { map ->
                        map.animateCamera(CameraUpdateFactory.zoomIn())
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Zoom in"
                )
            }

            // Zoom out
            FloatingActionButton(
                onClick = {
                    mapLibreMap?.let { map ->
                        map.animateCamera(CameraUpdateFactory.zoomOut())
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Zoom out"
                )
            }

            // 2D/3D toggle
            FloatingActionButton(
                onClick = {
                    mapLibreMap?.let { map ->
                        is3DMode = !is3DMode
                        val camPosition = map.cameraPosition
                        val newTilt = if (is3DMode) 45.0 else 0.0
                        map.animateCamera(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.Builder()
                                    .target(camPosition.target)
                                    .zoom(camPosition.zoom)
                                    .bearing(camPosition.bearing)
                                    .tilt(newTilt)
                                    .build()
                            )
                        )
                    }
                },
                containerColor = if (is3DMode)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (is3DMode) Icons.Default.ViewInAr else Icons.Default.Map,
                    contentDescription = if (is3DMode) "Switch to 2D" else "Switch to 3D"
                )
            }
        }

        // Bottom overlay row with position info and compass
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Position info card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Vehicle Position",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    uiState.position?.let { pos ->
                        Text(
                            text = "Lat: ${String.format("%.6f", pos.latitude)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Lon: ${String.format("%.6f", pos.longitude)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } ?: Text(
                        text = "Waiting for GPS...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    uiState.altitude?.let { alt ->
                        Text(
                            text = "Alt: ${String.format("%.0f", alt.altitudeFeet)} ft",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    uiState.gnssDetails?.let { details ->
                        Text(
                            text = "Heading: ${String.format("%.0f", details.courseOverGround)}°",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Compass card
            uiState.gnssDetails?.let { details ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        Compass(
                            courseOverGround = details.courseOverGround
                        )
                    }
                }
            }
        }

        // No position indicator
        if (uiState.position == null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Waiting for GPS signal...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    // Lifecycle management
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView?.onStart()
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                Lifecycle.Event.ON_STOP -> mapView?.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView?.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView?.onDestroy()
        }
    }
}
