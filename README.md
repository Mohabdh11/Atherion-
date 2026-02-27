# Aetherion Mobile NOC — Android Application

**Developer:** Mohammad Abdalftah Ibrahime  
**Platform:** Aetherion v8 OSS/AIOps Backend  
**Architecture:** MVVM + Clean Architecture  
**Language:** Kotlin · Jetpack Compose · Material 3

---

## Project Overview

Carrier-Grade, Enterprise-Level Android NOC application for telecom operators and government infrastructure environments. Fully integrated with the Aetherion v8 backend REST APIs.

---

## Project Structure

```
AetherionNOC/
├── app/
│   ├── build.gradle.kts                    # Build config, flavors, dependencies
│   ├── proguard-rules.pro                  # Production R8/ProGuard rules
│   └── src/main/kotlin/com/aetherion/noc/
│       ├── AetherionApp.kt                 # Application entry point
│       ├── core/
│       │   ├── di/                         # Hilt DI modules
│       │   │   ├── AppModule.kt            # Network, DB, Repository bindings
│       │   │   └── SystemModule.kt         # System services
│       │   ├── logging/
│       │   │   └── AetherionTree.kt        # Production Timber tree (Crashlytics)
│       │   ├── network/
│       │   │   └── NetworkInterceptors.kt  # Auth, refresh, retry, rate-limit
│       │   ├── notifications/
│       │   │   └── AetherionFirebaseService.kt  # FCM handler + deep links
│       │   └── security/
│       │       └── SecurityManager.kt      # Encrypted storage, biometric, session
│       ├── data/
│       │   ├── local/
│       │   │   └── AetherionDatabase.kt    # Room DB: entities + DAOs
│       │   ├── remote/
│       │   │   ├── api/
│       │   │   │   └── AetherionApi.kt     # Retrofit interfaces (all endpoints)
│       │   │   └── dto/
│       │   │       ├── Dtos.kt             # Kotlinx Serialization DTOs
│       │   │       └── Mappers.kt          # DTO → Domain model mappers
│       │   └── repository/
│       │       ├── AuthRepositoryImpl.kt   # Auth repository + token refresh
│       │       └── RepositoryImpls.kt      # Dashboard, Alert, Device, Topology, AI, Geo
│       ├── domain/
│       │   ├── model/
│       │   │   └── Models.kt               # Pure domain models
│       │   ├── repository/
│       │   │   └── Repositories.kt         # Repository interfaces (Clean Architecture)
│       │   └── usecase/
│       │       └── UseCases.kt             # One use case per operation
│       └── presentation/
│           ├── MainActivity.kt             # Entry + bottom nav
│           ├── auth/
│           │   ├── AuthViewModel.kt
│           │   └── LoginScreen.kt
│           ├── dashboard/
│           │   ├── DashboardViewModel.kt
│           │   └── DashboardScreen.kt
│           ├── alerts/
│           │   ├── AlertViewModel.kt
│           │   └── AlertCenterScreen.kt
│           ├── device/
│           │   └── DeviceDetailScreen.kt   # ViewModel + Screen (combined)
│           ├── topology/
│           │   └── TopologyScreen.kt       # Interactive Canvas graph
│           ├── ai/
│           │   └── AiInsightsScreen.kt
│           ├── geo/
│           │   └── GeoNetworkScreen.kt     # Google Maps integration
│           └── common/
│               ├── navigation/
│               │   └── NavGraph.kt         # Navigation routes + deep links
│               └── theme/
│                   └── Theme.kt            # Material 3 dark theme + severity colors
├── gradle/
│   └── libs.versions.toml                  # Version catalog
└── settings.gradle.kts
```

---

## Architecture

```
┌─────────────────────────────────────────────────┐
│              PRESENTATION LAYER                 │
│   Compose UI ← ViewModel ← Use Cases           │
├─────────────────────────────────────────────────┤
│               DOMAIN LAYER                      │
│   Models · Repository Interfaces · Use Cases   │
├─────────────────────────────────────────────────┤
│                DATA LAYER                       │
│   Repository Impls · Remote DTOs · Room Cache  │
└─────────────────────────────────────────────────┘
        ↕ Retrofit (TLS 1.3 + Cert Pinning)
┌─────────────────────────────────────────────────┐
│         AETHERION v8 BACKEND                    │
│   FastAPI · PostgreSQL · Redis · Kafka          │
└─────────────────────────────────────────────────┘
```

**Data flows:**
- ViewModel calls Use Case → Use Case calls Repository → Repository calls API (network-first, Room fallback)
- Kotlin Flow for reactive state: `StateFlow` in ViewModels, `PagingData<T>` for large lists
- Session state monitored via `SecurityManager.sessionState: StateFlow<SessionState>`

---

## Security Architecture

