<h1 align="center">uFlex Patient Mobile</h1>

<div align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.2-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Hilt-DI-34A853?style=for-the-badge&logo=android&logoColor=white" alt="Hilt" />
  <img src="https://img.shields.io/badge/Gradle-KTS-02303A?style=for-the-badge&logo=gradle&logoColor=white" alt="Gradle KTS" />
  <br />
  <img src="https://img.shields.io/badge/Architecture-Feature--first%20%2F%20Clean-blue?style=flat-square" alt="Feature-first Clean Architecture" />
  <img src="https://img.shields.io/badge/minSdk-24-6B7280?style=flat-square" alt="minSdk 24" />
  <img src="https://img.shields.io/badge/compileSdk-36.1-6B7280?style=flat-square" alt="compileSdk 36.1" />
</div>

---

This repository contains the **uFlex patient mobile app**, a native Android application that lets a patient sign in, review and update their patient profile, and interact with the uFlex telerehabilitation platform from their phone.

The app is part of the broader **uFlex** ecosystem (embedded firmware, edge gateway, REST API, clinic web, and this patient app), and focuses specifically on the **patient-facing** experience.

---

## Overview

The mobile client authenticates the patient against the uFlex REST API, bootstraps a real session by loading the patient identity, and exposes an authenticated area with bottom navigation, a profile screen, and contact-info editing.

Key business notes:

- The authenticated user identity and the business (patient) identity are **not** the same thing: `userId` != `patientId`.
- The `clinicId` coming from the patient profile is normalized in mobile as `tenantId`.
- A successful login is only considered valid **after** the patient profile (`GET patients/me`) is loaded; otherwise the session is cleared and the login is treated as failed.

---

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Kotlin `2.2.10` |
| UI | Jetpack Compose + Material 3 |
| Dependency Injection | Hilt + KSP |
| Navigation | Navigation Compose (typed routes) |
| Networking | Retrofit + OkHttp |
| Serialization | Kotlinx Serialization (JSON) |
| Persistence | DataStore (session) · Room (wired, not yet active) |
| Images | Coil |
| Background work | WorkManager |
| Async | Kotlin Coroutines |
| Build | Gradle (Kotlin DSL) + Version Catalog |

Target configuration:

- `minSdk`: **24**
- `targetSdk`: **36**
- `compileSdk`: **36.1**
- Java/Kotlin compatibility: **Java 11** (with core library desugaring)

---

## Architecture

The project follows a **feature-first** structure with clean separation between domain, data, and presentation layers.

```text
app/src/main/java/com/kiniot/uflex/
├── core/        # shared cross-feature infrastructure
│   ├── designsystem/   # theme, colors, shared feedback components
│   ├── di/             # shared DI modules (network, storage)
│   ├── navigation/     # root navigation + app navigator
│   ├── network/        # Retrofit, OkHttp, interceptors, SafeApiCaller
│   ├── result/         # AppResult, AppError, error codes
│   ├── serializers/    # date/time serializers
│   ├── session/        # persistent session storage
│   └── ui/             # shared UI primitives (UiText)
└── features/    # business features
    ├── auth/      # sign in + session bootstrap
    ├── profile/   # patient profile read/update + sign out
    ├── home/      # authenticated Home tab
    ├── main/      # authenticated shell (top/bottom bars, internal NavHost)
    └── splash/    # decides between auth and main flows
```

Each feature follows a consistent shape:

```text
features/<feature>/
├── data/        # local, remote, mapper, repository
├── di/          # feature dependency modules
├── domain/      # model, repository contracts, use cases
├── navigation/  # routes (when the feature owns them)
└── presentation # screens, view models, UI state
```

### Data flow

```text
ApiService (Response<Dto>)
  -> RemoteDataSource (AppResult<Dto>)
  -> RepositoryImpl (maps Dto -> domain model)
  -> Repository (AppResult<DomainModel>)
  -> ViewModel (UiState)
```

For a deeper breakdown of layer responsibilities, naming conventions, and navigation rules, see **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**.

---

## Getting Started

### Prerequisites

- **Android Studio** (latest stable, Compose-ready)
- **JDK 17+** to run Gradle
- An Android emulator or device running **API 24+**
- A running instance of the **uFlex REST API** reachable from the device

### Configuration

The API base URL is defined in [`app/build.gradle.kts`](app/build.gradle.kts) and currently points to:

```
http://10.0.2.2:8080/api/v1/
```

`10.0.2.2` is the loopback alias for the host machine from inside the Android emulator. Local cleartext traffic is allowed on purpose through `network_security_config`; do not "fix" this by blindly changing URLs.

### Build & Run

```bash
# Fast compile check
./gradlew :app:compileDebugKotlin

# Assemble a debug APK
./gradlew :app:assembleDebug

# Install on a connected device/emulator
./gradlew :app:installDebug
```

> A full step-by-step setup guide is available in **[docs/SETUP.md](docs/SETUP.md)**.

---

## Internationalization

The app avoids hardcoded user-facing strings and uses `UiText` in the presentation layer. Strings are split per feature:

- `res/values/strings_auth.xml`
- `res/values/strings_profile.xml`
- `res/values/strings_main.xml`
- `res/values/strings_home.xml`
- `res/values/strings_core.xml` (shared)

Latin American Spanish lives under `res/values-b+es+419/`.

---

## Documentation

- [Architecture Guide](docs/ARCHITECTURE.md)
- [Setup Guide](docs/SETUP.md)
- [E2E Testing Runbook](docs/E2E-TESTING.md) — run the full kit→edge→backend→mobile loop from scratch
- [Execution Contract](docs/EXECUTION-CONTRACT.md) — authoritative cross-repo design & status
- [Mobile Progress](docs/PROGRESS.md)
- [Next Steps](docs/NEXT-STEPS.md)
- [Agent / Operational Notes](AGENTS.md)

---

## Project Status

Active development. Implemented flows today:

- Authentication (sign in) with real session bootstrap
- Patient profile read/update and sign out
- Authenticated shell with bottom navigation
- Home tab placeholder

Sign up and email verification are intentionally not implemented yet; auth is currently oriented around sign in (and later forgot password).
