# Setup Guide

Step-by-step guide to build and run the **uFlex patient mobile** app locally.

For architecture and conventions, read [ARCHITECTURE.md](ARCHITECTURE.md) first.

---

## Prerequisites

| Tool | Requirement |
| --- | --- |
| Android Studio | Latest stable, Compose-ready |
| JDK | 17+ (to run Gradle) |
| Android SDK | API 24+ installed |
| Device/Emulator | Running **API 24 (Android 7.0)** or higher |
| uFlex REST API | A running instance reachable from the device |

The project targets:

- `minSdk` **24**
- `targetSdk` **36**
- `compileSdk` **36.1**
- Java/Kotlin compatibility **Java 11** (core library desugaring enabled)

---

## 1. Clone and open

```bash
git clone https://github.com/kiniot/uflex-patient-mobile.git
cd uflex-patient-mobile
```

Open the folder in Android Studio and let it sync the Gradle project. Dependencies and
versions are managed through the version catalog in
[`gradle/libs.versions.toml`](../gradle/libs.versions.toml).

---

## 2. Configure the API base URL

The base URL is **not** an environment file; it is a `buildConfigField` declared in
[`app/build.gradle.kts`](../app/build.gradle.kts):

```kotlin
buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/api/v1/\"")
```

- `10.0.2.2` is the special loopback alias that points to **your host machine** from inside
  the Android emulator. Use it when the API runs on your laptop.
- For a **physical device**, replace it with your machine's LAN IP (e.g.
  `http://192.168.x.x:8080/api/v1/`) and make sure both are on the same network.

> Cleartext (HTTP) traffic is allowed on purpose for local development through
> `res/xml/network_security_config.xml`. This is intentional — do not "fix" it by forcing
> HTTPS for local runs.

---

## 3. Build

```bash
# Fast verification: compile Kotlin without packaging
./gradlew :app:compileDebugKotlin

# Assemble a debug APK
./gradlew :app:assembleDebug
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

---

## 4. Run

With an emulator or device connected:

```bash
./gradlew :app:installDebug
```

Or simply press **Run** in Android Studio with a device selected.

---

## 5. First login

1. Make sure the uFlex REST API is running and reachable at the configured base URL.
2. Launch the app — the splash screen decides between the auth flow and the main area based
   on the locally persisted session.
3. Sign in with valid **patient** credentials.

The login is only considered successful after the app loads the patient profile
(`GET patients/me`) and stores the `patientId`. If that step fails, the session is cleared
and the login is treated as failed. See `SignInAndLoadPatientSessionUseCase`.

---

## Troubleshooting

| Symptom | Likely cause / fix |
| --- | --- |
| Network/connection errors on login | API not running, wrong base URL, or device can't reach the host. Verify `API_BASE_URL`. |
| Works on emulator but not on device | `10.0.2.2` only works on the emulator; use your LAN IP for physical devices. |
| Cleartext blocked | Confirm `network_security_config.xml` still allows local cleartext. |
| Gradle sync fails | Check JDK 17+, and let Android Studio download the required SDK (API 36). |
| Navigation compile errors | Inspect typed routes, `toRoute` imports, and feature DI wiring. |

---

## Useful commands

```bash
./gradlew :app:compileDebugKotlin   # quick compile check
./gradlew :app:assembleDebug        # build debug APK
./gradlew :app:installDebug         # install on device/emulator
./gradlew clean                     # clean build outputs
```