| Control | Implementation |
|--------|----------------|
| Transport | HTTPS only · TLS 1.3 enforced via `network_security_config.xml` |
| Cert Pinning | SHA-256 pins via `CertificatePinner` in OkHttp (configurable per env) |
| Token Storage | `EncryptedSharedPreferences` with AES256-GCM master key |
| Token Refresh | `TokenRefreshInterceptor` → auto-refresh on HTTP 401 |
| Session Timeout | Configurable per build flavor (15m prod / 30m staging / 1h dev) |
| Biometric | `BiometricPrompt` with Fingerprint + Face + Device Credential fallback |
| RBAC | `UserProfile.hasRole()` / `hasPermission()` for UI-layer enforcement |
| Logging | Production: Crashlytics only · PII scrubbed · No tokens in logs |
| Backup | `data_extraction_rules.xml` excludes all sensitive files from device backup |
| ProGuard | R8 aggressive optimisation · `android.util.Log` stripped in release |

---

## Build Flavors

| Flavor | Base URL | Session Timeout | Refresh Interval |
|--------|----------|-----------------|------------------|
| `dev` | `https://dev-api.aetherion.internal/api/v8/` | 60 min | 30s |
| `staging` | `https://staging-api.aetherion.internal/api/v8/` | 30 min | 15s |
| `prod` | `https://api.aetherion.internal/api/v8/` | 15 min | 10s |

---

## Steps to Run

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17+
- Android SDK API 35
- Google Maps API key
- Firebase project with `google-services.json`

### 1. Clone & Configure

```bash
git clone <repo>
cd AetherionNOC
```

### 2. Add Secrets

Create `local.properties` (never commit):
```properties
# Backend certificate pins (get from your PKI team)
CERT_PIN_1=sha256/YOUR_BACKEND_CERT_SHA256_1=
CERT_PIN_2=sha256/YOUR_BACKUP_CERT_SHA256=

# Maps
MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY

# Release signing (for production builds)
KEYSTORE_PATH=/path/to/aetherion-release.jks
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=aetherion
KEY_PASSWORD=your_key_password
```

### 3. Add Firebase Config

Place your `google-services.json` in `app/`.

### 4. Add Internal CA Certificate

Place your Aetherion backend CA certificate at:
```
app/src/main/res/raw/aetherion_ca.pem
```
(PEM format, Base64-encoded)

### 5. Build and Run

```bash
# Dev build (debug)
./gradlew assembleDevDebug

# Staging build
./gradlew assembleStagingRelease

# Production release
./gradlew assembleProdRelease

# Run unit tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest
```

### 6. Install on Device

```bash
adb install -r app/build/outputs/apk/dev/debug/app-dev-debug.apk
```

---

## API Endpoints Used

| Endpoint | Module |
|----------|--------|
| `POST /auth/login` | Authentication |
| `POST /auth/refresh` | Token refresh |
| `GET /auth/me` | User profile |
| `GET /tenants/{id}/dashboard` | Executive dashboard |
| `GET /alarms` | Alert center (paginated) |
| `POST /alarms/{id}/acknowledge` | Alert actions |
| `POST /alarms/{id}/escalate` | Alert escalation |
| `GET /nodes` | Device inventory |
| `GET /nodes/{id}` | Device detail |
| `GET /nodes/{id}/metrics` | Historical metrics |
| `GET /topology` | Topology graph |
| `GET /ai/insights` | AI anomaly/prediction feed |
| `GET /geo/status` | Geographic network status |

---

## FCM Notification Payload Format

```json
{
  "data": {
    "alert_id": "alert-uuid",
    "severity": "CRITICAL",
    "device": "core-router-01",
    "message": "BGP session dropped",
    "action": "ALERT"
  },
  "notification": {
    "title": "CRITICAL Alert",
    "body": "BGP session dropped on core-router-01"
  }
}
```

Silent refresh:
```json
{
  "data": {
    "action": "SILENT_REFRESH",
    "scope": "dashboard"
  }
}
```

Deep link format: `aetherion://noc/alert/{alertId}`

---

## Environment Variables (CI/CD)

```bash
KEYSTORE_PATH          # Path to .jks signing keystore
KEYSTORE_PASSWORD      # Keystore password
KEY_ALIAS              # Key alias
KEY_PASSWORD           # Key password
MAPS_API_KEY           # Google Maps API key
FIREBASE_APP_ID        # Firebase app ID
```

---

## Tech Stack Summary

| Category | Technology |
|----------|-----------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt 2.51 |
| Networking | Retrofit 2.11 + OkHttp 4.12 |
| Serialization | Kotlinx Serialization 1.7 |
| Local DB | Room 2.6 + Paging 3 |
| Security | EncryptedSharedPreferences + BiometricPrompt |
| Push | Firebase Cloud Messaging |
| Maps | Google Maps Compose 4.4 |
| Crash Reporting | Firebase Crashlytics |
| Logging | Timber (Crashlytics in prod) |
| Min SDK | API 26 (Android 8.0) |
| Target SDK | API 35 (Android 15) |

---

*Aetherion Mobile NOC — Carrier-Grade Enterprise Network Operations*  
*Developer: Mohammad Abdalftah Ibrahime*
