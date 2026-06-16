# Inioli Stock Management

Inioli Stock Management is an Android app for operational stock workflows. It is built with Kotlin and Jetpack Compose, authenticates users against the Inioli backend, loads stock movement data from remote APIs, and currently focuses on the receive-stock flow with barcode-based verification.

At the moment, the most complete path in the app is: sign in, open **Receive Stock**, choose a movement type, review available stock journeys, open a movement detail, and scan product barcodes to compare required quantities against what has been scanned during the current session.

## Current Status

- Implemented: login, persisted user session, receive-stock list, receive-stock detail, barcode scanning, quantity progress display, error handling, and unit tests for core logic.
- Partially implemented: receive-stock scanning is available as an in-app verification flow, but there is no submit/save API call wired to send scan results back to the backend yet.
- Placeholder screens: Sale, Return, and Stock Adjustment are routed in the app but do not contain business logic yet.

## Features

- User authentication against the Inioli login API.
- Local session persistence using DataStore so users stay signed in across app restarts.
- Home screen with entry points for Receive Stock, Sale, Return, and Stock Adjustment.
- Receive-stock movement type selector with fallback handling when the preferred movement type is unavailable.
- Stock journey list with status, route, notes, quantity, received quantity, and item count.
- Stock movement detail view with product-level barcode matching.
- CameraX + ML Kit barcode scanning from the device camera.
- In-session scan counting, remaining quantity calculation, and overscan detection.
- User-friendly error messages for auth, timeout, and connectivity failures.

## Tech Stack

- Kotlin
- Jetpack Compose + Material 3
- AndroidX Navigation Compose
- AndroidX Lifecycle ViewModel
- AndroidX DataStore Preferences
- CameraX
- Google ML Kit Barcode Scanning
- Gradle Kotlin DSL

## Architecture Overview

The app follows a lightweight layered structure:

- `ui/`: Compose screens, shared UI components, view models, and user-facing state.
- `data/auth/`: login models, session persistence, remote auth requests, and auth repository.
- `data/stockjourney/`: movement type, stock journey, detail models, remote data source, and repository.
- `data/remote/`: shared `HttpURLConnection` and JSON request helpers.
- `InioliApplication` + `AppContainer`: simple app-level dependency wiring.

This keeps the UI focused on rendering and interaction, while repositories and remote data sources handle API communication.

## Supported User Flow

1. The user signs in with username and password.
2. The app stores the returned access token and username in DataStore.
3. The home screen exposes the available operational modules.
4. The user opens **Receive Stock**.
5. The app loads movement types and stock journeys from the backend.
6. The user opens a stock movement detail screen.
7. The user scans product barcodes with the camera.
8. The app highlights the matched product and updates scanned and remaining quantities for the current session.

## Important Limitations

- Receive-stock scanning is currently read-only from a backend perspective. The app does not yet submit scanned counts back to the server.
- Scanned quantities are stored in view-model state for the current app session and are not persisted as a durable receive record.
- Sale, Return, and Stock Adjustment are placeholder screens at this stage.

## Requirements

- Android Studio
- Android SDK installed locally
- Android device or emulator running API 24 or higher

The project includes a Gradle wrapper, so no separate Gradle installation is required.

## Configuration

Runtime configuration is read from `local.properties` or Gradle properties in `app/build.gradle.kts`.

Example `local.properties`:

```properties
sdk.dir=C\:\\Users\\<your-user>\\AppData\\Local\\Android\\Sdk
INIOLI_LOGIN_BASE_URL=https://lowderancorp.com/inioli/server/api/
INIOLI_LOGIN_BEARER_TOKEN=
INIOLI_STOCK_MOVEMENT_BASE_URL=https://lowderancorp.com/inioli/server/api/
```

### Configuration Notes

- `INIOLI_LOGIN_BASE_URL` is used for `Login.php`.
- `INIOLI_LOGIN_BEARER_TOKEN` is optional and is only sent when non-empty.
- `INIOLI_STOCK_MOVEMENT_BASE_URL` is used for stock movement and stock journey endpoints.
- `local.properties` is git-ignored and should not be committed.

### Emulator and Local Backend Notes

- When testing a backend running on your own machine from the Android emulator, use `10.0.2.2` instead of `localhost`.
- The app allows cleartext HTTP only for `10.0.2.2`, `127.0.0.1`, and `localhost`.
- Other hosts should use HTTPS.

## Backend API Expectations

The current implementation expects these endpoints relative to the configured base URLs:

- `POST Login.php`
- `GET GetMovementTypes.php`
- `GET GetStockJourneyByMovementType.php?movement_type_code=...`
- `GET GetStockJourneyDetail.php?stock_journey_id=...`

### Expected Login Response

The login response should include:

- `access_token`
- `username`

The stock journey endpoints are expected to return movement types, journey lists, and journey detail payloads compatible with the current models in `data/stockjourney/`.

## Getting Started

1. Open the project in Android Studio.
2. Make sure your local configuration values are present in `local.properties` or Gradle properties.
3. Sync the project with the Gradle wrapper.
4. Start an emulator or connect a physical Android device.
5. Run the `app` configuration.
6. Grant camera permission when opening a receive-stock detail screen.

## Running Tests

Run unit tests with:

```powershell
.\gradlew.bat testDebugUnitTest
```

If your shell does not already have Java configured, you can point it to Android Studio's bundled runtime first:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat testDebugUnitTest
```

Current unit-test coverage includes:

- movement type fallback selection
- quantity progress and overscan formatting
- user-facing error message mapping

## Project Structure

```text
app/src/main/java/com/lowderancorp/inioli/
|-- data/
|   |-- auth/
|   |-- remote/
|   `-- stockjourney/
|-- ui/
|   |-- components/
|   |-- screens/
|   `-- theme/
|-- InioliApplication.kt
`-- MainActivity.kt
```

## Suggested Next Steps

- Implement submit/save behavior for receive-stock scan results.
- Persist receive progress locally if the workflow needs resume support.
- Build the Sale, Return, and Stock Adjustment modules.
- Add instrumentation and UI tests for the primary flows.
