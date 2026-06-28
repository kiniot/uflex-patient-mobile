# uFlex Patient Mobile — Progreso

Estado del trabajo realizado sobre la app del paciente (Kotlin · Compose · Hilt · Retrofit ·
Navigation · DataStore) y los siguientes pasos. Para arquitectura y convenciones, ver
[`ARCHITECTURE.md`](ARCHITECTURE.md) y [`../AGENTS.md`](../AGENTS.md).

> Contexto: la app es parte del ecosistema **uFlex** (firmware embebido, edge gateway, REST API,
> web clínica). El paciente se conecta a su **kit IoT** por **BLE** y consume el backend por HTTP.
> La identidad del kit es el **`serialNumber`** (ver `device-identity-contract.md` en el repo del
> firmware). El backend es el sistema de registro; el edge (separado, en construcción) validará y
> reenviará las repeticiones — su **autenticación contra el backend ya está implementada y
> verificada** (§13.0.a del EXECUTION-CONTRACT).

---

## ✅ Hecho

### `features/device` — Conexión BLE con el kit
- **Identidad (remoto):** `GET /devices/my-assigned` → la app conoce el `serialNumber`,
  `advertisedName` y `macAddress` del kit asignado.
- **Capa BLE** (Nordic Kotlin-BLE-Library), aislada en `data/ble/`:
  - **Descubrimiento por Service UUID** (`a1f7b2c0…`) — robusto (el nombre no siempre cabe en el
    advertisement).
  - **Confirmación de identidad** leyendo la característica de serial (`a1f7b2c2…`).
  - **Telemetría** (`a1f7b2c1…` NOTIFY): parser del frame de 53 bytes → 3 quaternions + actuadores +
    secuencia, con `MTU` negociado a 517 (si no, las tramas llegan truncadas).
  - **Desconexión robusta:** monitorea `gatt.connectionState` y propaga caídas (`ConnectionLost`).
- **Use cases reutilizables:** `ConnectToAssignedDeviceUseCase`, `ObserveDeviceConnectionStateUseCase`,
  `ObserveMotionTelemetryUseCase`, `DisconnectDeviceUseCase`, `GetMyAssignedDeviceUseCase`.
- **PoC de presentación:** `DeviceConnectionScreen` (tab "Devices") — escanear → conectar → confirmar
  serial → ver las tramas en vivo. Pide permisos BLE en runtime (split por versión de Android).

### `features/plan` — Fase 1: tab "Ejercicios" (lectura)
- **Datos:** plan activo (`/patients/me/treatment-plans/active`) + programados (`/scheduled`) +
  catálogo de ejercicios (`/exercises`), uniendo cada serie con el nombre/parte/movimiento del
  ejercicio. El 404 de "sin plan activo" se trata como estado vacío.
- **UI atractiva:** `ExercisesScreen` — hero del plan activo (estado, periodo), rutinas → ejercicios
  con su dosis (reps · ROM° · duración), y sección de planes programados.
- **Detalle del ejercicio:** `ExerciseDetailScreen` con **video inline (Media3/ExoPlayer)**:
  - `TextureView` (mejor que SurfaceView en estos dispositivos; permite esquinas redondeadas),
  - **controles mínimos: solo play/pausa** (sin barra, sin rebobinar/avanzar),
  - **autoplay**, pausa al ir a background, liberación al salir.

### `features/therapy` — Fase 2: iniciar la sesión
- **Datos:** `TherapyApiService` (schedule, initiate, confirmHardware, start, cancel, active). El
  repositorio resuelve el `patientId` desde `SessionStore`.
- **Asistente de preparación** (`SessionPreparationScreen` + `SessionPreparationViewModel`, máquina de
  estados): rutina de hoy (schedule) + kit asignado → **initiate** (`iotDeviceId = serial`) →
  **conectar BLE** (reusa `features/device`) → **confirmar sensores** → **start**.
  - **Reanuda** una sesión activa existente (`/active`): Pending/Ready continúan el flujo;
    InProgress aterriza en "iniciada".
  - **La pantalla "iniciada" refleja la conexión real** (no la asume): si el kit no está conectado,
    avisa y ofrece **reconectar**. Cancelar disponible (cancela + desconecta).
  - Maneja 409 (ya hay sesión activa → reanuda), permisos BLE y fallos de conexión legibles.

