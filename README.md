# TrailCurrent Android App

Native Android application for the TrailCurrent vehicle monitoring and control system.

## Overview

This Android app provides a native mobile interface for the TrailCurrent cloud-based vehicle control system. It communicates with the TrailCurrentCloud backend via REST API and WebSockets for real-time updates.

## Features

- **Authentication**: Secure login with session management
- **Home Dashboard**: Thermostat control and lighting management
- **Trailer Monitoring**: Level indicators and GPS/GNSS details
- **Energy Monitoring**: Solar input, battery status, and charge information
- **Water Tanks**: Fresh, grey, and black water tank levels
- **Air Quality**: Temperature, humidity, IAQ index, and CO2 monitoring
- **Map View**: Real-time vehicle location with MapLibre (vector tiles)
- **Settings**: Server configuration, theme options, and account management

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Hilt
- **Networking**: Retrofit + OkHttp
- **Real-time Updates**: WebSocket (OkHttp)
- **Local Storage**: DataStore Preferences
- **Maps**: MapLibre GL (vector tiles from tileserver-gl)
- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)

## Configuration

On first launch, the app will prompt you to configure the server connection:

1. **Server URL**: The base URL of your TrailCurrentCloud server (e.g., `https://your-server.com`)

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
├── TrailCurrentApp.kt          # Application class
├── MainActivity.kt             # Main entry point with navigation
├── data/
│   ├── api/
│   │   ├── ApiService.kt       # Retrofit API interface
│   │   └── AuthInterceptor.kt  # Auth token injection
│   ├── model/
│   │   └── Models.kt           # Data classes
│   ├── repository/
│   │   ├── AuthRepository.kt   # Authentication logic
│   │   └── VehicleRepository.kt # Vehicle data access
│   └── websocket/
│       └── WebSocketService.kt  # Real-time updates
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
│   │   ├── home/               # Dashboard with thermostat/lights
│   │   ├── login/              # Authentication
│   │   ├── map/                # GPS location map
│   │   ├── settings/           # App configuration
│   │   ├── trailer/            # Level and GNSS info
│   │   └── water/              # Tank levels
│   └── theme/
│       ├── Theme.kt            # Material theme
│       └── Type.kt             # Typography
└── util/
    └── PreferencesManager.kt   # Local settings storage
```

## API Integration

The app connects to the TrailCurrentCloud backend using:

### REST API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth/login` | POST | User authentication |
| `/api/auth/logout` | POST | End session |
| `/api/auth/check` | GET | Verify auth status |
| `/api/thermostat` | GET/PUT | Thermostat state |
| `/api/lights` | GET | List all lights |
| `/api/lights/:id` | PUT | Update single light |
| `/api/trailer/level` | GET | Trailer level data |
| `/api/energy` | GET | Energy/battery status |
| `/api/water` | GET | Water tank levels |
| `/api/airquality` | GET | Air quality data |
| `/api/settings` | GET/PUT | User settings |

### WebSocket Events

The app subscribes to real-time updates via WebSocket:

- `thermostat` - Thermostat state changes
- `light` - Individual light updates
- `energy` - Battery/solar status
- `water` - Tank level changes
- `airquality` - IAQ and CO2 readings
- `temphumid` - Temperature and humidity
- `latlon` - GPS coordinates
- `alt` - Altitude data
- `gnss_details` - Satellite info
- `level` - Trailer level sensor

## Security

- Authentication tokens stored securely using DataStore
- All network communication uses HTTPS
- WebSocket connection authenticated with bearer token
- No sensitive data logged in release builds

## License

MIT License - See LICENSE file for details.
