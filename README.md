# TrailCurrent Android App

Native Android application for the [TrailCurrent](https://trailcurrent.com) vehicle monitoring and control system.

## Overview

This Android app provides a native mobile interface for the TrailCurrent in-vehicle control system. It communicates with the TrailCurrent backend via REST API for commands and WebSockets for real-time status updates.

## Features

- **Home Dashboard**: Nest-style thermostat dial and light controls with brightness adjustment
- **Trailer Monitoring**: Level indicators and GPS/GNSS details with heading
- **Energy Monitoring**: Solar input, battery status, and charge information
- **Water Tanks**: Fresh, grey, and black water tank levels
- **Air Quality**: Temperature, humidity, IAQ index, and CO2 monitoring
- **Map View**: Real-time vehicle location with MapLibre vector tiles, 2D/3D modes, and compass
- **Settings**: Server configuration, dark/light theme, timezone, and clock format

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Hilt
- **Networking**: Retrofit + OkHttp (REST), OkHttp WebSocket (real-time)
- **Local Storage**: DataStore Preferences
- **Maps**: MapLibre GL (vector tiles from tileserver-gl)
- **Authentication**: API key via Authorization header
- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)

## Configuration

On first launch, the app will prompt for:

1. **Server URL**: The base URL of your TrailCurrent server (e.g., `https://your-server.com`)
2. **API Key**: Authentication key for the server

The WebSocket URL is automatically derived from the server URL. These settings can be changed later in the Settings screen.

## Building

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK with API level 34

### Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
```

### Opening in Android Studio

1. Open Android Studio
2. Select "Open an existing project"
3. Navigate to this directory and click "Open"
4. Wait for Gradle sync to complete
5. Run on emulator or connected device

## Project Structure

```
app/src/main/java/com/trailcurrent/app/
├── TrailCurrentApp.kt          # Application class (Hilt entry point)
├── MainActivity.kt             # Main entry point, navigation, lifecycle management
├── data/
│   ├── api/
│   │   ├── ApiService.kt       # Retrofit API interface
│   │   └── AuthInterceptor.kt  # API key header injection
│   ├── model/
│   │   └── Models.kt           # Data classes
│   ├── repository/
│   │   └── VehicleRepository.kt # Vehicle data access
│   └── websocket/
│       └── WebSocketService.kt  # Real-time updates with auto-reconnect
├── di/
│   └── AppModule.kt            # Hilt dependency injection
├── ui/
│   ├── components/
│   │   └── BottomNavBar.kt     # Navigation bar
│   ├── navigation/
│   │   └── Navigation.kt       # Screen routes
│   ├── screens/
│   │   ├── airquality/         # Air quality monitoring
│   │   ├── energy/             # Energy/battery status
│   │   ├── home/               # Dashboard with thermostat and lights
│   │   ├── map/                # GPS location map with compass
│   │   ├── settings/           # Server config and app settings
│   │   ├── trailer/            # Level and GNSS info
│   │   └── water/              # Tank levels
│   └── theme/
│       ├── Theme.kt            # Material theme with dark mode
│       └── Type.kt             # Typography
└── util/
    └── PreferencesManager.kt   # Local settings storage (DataStore)
```

## API Integration

The app connects to the TrailCurrent backend using:

### REST API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/thermostat` | GET/PUT | Thermostat state |
| `/api/lights` | GET | List all lights |
| `/api/lights/:id` | PUT | Update single light (state + brightness) |
| `/api/trailer/level` | GET | Trailer level data |
| `/api/energy` | GET | Energy/battery status |
| `/api/water` | GET | Water tank levels |
| `/api/airquality` | GET | Air quality data |
| `/api/settings` | GET/PUT | User settings |

### WebSocket Events

The app maintains a persistent WebSocket connection for real-time updates with automatic reconnection (exponential backoff, resumes on app foreground):

- `thermostat` - Thermostat state changes
- `light` - Individual light updates
- `energy` - Battery/solar status
- `water` - Tank level changes
- `airquality` - IAQ and CO2 readings
- `temphumid` - Temperature and humidity
- `latlon` - GPS coordinates
- `alt` - Altitude data
- `gnss_details` - Satellite and heading info
- `level` - Trailer level sensor

### Map Tiles

The map uses MapLibre GL with vector tiles served by tileserver-gl. Available styles:

- `3d` - Light theme with 3D perspective
- `3d-dark` - Dark theme with 3D perspective

Style URLs follow the pattern: `{serverUrl}/styles/{styleName}/style.json`

## Security

- API key stored securely using Android DataStore
- All network communication uses HTTPS/WSS
- WebSocket connection authenticated with API key header
- Custom SSL trust manager for self-signed certificates in development

## License

MIT License - See LICENSE file for details.