### `features/therapy` — Fase 3: ejecución de la sesión ✅ *(verificada en dispositivo)*
- **Datos:** `startSerie`, `getProgress`, `reportPain`, `finalize` en `TherapyApiService` +
  `SessionProgress`/`SerieProgress` (dominio) + use cases. `SafeApiCaller.executeNoContent` para los
  endpoints sin body útil (reportPain devuelve 200 vacío; startSerie ignora su body).
- **Pantalla de ejecución** (`SessionExecutionScreen` + `SessionExecutionViewModel`): inicio **manual**
  de cada serie → **progreso de reps por polling de `/progress`** (~2.5 s, autoritativo) + **SSE en vivo** (abajo) → **reportar
  dolor** (diálogo 0–10) → **finalizar** cuando todas las series están completas.
- **Gauge BLE en vivo** (reusa `features/device`): ángulo en vivo + chips de actuadores + banner de
  desconexión con **reconectar**. Reusa la conexión BLE singleton de la preparación.
- **Handoff:** la pantalla "iniciada" de preparación tiene botón **"Comenzar ejercicios"** → ejecución
  (con `popUpTo` de la preparación).
- **Fix incluido:** "sin sesión activa" llega como 404 con código de dominio
  (`THERAPY_SESSION_NOT_FOUND`) que el mapper convertía en `AppError.Business` (no `NotFound`), lo que
  hacía fallar la preparación. `TherapyRepositoryImpl.getActiveSession()` normaliza ese 404 →
  `NotFound` (ver Notas).
- **Estado:** **verificada end-to-end en dispositivo físico** contra el backend real — Resumen →
  "Comenzar ejercicios" → ejecución → iniciar serie → reportar dolor (`painLevel=7` en backend) →
  5 reps posteadas por el edge → "Todos completados" → finalizar (`status=Completed`, `good:5`,
  `avgRom:92`). El **conteo de reps solo avanza con el edge+embedded**; el gauge BLE necesita un
  kit/sim conectado.

### `features/therapy` — Olas 1+2: calibración guiada + progreso en vivo (SSE) ✅
- **Calibración guiada (Ola 1):** antes de cada serie, un paso **"ponte en posición → Calibrar e
  iniciar"** (mantén el brazo en el cero ~5 s) que ancla el **cero de sesión** del kit, integrado en el
  flujo de `startSerie` (`SessionExecutionScreen`/`ViewModel`/`UiState` + strings EN/ES).
- **Progreso en vivo por SSE (Ola 2 · slice 1):** cliente `okhttp-sse` (`EdgeProgressDataSource` +
  `ObserveLiveProgressUseCase`) suscrito al **edge** (`EDGE_BASE_URL` por build type, sin
  `AuthInterceptor`, `readTimeout=0`).
  - **Optimista vs autoritativo:** el SSE **sube** el contador al instante (toma el máximo, nunca
    retrocede); el **polling de 2.5 s al backend sigue siendo la verdad** y reconcilia. Si el edge no
    está o el stream cae → **degradación silenciosa** al polling (retry con backoff).
- **Verificado:** `compileDebugKotlin` + SSE E2E real por HTTP contra el edge.
- **Validado en dispositivo físico (2026-06-22):** Android (M2101K7BL) en la misma LAN → en la
  pantalla de ejecución el cliente SSE se suscribió al edge y, al publicar un `rep` con el `serieId`
  real, **el contador saltó 0→4 en la UI** (luego el polling reconcilia). Solo se inyectó el disparo
  del rep; transporte/SSE/UI son reales.

