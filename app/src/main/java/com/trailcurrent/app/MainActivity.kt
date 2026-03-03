package com.trailcurrent.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import com.trailcurrent.app.data.repository.VehicleRepository
import com.trailcurrent.app.data.websocket.WebSocketService
import com.trailcurrent.app.di.ApiServiceFactory
import com.trailcurrent.app.ui.components.BottomNavBar
import com.trailcurrent.app.ui.navigation.Screen
import com.trailcurrent.app.ui.screens.airquality.AirQualityScreen
import com.trailcurrent.app.ui.screens.energy.EnergyScreen
import com.trailcurrent.app.ui.screens.home.HomeScreen
import com.trailcurrent.app.ui.screens.map.MapScreen
import com.trailcurrent.app.ui.screens.settings.ServerConfigScreen
import com.trailcurrent.app.ui.screens.settings.SettingsScreen
import com.trailcurrent.app.ui.screens.trailer.TrailerScreen
import com.trailcurrent.app.ui.screens.water.WaterScreen
import com.trailcurrent.app.ui.theme.TrailCurrentTheme
import com.trailcurrent.app.util.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

data class MainUiState(
    val isLoading: Boolean = true,
    val isConfigured: Boolean = false,
    val hasApiKey: Boolean = false,
    val darkMode: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val vehicleRepository: VehicleRepository,
    private val webSocketService: WebSocketService,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val apiServiceFactory = ApiServiceFactory(okHttpClient, gson)

    init {
        checkInitialState()
        observeDarkMode()
    }

    private fun checkInitialState() {
        viewModelScope.launch {
            val isConfigured = preferencesManager.isConfigured()
            val apiKey = preferencesManager.getApiKey()
            val hasApiKey = !apiKey.isNullOrBlank()
            val darkMode = preferencesManager.getDarkMode()

            if (isConfigured) {
                initializeApiService()
                if (hasApiKey) {
                    webSocketService.connect(null)
                }
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isConfigured = isConfigured,
                hasApiKey = hasApiKey,
                darkMode = darkMode
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

    fun onServerConfigured() {
        viewModelScope.launch {
            initializeApiService()
            _uiState.value = _uiState.value.copy(isConfigured = true)
        }
    }

    fun onApiKeySet(apiKey: String) {
        viewModelScope.launch {
            preferencesManager.setApiKey(apiKey)
            webSocketService.connect(null)
            _uiState.value = _uiState.value.copy(hasApiKey = true)
        }
    }

    private suspend fun initializeApiService() {
        val serverUrl = preferencesManager.getServerUrl()
        val apiService = apiServiceFactory.create(serverUrl)
        vehicleRepository.setApiService(apiService)
    }

    fun onAppForegrounded() {
        webSocketService.reconnectIfNeeded()
    }

    override fun onCleared() {
        super.onCleared()
        webSocketService.destroy()
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            // Reconnect WebSocket when app returns to foreground
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_START) {
                        viewModel.onAppForegrounded()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            TrailCurrentTheme(darkTheme = uiState.darkMode) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    if (uiState.isLoading) {
                        LoadingScreen()
                    } else {
                        MainContent(
                            uiState = uiState,
                            onServerConfigured = viewModel::onServerConfigured,
                            onApiKeySet = viewModel::onApiKeySet
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading...")
        }
    }
}

@Composable
fun ApiKeyDialog(
    onApiKeySet: (String) -> Unit
) {
    var apiKey by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { /* Cannot dismiss without entering key */ },
        icon = {
            Icon(Icons.Default.Key, contentDescription = null)
        },
        title = {
            Text("API Key Required")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Please enter your API key to connect to the server.")

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("Enter your API key") },
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showApiKey) "Hide API Key" else "Show API Key"
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onApiKeySet(apiKey) },
                enabled = apiKey.isNotBlank()
            ) {
                Text("Connect")
            }
        }
    )
}

@Composable
fun MainContent(
    uiState: MainUiState,
    onServerConfigured: () -> Unit,
    onApiKeySet: (String) -> Unit
) {
    // Show API key dialog if configured but no API key
    // Don't render the main app until API key is set so ViewModels load fresh with auth
    if (uiState.isConfigured && !uiState.hasApiKey) {
        ApiKeyDialog(onApiKeySet = onApiKeySet)
        return
    }

    // Show server config if not configured
    if (!uiState.isConfigured) {
        ServerConfigScreen(
            onConfigured = {
                onServerConfigured()
            }
        )
        return
    }

    // Main app with navigation - only rendered when fully configured with API key
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val navBarRoutes = remember { Screen.navBarItems.map { it.route } }
    val showBottomNav = currentDestination?.route in navBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                BottomNavBar(
                    currentRoute = currentDestination?.route,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen()
            }

            composable(Screen.Trailer.route) {
                TrailerScreen()
            }

            composable(Screen.Energy.route) {
                EnergyScreen()
            }

            composable(Screen.Water.route) {
                WaterScreen()
            }

            composable(Screen.AirQuality.route) {
                AirQualityScreen()
            }

            composable(Screen.Map.route) {
                MapScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
