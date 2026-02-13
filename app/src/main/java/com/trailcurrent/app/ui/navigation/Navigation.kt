package com.trailcurrent.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null
) {
    object ServerConfig : Screen("server_config", "Server Configuration", Icons.Default.Settings)
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Trailer : Screen("trailer", "Trailer", Icons.Default.DirectionsCar)
    object Energy : Screen("energy", "Energy", Icons.Default.BatteryChargingFull)
    object Water : Screen("water", "Water", Icons.Default.WaterDrop)
    object AirQuality : Screen("air_quality", "Air Quality", Icons.Default.Air)
    object Map : Screen("map", "Map", Icons.Default.Map)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        // Navigation bar items (main screens)
        val navBarItems: List<Screen> by lazy {
            listOf(Home, Trailer, Energy, Water, AirQuality, Map, Settings)
        }

        // All screens
        val allScreens: List<Screen> by lazy {
            listOf(ServerConfig, Home, Trailer, Energy, Water, AirQuality, Map, Settings)
        }
    }
}