### Soporte / ecosistema
- **Backend:** se habilitó que el **paciente** pueda iniciar su sesión (`ROLE_PATIENT` en el `POST`
  de initiate) con un **guard de seguridad** (solo puede iniciar para sí mismo). *(cambios en el repo
  `uflex-rest-api`.)*
- **Dependencia nueva:** Media3 (ExoPlayer + UI) para el video.
- **Transversal:** todo sigue el patrón establecido (data/domain/presentation, `AppResult`, Hilt
  `@Binds`, rutas overlay en `MainShell`, strings **EN + ES** vía `UiText`, design system Material3 +
  `ExtendedTheme`).

---

## 🔜 Siguientes pasos

> **Lectura para alguien nuevo.** El lazo en vivo **ya está cerrado en código**: el firmware envía
> muestras enriquecidas a ~10 Hz, el **edge detecta reps + compensación y reenvía al backend**, y el
> **móvil recibe el progreso por SSE** (con polling de respaldo). Lo que queda es **validar en hardware
> real** (sobre todo el magnetómetro) y unos pulidos. Estado cross-repo completo en la sección "Estado
> de implementación (Olas 1–2)" del `EXECUTION-CONTRACT.md`.

### ✅ Ya hecho (Olas 1–2 — antes figuraba como pendiente aquí)
- **Embedded:** muestra enriquecida `{target_angle, proximal_signal}` a ~10 Hz, down-channel
  (`maxSafeAngle`), seguridad local, y magnetómetro + bias del giro (código; builds sim+hw OK).
- **Edge:** detección de compensación (`CompensationDetector`) + SSE de progreso (`ProgressBroker` +
  `/progress-stream`); pytest verde.
- **Móvil:** calibración guiada + cliente SSE optimista con reconciliación (ver arriba).
- **Auth del SSE — rendezvous por backend + token de pairing (§13.0.b):** el backend acuña el token
  por sesión y expone `GET /patients/me/edge-connection`; el edge reporta su URL de LAN
  (`PUT /iam/edge-service-accounts/me/lan-url`) y valida el `Authorization: Bearer` del SSE; el móvil
  resuelve URL+token y abre el stream con el token (fallback a `BuildConfig` para emulador).
  Verificado headless (backend `mvnw test`, edge `pytest`, móvil `compileDebugKotlin`/`assembleDebug`)
  **y validado E2E en LAN (2026-06-23)** en dispositivo físico: contador **3/5→5/5** por SSE; token
  ausente/malo → **401**.

### 1. Validación en hardware (lo que realmente falta) — necesita placa/LAN
- **Magnetómetro — diagnóstico resuelto (2026-06-23):** no era el DRDY ni chip clónico, sino
  **colisión I²C** (2 MPU9250 comparten bus, AK8963 fijo en `0x0C`). El **I²C master mode** (ya en el
  firmware, + reset de la MPU al arranque) **lee mag real en una IMU aislada** (probado en placa), pero
  los 3 mags necesitan aislar cada AK8963 → **multiplexor** (ya comprado, sin montar) o recablear el
  proximal solo en el 2º bus. Hasta entonces la **compensación con señal real no se valida**;
  alternativa: **Plan B** (6-DOF). El board quedó re-flasheado en **bypass** (Ola 1 validada) a la
  espera del mux; el master mode vive en el código. Detalle en `EXECUTION-CONTRACT.md` §13.4 ítem 2.
- **`.env` del firmware:** en **placeholder** (no commitear secretos); para correr en placa llenar
  WiFi real + IP del edge. Ya validado el 2026-06-23; `platformio.ini` soporta SSIDs con espacios.
- **E2E en LAN — ✅ HECHO (2026-06-23) con kit real:** el firmware (`esp32_hw`, WiFi real) alimentó al
  edge → el detector contó **5 reps reales** → backend (serie 5/5) → la app mostró el conteo + gauge
  BLE en vivo (18°) y se finalizó la sesión. También el rendezvous+SSE (token válido sube el contador;
  token malo → 401). `begin()` reordenado (IMUs antes del WiFi). **Lo que aún falta:** el
  **magnetómetro** (yaw real) para la **compensación** — ver bullet del DRDY arriba.
