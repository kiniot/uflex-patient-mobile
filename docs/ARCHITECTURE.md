# Architecture

## Purpose

This document defines the architectural structure, layer responsibilities, and project conventions for this codebase.

Its goal is to keep feature code predictable as the app grows, while preserving clear boundaries between:

- domain models
- remote models
- local persistence models
- presentation state
- shared infrastructure

For session-specific operational guidance, known pitfalls, and implementation tips for AI agents, see [AGENTS.md](/Users/salim/dev/projects/kiniot/uflex-patient-mobile/AGENTS.md).

## Project shape

The app follows a feature-first structure under:

```text
app/src/main/java/com/kiniot/uflex/
```

High-level areas:

- `core/`: shared cross-feature infrastructure
- `features/`: business features

## Feature structure

Each feature should follow this general structure:

```text
features/<feature>/
├── data
│   ├── local
│   │   ├── dao
│   │   ├── datasource
│   │   └── entity
│   ├── mapper
│   ├── remote
│   │   ├── api
│   │   ├── datasource
│   │   └── dto
│   └── repository
├── di
├── domain
│   ├── model
│   ├── repository
│   └── usecase
├── navigation
└── presentation
```

Notes:

- `navigation/` is part of the real feature structure when the feature owns routes or route wiring.
- `error/` may exist at feature level when the feature needs its own business error catalog. Current example: `features/auth/error/`.
- `local/dao` and `local/entity` should remain part of the structure even if a given feature currently only uses `DataStore`-backed local sources.

## Shared core

Shared infrastructure belongs in `core/` when it is truly cross-feature.

Current examples:

- `core/network/`: Retrofit, OkHttp, interceptors, `SafeApiCaller`, API error mapping
- `core/result/`: `AppResult`, `AppError`, core error code catalog
- `core/session/`: persistent session storage
- `core/navigation/`: root navigation and app-wide navigator wiring
- `core/designsystem/`: theme, semantic colors, shared feedback components
- `core/ui/`: shared UI-facing primitives such as `UiText`
- `core/serializers/`: shared serializers such as `LocalDateSerializer`
- `core/di/`: shared dependency modules such as storage/network wiring

Move code into `core/` only when it is foundational or reused across multiple features with the same responsibility.

## Layer responsibilities

### Domain

The domain layer contains business-facing contracts and models.

Rules:

- domain models use business names without technical suffixes
- domain must not depend on Retrofit, DTOs, Room, DataStore, or Compose
- repository interfaces belong here
- use cases express business actions

Examples:

- `User`
- `PatientProfile`
- `AuthRepository`
- `ProfileRepository`
- `SignInUseCase`
- `GetMyPatientProfileUseCase`

### Remote

The remote layer models HTTP communication.

Rules:

- Retrofit interfaces should be named by feature or business area
- request/response payloads should use `Dto` suffixes
- prefer `RequestDto` and `ResponseDto` when request and response shapes differ

Examples:

- `AuthApiService`
- `ProfileApiService`
- `SignInRequestDto`
- `SignInResponseDto`
- `PatientResponseDto`
- `UpdatePatientProfileRequestDto`

### Local

The local layer models persistence concerns.

Rules:

- Room entities should use `Entity`
- DAOs should use `Dao`
- local data sources expose persistence behavior to repositories
- a feature may use only part of the local structure depending on current needs

Examples:

- `UserEntity`
- `PatientEntity`
- `UserDao`
- `AuthLocalDataSource`
- `ProfileLocalDataSource`

### Data sources

Data sources should be named by feature or business capability, not by low-level resource unless the feature itself is resource-oriented.

Examples:

- `AuthRemoteDataSource`
- `AuthRemoteDataSourceImpl`
- `AuthLocalDataSource`
- `ProfileRemoteDataSource`
- `ProfileLocalDataSource`

### Repositories

Repository implementations live in `data/repository/` and use the `Impl` suffix.

Examples:

- `AuthRepositoryImpl`
- `ProfileRepositoryImpl`

Repositories are the place where:

- remote and local sources are coordinated
- DTOs are mapped into domain models
- session consistency is preserved when needed

## Navigation architecture

Navigation is split into global and authenticated-local responsibilities.

### Root navigation

