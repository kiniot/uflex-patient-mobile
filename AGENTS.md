# AGENTS.md

## Project
- Native Android app built with Kotlin, Jetpack Compose, Hilt, Navigation Compose, Retrofit, DataStore, and Kotlinx Serialization.
- Business context: this is the **uFlex patient mobile app**. The authenticated mobile user belongs to the patient domain, but authentication identity and business identity are not the same thing.
- `userId` != `patientId`.
- `clinicId` from the patient profile is normalized in mobile as `tenantId`.

## Sources of truth to read first
- Read [docs/ARCHITECTURE.md](/Users/salim/dev/projects/kiniot/uflex-patient-mobile/docs/ARCHITECTURE.md) before creating new features or renaming layers.
- There is no `README.md` in the root at the time of writing; this file is the main operational guide for agent sessions.

## Build and environment
- Most useful fast verification command: `./gradlew :app:compileDebugKotlin`
- Current `compileSdk`: `36.1`
- `minSdk`: `24`
- `API_BASE_URL` lives in [app/build.gradle.kts](/Users/salim/dev/projects/kiniot/uflex-patient-mobile/app/build.gradle.kts) and currently points to `http://10.0.2.2:8080/api/v1/`
- `10.0.2.2` is intentional for the Android emulator.
- The app allows local cleartext traffic through `network_security_config`; do not “fix” this by blindly changing URLs.
- `MainActivity` uses edge-to-edge and the manifest sets `android:windowSoftInputMode="adjustResize"`.

## Overall architecture
- Feature-first structure under `app/src/main/java/com/kiniot/uflex/features/<feature>/`
- Expected feature shape:
  - `data/local|remote|mapper|repository`
  - `di`
  - `domain/model|repository|usecase`
  - `presentation`
  - `navigation` when the feature owns routes
- Shared infrastructure lives in `core/`:
  - `core/network/`: Retrofit, OkHttp, `SafeApiCaller`, `ApiErrorMapper`, `AuthInterceptor`
  - `core/result/`: `AppResult`, `AppError`, `CoreErrorCode`, `AppErrorMessageMapper`
  - `core/session/`: `SessionStore`, `LocalSession`
  - `core/navigation/`: `AppNavigator`, `RootNavGraph`, root routes
  - `core/designsystem/`: theme, extended colors, snackbar
  - `core/ui/UiText.kt`
  - `core/serializers/`: date/time serializers

## Relevant features today
- `features/auth`: sign in, session bootstrap, auth error catalog.
- `features/profile`: patient profile read/update and sign out.
- `features/home`: authenticated Home tab placeholder.
- `features/main`: authenticated shell with top bar, bottom bar, and internal `NavHost`.
- `features/splash`: decides whether to enter auth or main based on local session state.

## Navigation
- Root navigation lives in [RootNavGraph.kt](/Users/salim/dev/projects/kiniot/uflex-patient-mobile/app/src/main/java/com/kiniot/uflex/core/navigation/RootNavGraph.kt) and only decides between:
  - `Splash`
  - `AuthGraph`
  - `MainGraph`
- `MainGraph` does not render final screens directly; it mounts `MainShell`.
- `MainShell` owns its own internal `NavHost` and controls:
  - bottom tabs
  - authenticated area top bar
  - detail screens that hide the bottom bar
- The project uses typed routes with `@Serializable object/data class`, not manual string routes.
- For new authenticated screens, first decide whether they are:
  - a main tab destination
  - or a secondary route inside `MainShell`
- `ProfileRoute` and `EditContactInfoRoute` are current examples of detail screens inside the shell.

## Auth and real session bootstrap
- A successful login does not end at `authentication/sign-in`.
- The real implemented flow is:
  1. `signIn`
  2. `GET patients/me`
  3. store `patientId`
  4. only then consider the session valid
- This orchestration lives in [SignInAndLoadPatientSessionUseCase.kt](/Users/salim/dev/projects/kiniot/uflex-patient-mobile/app/src/main/java/com/kiniot/uflex/features/auth/domain/usecase/SignInAndLoadPatientSessionUseCase.kt)
- If `signIn` succeeds but `patients/me` fails, the session is cleared and login is treated as failed.
- The mobile app does not currently support `sign up` or `verify email`; auth is currently oriented around `sign in` and later `forgot password`.

## Local session
- `SessionStore` in [core/session/SessionStore.kt](/Users/salim/dev/projects/kiniot/uflex-patient-mobile/app/src/main/java/com/kiniot/uflex/core/session/SessionStore.kt) uses DataStore.
- The persisted session currently includes:
  - `userId`
  - `patientId`
  - `email`
  - `roles`
  - `tenantId`
  - `token`
- `AuthInterceptor` reads the token from `SessionStore` and adds `Authorization: Bearer ...` and `Accept: application/json`.
- If a flow changes persisted session data, update it from repository/data source, not from UI code.
- Existing example: `ProfileRepositoryImpl` keeps the session `email` in sync after contact info edits.

## Result and error flow
- Do not use Kotlin's standard `Result` for app architecture.
- The agreed flow is:
  - `ApiService` -> `Response<Dto>`
  - `RemoteDataSource` -> `AppResult<Dto>`
  - `Repository` -> `AppResult<DomainModel>`
  - `ViewModel` -> `UiState`
- Shared types:
  - [AppResult.kt](/Users/salim/dev/projects/kiniot/uflex-patient-mobile/app/src/main/java/com/kiniot/uflex/core/result/AppResult.kt)
  - [AppError.kt](/Users/salim/dev/projects/kiniot/uflex-patient-mobile/app/src/main/java/com/kiniot/uflex/core/result/AppError.kt)
  - [CoreErrorCode.kt](/Users/salim/dev/projects/kiniot/uflex-patient-mobile/app/src/main/java/com/kiniot/uflex/core/result/CoreErrorCode.kt)