- **Hallazgo (seguridad/calibración):** sin la **calibración guiada** previa (cero de sesión mal
  anclado), el `target_angle` puede cruzar `maxSafeAngle` y el buzzer suena en continuo; considerar un
  guard en firmware (no enforcar hasta una primera lectura/calibración válida).

### 2. Auth del SSE (§13.0.b) — ✅ HECHO y validado E2E en LAN (2026-06-23)
- **Mecanismo (rendezvous por backend, no mDNS):** el edge reporta su URL de LAN
  (`PUT /iam/edge-service-accounts/me/lan-url`); el móvil pide `{localEdgeUrl, pairingToken}` a
  `GET /patients/me/edge-connection` y abre el SSE con `Authorization: Bearer`; el edge valida el
  token (acuñado por sesión en el backend, cacheado del `active/by-device`) → 401 si falta/no coincide.
- **Validado E2E en LAN (2026-06-23):** dispositivo físico → la app resolvió URL+token del backend,
  abrió el SSE con `Bearer`, el contador saltó 3/5→5/5; token ausente/malo → 401. **Follow-ons:**
  **TLS** del canal y **mDNS** (descubrimiento sin nube).

### 3. Fix central de errores (recomendado, pequeño)
- El fix del 404 se hizo **local** a `getActiveSession`. Conviene centralizarlo en
  `core/network/ApiErrorMapper`: cuando el `code` de dominio no se reconozca pero el HTTP status sí
  (404→NotFound, 409→Conflict…), **preferir el status**. Así ninguna otra feature repite el bug
  (ver memoria del proyecto sobre este mapeo).

### Pulido (cuando haya tiempo)
- **Connection-priority del gauge BLE:** el móvil pide `requestMtu` pero **no**
  `requestConnectionPriority(CONNECTION_PRIORITY_HIGH)` → el connection-interval por defecto de Android
  estrangula la entrega de NOTIFY (el gauge se ve lento). Añadirlo en `features/device/data/ble`.
- **Autoplay en silencio** del video (hoy arranca con audio).
- Mover textos del **PoC de device** (`DeviceConnectionScreen`) a `strings_*` + `UiText` (hoy inline).
- Estados de carga (shimmer) y accesibilidad.

### Operativo
- **Commitear** el trabajo (mobile + backend + edge están sin commitear; tres repos).
- E2E en **hardware real**: login paciente → ver plan → iniciar sesión → kit encendido → reps reales
  fluyendo edge→backend→móvil.

---

## ⚠️ Notas y decisiones
- **Conexión BLE ≠ sesión backend:** el backend marca `InProgress` solo por las llamadas API; no sabe
  si el BLE está conectado. La UI refleja el estado real del kit.
- **Identidad del kit = `serialNumber`** (cross-service). El `DeviceId` (UUID) es interno del backend;
  la app puede usarlo para `/devices/{id}`, pero la correlación con el edge va por serial.
- **Errores 404 de dominio:** el backend usa códigos de dominio (`THERAPY_SESSION_NOT_FOUND`, etc.) en
  los 404. El `ApiErrorMapper`/`CoreErrorCode` solo reconoce los códigos genéricos, así que un código
  desconocido cae en `AppError.Business` (no `NotFound`). Al chequear `is AppError.NotFound` hay que
  recordarlo (o normalizar el 404 por llamada, como en `getActiveSession`).
- **Logs `BufferQueueProducer … TIMED_OUT`:** ruido benigno del códec del fabricante (MediaTek), no es
  un bug de la app; el video funciona.
- **`API_BASE_URL`** vive en `app/build.gradle.kts` (build `debug`). Apuntar a la IP del backend en la
  LAN con su puerto (`http://<ip>:8080/api/v1/`). Verificación rápida: `./gradlew :app:assembleDebug`.