`core/navigation/RootNavGraph.kt` is the single root `NavHost`.

It should only decide between global flows such as:

- splash
- auth
- main

It should not own bottom-tab navigation or authenticated detail routing.

### Authenticated area navigation

The authenticated area is mounted through `MainGraph`, which renders `MainShell`.

`MainShell` owns:

- its own internal `NavHost`
- bottom navigation
- top bar switching for the authenticated area
- secondary/detail routes that can hide the bottom bar

This keeps global navigation separate from in-shell navigation.

## Data flow

The expected remote-to-domain flow is:

1. `ApiService` returns `Response<Dto>`
2. `RemoteDataSource` transforms the call into `AppResult<Dto>`
3. `RepositoryImpl` maps the DTO into domain models
4. `Repository` exposes domain-facing contracts
5. `ViewModel` converts use case results into feature-specific UI state

Recommended examples:

- `SignInResponseDto -> User`
- `PatientResponseDto -> PatientProfile`
- `UserEntity -> User`
- `PatientEntity -> PatientProfile`

Avoid mapping from a domain model into a request DTO unless the domain model actually represents the same input concept.

For sign-in, prefer:

- `SignInRequestDto(email, password)`

instead of mapping from `User`.

## Result and error handling

Use the shared result and error types from `core/result`.

Primary shared types:

- `AppResult<T>`
- `AppError`

Recommended usage by layer:

- `ApiService`: `Response<Dto>`
- `RemoteDataSource`: `AppResult<Dto>`
- `Repository`: `AppResult<DomainModel>`
- `ViewModel`: converts `AppResult` into `UiState`

Error ownership rules:

- generic transport/framework/server cases belong in core
- feature business codes can have feature-specific catalogs

Current example:

- auth has a feature error catalog in `features/auth/error/`

## UI state and presentation

Each screen should define its own UI state.

Examples:

- `SignInUiState`
- `ProfileUiState`
- `EditContactInfoUiState`

Do not create a single generic global UI state for the whole app.

Presentation rules:

- screens and view models should use feature-specific UI state
- use `UiText` for user-facing messages instead of hardcoded strings
- keep business logic out of composables

## DI modules

Feature DI modules live in `features/<feature>/di` and should be named by responsibility.

Examples:

- `AuthBindingsModule`
- `AuthApiModule`
- `ProfileBindingsModule`
- `ProfileApiModule`

Shared modules can live in `core/di/`.

Examples:

- `NetworkModule`
- `StorageModule`

## Mapper naming

Mappers should be named by context.

Use feature-based names when the feature is small and the mappings are closely related.

Examples:

- `AuthMappers.kt`
- `ProfileMappers.kt`

Use resource-based names when a feature grows multiple entities or mapper responsibilities.

Examples:

- `PatientMappers.kt`
- `PatientRemoteMappers.kt`
- `PatientLocalMappers.kt`

## Verb conventions

Prefer stable verbs across layers when the business action is the same.

Example:

- `AuthApiService.signIn`
- `AuthRemoteDataSource.signIn`
- `AuthRepository.signIn`
- `SignInUseCase.invoke`

Preferred verbs:

- `get`: obtain something through a business-facing or abstract contract
- `fetch`: optionally emphasize remote reads inside remote data sources
- `create`: create a new resource
- `update`: update an existing resource
- `delete`: remove a resource
- `save`: persist locally or upsert-like behavior
- `signIn`, `signOut`: authentication actions

Avoid overloading:

- `insert`: mainly for DAO or database-level operations
- `register`: only when the business language explicitly uses "register"

## Naming summary

### Domain

- `User`
- `PatientProfile`
- `PatientStatus`

### Remote

- `AuthApiService`
- `SignInRequestDto`
- `PatientResponseDto`

### Local

- `PatientEntity`
- `PatientDao`

### Repositories

- `AuthRepository`
- `AuthRepositoryImpl`

### Presentation

- `SignInViewModel`
- `ProfileScreen`
- `ProfileUiState`

## Practical guidance

- Keep domain independent from transport and persistence details.
- Keep repositories domain-facing even when they coordinate remote and local work.
- Prefer typed navigation destinations over manual route strings.
- Preserve feature-level ownership for business-specific errors.
- Keep local persistence structure available even when some features currently use only `DataStore`.