- `CoreErrorCode` is sealed, not an enum, because the project needs the dynamic `HTTP_<STATUS>` case.
- `ApiErrorMapper` translates backend JSON into:
  - known core errors
  - or `AppError.Business(code, backendMessage, status)` for business codes
- `SafeApiCaller` centralizes exception/network/HTTP translation into `AppResult.Error`.

## Error catalog
- Core handles shared generic errors:
  - `AUTH_REQUIRED`
  - `ACCESS_DENIED`
  - `BAD_REQUEST`
  - `CONFLICT`
  - `NOT_FOUND`
  - `INTERNAL_SERVER_ERROR`
  - `HTTP_<STATUS>`
- Each feature may have its own business error catalog.
- Auth already follows that pattern:
  - [features/auth/error/AuthErrorCode.kt](/Users/salim/dev/projects/kiniot/uflex-patient-mobile/app/src/main/java/com/kiniot/uflex/features/auth/error/AuthErrorCode.kt)
  - [features/auth/presentation/AuthErrorMessageMapper.kt](/Users/salim/dev/projects/kiniot/uflex-patient-mobile/app/src/main/java/com/kiniot/uflex/features/auth/presentation/AuthErrorMessageMapper.kt)
  - [features/auth/presentation/AuthErrorPresentation.kt](/Users/salim/dev/projects/kiniot/uflex-patient-mobile/app/src/main/java/com/kiniot/uflex/features/auth/presentation/AuthErrorPresentation.kt)
- Practical rule:
  - if the code is framework/infrastructure related, it belongs in core
  - if the code expresses a feature business rule, map it inside that feature

## Snackbar and inline errors
- There is a global snackbar host in [AppNavigator.kt](/Users/salim/dev/projects/kiniot/uflex-patient-mobile/app/src/main/java/com/kiniot/uflex/core/navigation/AppNavigator.kt)
- It is fed by `SnackbarManager`.
- Recommended usage:
  - `inline` for form validation or field-adjacent errors
  - `snackbar` for transient feedback or cross-cutting/global errors
- Auth already uses that split:
  - inline for local validation and errors like invalid credentials
  - snackbar for broader transient/global failures
- Do not mount a separate `SnackbarHost` per screen unless there is a strong reason; the current app already has a global flow.

## i18n
- Avoid hardcoded strings in `ViewModel`s and screens.
- Use `UiText` for presentation-layer messages.
- Strings are split by feature:
  - `res/values/strings_auth.xml`
  - `res/values/strings_profile.xml`
  - `res/values/strings_main.xml`
  - `res/values/strings_home.xml`
  - generic/shared strings in `strings_core.xml`
- Latin American Spanish lives in `values-b+es+419/`.
- When adding a generic error message, first check whether it belongs in `strings_core.xml`.

## DTOs, dates, and enums
- Remote naming convention: `RequestDto` and `ResponseDto`.
- Domain naming convention: business names without technical suffixes.
- Current pattern for backend data that may evolve:
  - DTO keeps some backend enum-like values as `String`
  - mapper converts them to domain enums with `Unknown` fallbacks
- Real current example in profile:
  - DTO: `birthDate: LocalDate` with `LocalDateSerializer`
  - DTO: `gender: String`, `status: String`
  - Domain: `PatientGender`, `PatientStatus`, `birthDate: LocalDate`
- Reuse `LocalDateSerializer` and `LocalDateTimeSerializer` from `core/serializers/` instead of adding ad hoc parsing.

## Compose, insets, and IME
- This app already had a subtle keyboard/insets bug; do not reintroduce it.
- Important rule:
  - do not put `imePadding()` on a fullscreen root `Box` that contains scrollable content
  - apply it to the scrollable form container instead
- The observed bug happened when quickly switching focus `password -> close keyboard -> email`, creating strange layout gaps.
- The global snackbar host uses `windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))`; do not change this casually without retesting keyboard behavior.
- Whenever you touch a form screen, manually verify:
  - focus email -> password
  - quickly open/close the keyboard
  - scroll while keyboard is visible
  - snackbar while keyboard is visible

## UI and presentation
- Material 3 is the baseline.
- Use the existing theme and palette in `core/designsystem/theme/`.
- Extended semantic colors already exist and are used for snackbar feedback and profile status chips.
- `MainShell` switches top bars by route:
  - main tabs -> `MainTopBar`
  - profile detail -> `ProfileTopBar`
  - contact edit -> `EditContactInfoTopBar`
- Detail screens inside the shell use a soft horizontal slide + fade transition. Keep new detail routes visually consistent.
- Authenticated screens are allowed to hide the bottom bar; that does not break the architecture as long as they still live inside `MainShell`'s internal `NavHost`.

## Existing flows worth copying
- Auth bootstrap: `features/auth/`
- Patient profile read/update: `features/profile/`
- Authenticated shell and tabs: `features/main/presentation/shell/`
- Home placeholder decoupled from shell: `features/home/`

## Recommendations for future sessions
- Before moving something into `core/`, verify that it is truly cross-cutting and not merely reusable.
- Before creating a new error catalog, check whether the backend code already belongs in core or should stay feature-specific.
- Before pushing profile fields into `User`, remember that auth and patient are separate models in this app.
- If an endpoint returns data that affects session state, consider `SessionStore` consistency as part of the change.
- If a new authenticated screen is secondary, it probably belongs as an internal `MainShell` route, not in the root graph.
- If compilation fails around navigation, first inspect typed routes, `toRoute` imports, and feature DI wiring.
- If Room is introduced later, note that there is currently no real Room-backed local layer in active use; do not assume relational persistence exists just because the dependency is present in Gradle.
