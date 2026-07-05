# uFlex — Contrato de Ejecución y Decisiones de Arquitectura

Documento de referencia transversal del ecosistema uFlex. Recoge las decisiones
de diseño acordadas para el flujo de ejecución de una sesión de terapia
(telemetría del kit, detección de repeticiones, registro en el backend y
visualización en el móvil) y los siguientes pasos por repositorio.

Vive en el repo del móvil por conveniencia, pero aplica a los cinco proyectos:
firmware embebido, edge gateway, REST API (backend), app del paciente (este
repo) y, de forma indirecta, la web clínica.

Estado: decisiones cerradas e **implementadas**. Las **Olas 1 y 2** (lazo de
conteo en vivo, seguridad local, SSE de progreso, compensación y magnetómetro)
están **construidas y verificadas headless**; falta la **validación final en
placa** de las piezas de hardware. El estado autoritativo y actual está en la
sección de abajo, **"Estado de implementación (Olas 1–2)"**. El §13 es el roadmap
de diseño original — sus marcadores de estado pueden quedar por detrás de esa
sección.

---

## 1. Alcance

- uFlex es una plataforma de telerehabilitación domiciliaria. La terapia se
  realiza en casa del paciente, con un edge co-localizado junto al kit.
- Rehabilitación de brazo: codo (flexión / extensión) y, posiblemente, muñeca
  (pronación / supinación). Nada más por ahora.
- Los enums del backend ya reflejan este alcance: `BodyPart {ELBOW, WRIST}`,
  `MovementType {FLEXION, EXTENSION, PRONATION, SUPINATION}`.

### Autenticación: edge→backend y móvil↔edge HECHAS

La autenticación máquina-a-máquina **edge→backend ya está implementada y
verificada** (§13.0.a): los endpoints de ingesta (`recordRepetition`,
`compensatory-movements`, `GET /active/by-device`) aceptan un principal de
servicio `ROLE_EDGE`, con least-privilege por serial. La otra frontera, **el canal
móvil↔edge (SSE) ahora también está autenticada** (§13.0.b): **rendezvous mediado por
el backend** (el edge reporta su URL de LAN; el móvil pide URL + **token de pairing**
por sesión) y el edge valida el token al suscribirse al SSE. **Construido y validado E2E en LAN
(2026-06-23)** en dispositivo físico. Detalle en §13.0.

---

## Estado de implementación (Olas 1–2) — actualizado

> **Esta sección manda** para "qué está hecho / validado / pendiente". El §13 (más
> abajo) es el roadmap de diseño original. "Headless" = verificado sin placa
> (pytest, compiles `pio`/`gradle`, tests host); "en placa" = validado en el ESP32 real.

**Arquitectura del lazo (recordatorio):** ESP32 (3 IMUs) → **mide** el ángulo
articular y lo envía al **edge** (WiFi/HTTP) + telemetría BLE al **móvil**; el edge
**detecta/clasifica** reps y compensación y las reenvía al **backend** (sistema de
registro durable, idempotente) + empuja progreso en vivo al móvil por **SSE**; el
**móvil** orquesta la sesión y muestra el progreso (autoritativo por polling al
backend + optimista por SSE). Identidad cross-service = `serialNumber`.

### Ola 1 — Cerrar el lazo (conteo de reps + seguridad local) — HECHO + VERIFICADO
- **Embedded:** ángulo articular **absoluto** del par activo (codo = par
  upper-middle, muñeca = middle-lower) con **cero de sesión**; muestra enriquecida
  `{target_angle, proximal_signal}` a **~10 Hz** (reemplaza el viejo payload `motion`
  cada 5 s); **down-channel** `GET active-context` (articulación activa + `maxSafeAngle`);
  **seguridad local** (vibración+buzzer al cruzar el techo, sin red). El mapeo
  articulación→par y el cero viven en el firmware (§5.7).
- **Edge:** ingesta enriquecida (batch `samples[]`, conserva el contrato legacy) +
  `GET active-context` (deriva `maxSafeAngle = targetRom + 15`).
- **Móvil:** **calibración guiada** ("ponte en posición → Calibrar e iniciar" antes
  del `startSerie` existente, mantén ~5 s).
- Verificado: host (matemática/serializer/parser), `pio run esp32_sim`+`esp32_hw`
  SUCCESS, Unity compila; edge `pytest` + curl; móvil `compileDebugKotlin`.

### Ola 2 · slice 1 — SSE de progreso en vivo (edge→móvil) — HECHO + VERIFICADO (headless + EN DISPOSITIVO)
- **Edge:** `ProgressBroker` (fan-out por kit) + tally de reps por serie + `GET
  /movement-monitoring/progress-stream` (SSE: evento `rep` con tally **absoluto** +
  heartbeat); `app.run(threaded=True)`. **Sin auth (LAN local, §13.0.b)** — el pairing
  token es follow-on (TODO marcado en la ruta).
- **Móvil:** cliente `okhttp-sse` (`EdgeProgressDataSource`) a `EDGE_BASE_URL`
  (por build type); conteo **optimista** (toma el máximo, nunca retrocede); el
  **polling de 2.5 s sigue siendo autoritativo** y reconcilia; fallback silencioso
  si el SSE cae (retry con backoff).
- Verificado: edge `pytest`, **SSE E2E real por HTTP** (suscribir → POST rep → llega
  `event: rep` con `reps_detected`), móvil `compileDebugKotlin`.
- **Validado en dispositivo físico (2026-06-22):** Android M2101K7BL en la misma LAN que el edge →
  en la pantalla de ejecución el cliente `okhttp-sse` se suscribió (`GET /progress-stream` visto en
  el edge desde la IP del teléfono) y, al publicar un `rep` con el `serieId` real de la serie en
  curso, **el contador saltó 0→4 en la UI** (luego el polling de 2.5 s reconcilia, según diseño).
  Único elemento inyectado: el disparo del rep (el firmware no alimentaba el edge — WiFi en
  placeholder); transporte/SSE/broker/cliente/UI son reales (la detección está cubierta por pytest).

### Ola 2 · slice 2 — Detección de compensación (edge) — HECHO + VERIFICADO headless
- `CompensationDetector`: ventana ~2 s de `(angle, proximal)`; dispara si el **rango
  de yaw proximal ≥15°** mientras el **ángulo objetivo se estanca (≤10°)**, con
  cooldown (un episodio = un evento); **no-op si falta `proximal`**. Reenvía
  `CompensatoryMovement` al backend (entidad/outbox/endpoint `compensatory-movements`
  ya existían); payload = `{"type":"ShoulderCompensation"}` (una sola IMU proximal no
  separa hombro vs torso).
- Verificado: edge `pytest` (detector + ingest + mapping). **La validación con señal
  real va en placa** (depende del magnetómetro/yaw).

### Ola 2 · slice 3 — Magnetómetro (firmware) — CÓDIGO HECHO; validación en placa PARCIAL
- **Código:** `GyroBiasCalibrator` (puro, media de N muestras quietas) + en
  `mpu9250_imu_array`: lectura **ASA** (Fuse-ROM + escala por eje, saturada a int16),
  check de **overflow ST2** (HOFL), y **bias del giroscopio** calibrado en `begin()`
  y restado en `updateImu` (el filtro Mahony no cambia: recibe el giro de-biased).
- Verificado headless: host (calibrador), `pio run esp32_sim`+`esp32_hw` SUCCESS, Unity compila.
- **Validado en placa** (flasheado `esp32_hw` + leído serial):
  - ✅ Flashea/arranca/corre sin colgar; **3× MPU9250 detectadas** (`WHO_AM_I=0x71`);
    **3× AK8963 "ready"** (la secuencia ASA **no** rompe el init); la **calibración de
    giro corre** y guarda bias; BLE arriba.
  - ⚠️ **BLOQUEO (pre-existente, hardware):** el AK8963 inicializa pero su bit
    **DRDY (ST1) nunca se pone en 1** en runtime → `magnetometer read skipped (not
    ready)` → mag=0 → el filtro sigue en **6-DOF y el yaw deriva**. O sea: **el yaw
    proximal aún no es real.** Es el debug de hardware que queda (área del "Error
    263"); típico de módulos MPU9250 clónicos o un detalle de la secuencia de lectura
    en modo continuo (fix probable: leer ST1+datos+ST2 en **una sola transacción** de
    8 bytes). **No lo introdujo este código** — el check DRDY ya existía.

### Backend (REST API) + auth edge→backend — HECHO + VERIFICADO (sin cambios en estas olas)
- Per-rep idempotente (`X-Edge-Sequence-Id`), `active/by-device`, `compensatory-movements`,
  `progress`, `ROLE_EDGE` con least-privilege por serial. Ver §13.2 / §13.0.a.

### Pendiente (qué falta hacer y/o validar)
- **Magnetómetro — RESUELTO EN PLACA por el mux (2026-07-02):** se montó el **wearable de fase de
  brazo** (ESP32 + **TCA9548A mux** + 3 satélites MPU9250 + RGB/buzzer; **motor descartado**) y el
  bring-up pasó: mux en `0x70`, 3× `WHO_AM_I=0x71` por canal, actuadores OK, y **2 de 3
  magnetómetros leen estables + responden al giro** (bíceps + antebrazo, vía **bypass-por-canal**).
  El viejo "DRDY nunca listo / Error 263 / mag=0" era la **colisión I²C del AK8963 en `0x0C`** — el
  **mux lo resuelve** (mags leen aislados por canal). El **3er IMU tiene el AK8963 muerto** (el fallo
  **sigue al IMU**, confirmado por swap; no es cableado) → ubicado en la **mano (ch2)**, no crítico
  para codo (par codo = bíceps+antebrazo, ambos con mag bueno); para muñeca haría falta un 4º IMU.
  **Pendiente aún:** (a) **delta de firmware del mux** — el firmware sigue en **dos buses**; pasarlo a
  **un bus + select de canal** (`1<<n → 0x70`) + **bypass-por-canal** (ya no hace falta el I²C master
  mode); (b) **compensación E2E** (edge) con el yaw proximal real; (c) **`GPIO32` (motor) sin uso** →
  seguridad = buzzer. Detalle: `uflex-embedded-app/docs/arm-phase-assembly-plan.md`.
- **[previo] Magnetómetro operativo — DIAGNÓSTICO CORREGIDO (2026-06-23):** no era el DRDY ni chip clónico,
  sino **colisión de direcciones I²C** — dos MPU9250 comparten el bus primario y el AK8963 tiene
  dirección fija `0x0C` (con bypass ambos colisionan; con I²C master mode hay contención multi-master y
  la 2ª IMU del bus falla el init). El **master mode lee mag real en una IMU aislada** (probado en
  placa), así que el firmware es la base correcta — pero con **3 IMUs y 2 buses** hay que aislar cada
  AK8963: **multiplexor I²C** (ya comprado, sin montar) o recablear la IMU proximal sola en el 2º bus.
  Hasta resolverlo, la **compensación con señal real no se valida**. *Plan B sin mag:* compensación por
  **rango de yaw sobre 6-DOF** con el bias del giro bien calibrado (deriva <0.5° en ~2 s; una
  compensación de hombro mueve 15–30°).
- **Validaciones manuales** (no headless, necesitan placa/LAN):
  1. Mag: ASA sanos, ST2 rechaza bajo imán, yaw estacionario deja de derivar.
  2. Compensación E2E: mover el brazo proximal con el codo estancado → outbox
     `compensatory` → backend.
  3. ✅ **SSE en vivo E2E — HECHO** (dispositivo físico, 2026-06-22): el contador del móvil saltó
     0→4 por SSE en la pantalla de ejecución; detalle en la slice 1 de arriba.
  4. ✅ **Lazo completo Ola 1 en placa — HECHO** (2026-06-23): el kit (`esp32_hw`, WiFi real)
     posteó muestras reales al edge → el detector contó **5 reps** → backend (serie 5/5) → la app
     mostró el conteo (+ gauge BLE en vivo, 18°) y se finalizó la sesión. Movimiento real, sin inyección.
- **Config de entorno:** el `.env` del firmware vuelve a **placeholder** por higiene (no commitear
  secretos); para correr en placa, llenar WiFi real + IP del edge (ya validado el 2026-06-23).
  `platformio.ini` ya soporta SSIDs con espacios (idiom de single-quote en los build flags).
- **Orden de `begin()` (firmware) — HECHO:** las IMUs se inicializan **antes** del WiFi (calibración
  del giro al arranque + no esperar a una asociación WiFi lenta).
- **Hallazgo (seguridad/calibración):** si la serie arranca sin la **calibración guiada** (cero de
  sesión mal anclado), el `target_angle` puede cruzar `maxSafeAngle` y el buzzer de seguridad suena
  en continuo. En uso normal la app obliga a calibrar; considerar un guard en firmware (no enforcar
  la seguridad hasta una primera lectura/calibración válida).
- **Auth del SSE (§13.0.b) — HECHO y validado E2E en LAN (2026-06-23):** rendezvous mediado por el
  backend + **token de pairing** por sesión; verificado en dispositivo físico (token válido → el
  contador saltó 3/5→5/5; token ausente/manipulado → 401). Quedan como follow-ons: **TLS** del canal
  y **mDNS** (descubrimiento sin nube).
- **Operativo:** **commitear** (edge + firmware + móvil sin commitear, tres repos).

---

## 2. Proyectos y responsabilidades

| Proyecto | Rol | Responsabilidad principal |
|---|---|---|
| `uflex-embedded-app` (ESP32) | Sensar y actuar | Leer IMUs, calcular ángulos, enviar muestras al edge, reaccionar localmente ante movimiento peligroso |
| `uflex-edge-gateway` (Flask) | Cerebro de detección | Detectar y clasificar cada repetición en tiempo real, detectar movimiento compensatorio, reenviar al backend, alimentar la UI en vivo del móvil |
| `uflex-rest-api` (Spring) | Sistema de registro | Persistir de forma durable e idempotente los hechos medidos, mantener el estado de la sesión, exponer progreso/historial |
| `uflex-patient-mobile` (Android) | Orquestación y UI | Conducir el ciclo de sesión, mostrar progreso en vivo, recibir el reporte de dolor del paciente |

Regla rectora: **el edge es el cerebro de detección en tiempo real; el backend
es el sistema de registro durable; el móvil orquesta y presenta; el embedded
sensa y actúa.**

---

## 3. Identidades y correlación

| Identidad | Dueño | Uso |
|---|---|---|
| `serialNumber` | Identidad física global del kit | Clave de correlación entre embedded, edge, backend y móvil |
| `DeviceId` (UUID) | Backend (`device/`) | Identidad interna del backend; no cruza al edge |
| `TherapySessionId` (UUID) | Backend (`therapy/`) | Sesión de ejecución |
| `SerieId` (UUID) | Backend (`therapy/`) | Serie dentro de la sesión |
| `edgeSequenceId` | Edge | Clave de idempotencia por repetición |

- `serialNumber` es el identificador casi global del kit. Es lo que viaja entre
  servicios. Cada servicio puede elegir su clave primaria interna; lo compartido
  es el valor del serial.
- En el edge, `serialNumber` se usa como **clave natural** (primaria) del
  dispositivo. Ver sección 6.1 (rename de `device_id`).

---

## 4. Flujo de datos de extremo a extremo

```
Embedded (ESP32)
  -- muestras de movimiento (WiFi local) -->  EDGE
  -- ángulo en vivo + estado de actuadores (BLE) -->  MÓVIL

EDGE
  -- detecta y clasifica cada repetición -->  outbox local
  -- recordRepetition idempotente (HTTP) -->  BACKEND
  -- evento de compensación (HTTP) -->  BACKEND
  -- progreso/validación en vivo (SSE, LAN local) -->  MÓVIL
  -- config de umbrales por serie (+ feedback de rep opcional) -->  EMBEDDED
  <-- GET /active/by-device/{serial}: sesión/serie activa + targets --  BACKEND

EMBEDDED
  -- dispara actuador de seguridad localmente si el ángulo cruza el techo

MÓVIL
  -- ciclo de sesión + dolor + lecturas autoritativas (HTTP+JWT) -->  BACKEND

BACKEND
  -- es consultado por la web clínica para historial/métricas
```

Cada dato viaja por su camino más directo:

- BLE (kit -> móvil): ángulo en vivo y estado de actuadores. Feedback
  instantáneo (gauge de movimiento). Presentación, no detección.
- SSE (edge -> móvil): conteo/validación de repeticiones en vivo, provisional.
- HTTP (edge -> backend): hechos medidos, verdad durable.
- HTTP+JWT (móvil -> backend): orquestación, input humano y lecturas
  autoritativas.

---

## 5. Decisiones de arquitectura

### 5.1 Registro por repetición (per-rep)

El edge reenvía **cada repetición** al backend, de forma idempotente, a medida
que la detecta. No se difiere a un resumen al final.

- Habilita progreso en vivo, acota la pérdida de datos ante un corte y preserva
  el timeline (dolor y eventos intercalados con repeticiones).
- La idempotencia (`edgeSequenceId`) hace seguro el reenvío con reintentos.

### 5.2 Cadencia desacoplada de la propiedad

La detección en tiempo real es responsabilidad del edge; la durabilidad de la
verdad es del backend. El edge bufferiza en un **outbox local** y reenvía a su
ritmo, reintentando ante cortes transitorios. Esto da resiliencia sin ser
offline-first (requisito: resiliencia ante cortes, no operación 100% offline).

### 5.3 Un solo cerebro de detección

La detección y validación de repeticiones vive **solo en el edge**. El móvil no
detecta repeticiones: lee el conteo (del edge por SSE en vivo; del backend como
fuente autoritativa). El embedded no cuenta repeticiones.

### 5.4 Camino vivo local vs. registro autoritativo

- UI en vivo (provisional): `kit -> edge -> móvil` por LAN local + BLE para el
  gauge. Sin salto a internet en el lazo en vivo.
- Registro autoritativo: `edge -> backend`. El móvil reconcilia contra el
  backend, que es la única fuente de verdad.

### 5.5 Resumen de serie

Como el backend recibe cada repetición con su clasificación, **calcula él mismo
el resumen de serie** (reps totales, buenas/incompletas/inseguras, ROM promedio,
valoración). No se requiere que el edge envíe un resumen agregado; si se
mantuviera, sería solo para reconciliación o diagnóstico.

### 5.6 Completitud de serie por totales

Una serie se considera completada al alcanzar las **repeticiones objetivo
totales** (buenas o no). No se fuerza al paciente a alcanzar un ROM que hoy no
puede. Cada repetición se clasifica y registra (`good` / `incomplete` /
`unsafe`); la valoración (porcentaje de buenas) es resultado, no requisito de
completitud.

Implicación en el backend: el conteo ya es por totales. Se renombra
`SerieStatus.Validated -> Completed` (porque es por conteo, no por calidad);
`Failed` queda disponible para serie abandonada o interrumpida.

### 5.7 Modelado del ROM

- El ROM objetivo es un **span relativo** (cuánto rango lograr), medido respecto
  a un baseline descubierto por repetición. No es una ventana absoluta.
- No se prescribe un mínimo: el baseline se descubre en tiempo real (el edge ya
  mide `achieved_rom = peak - baseline`).
- El **ángulo máximo seguro** es un techo absoluto, distinto del objetivo. Solo
  existe por la función de seguridad. Por ahora se **deriva** como
  `targetRom + margen` con una constante, calculado una sola vez en el backend y
  propagado idéntico al edge y al embedded. No se almacena como campo (mientras
  sea derivado, almacenarlo sería redundante y arriesgaría drift). Se promoverá a
  campo prescrito cuando el fisioterapeuta deba fijarlo de forma independiente.
- Se reemplaza `AngleThreshold(0, ROM)` (que modela el concepto equivocado: un
  mínimo espurio en 0 y el objetivo como tope de ventana, sin techo seguro) por
  un VO de targets de la serie (p. ej. `SerieTargets(targetRom, maxSafeAngle)`) o
  dos campos. Se elimina `isWithinThreshold()`: el backend no evalúa ángulos,
  confía en la clasificación del edge.
- Marco de referencia (a especificar en implementación): el objetivo es relativo
  y el techo absoluto; para que `target + margen` sea un techo coherente se
  asume el ángulo articular en un mismo marco (extensión aproximadamente 0).

### 5.8 Movimiento compensatorio

Se renombra el concepto vago de "anomalía" a **movimiento compensatorio**. Son
dos cosas distintas:

- Repetición insegura: una repetición completa cuyo pico cruzó el ángulo máximo
  seguro. Se reporta vía `recordRepetition` con `classification = unsafe`. No es
  una anomalía.
- Movimiento compensatorio: el paciente compensa (por ejemplo mueve el hombro o
  el cuerpo) de modo que su patrón cambia pero el ángulo relativo del brazo no
  progresa como debería. Concepto y endpoint propios.

Detectarlo entra en este alcance e implica:

1. Muestra más rica de embedded -> edge: un único ángulo relativo no basta; se
   necesita información del segmento proximal (hombro/torso). El embedded tiene 3
   IMUs y puede aportarlo, pero el contrato de la muestra debe llevarlo.
2. Servicio de detección de compensación en el edge.
3. Concepto de dominio en el edge (`CompensatoryMovement`) y su reenvío.
4. Backend: renombrar `AnomalousMovement -> CompensatoryMovement` (entidad,
   `AlertType`/enum, evento, resource y validación), eventualmente con metadatos
   (segmento, magnitud).

### 5.9 El edge no se modela como dominio en el backend

El edge es un cliente máquina autenticado, no un concepto de dominio. Su
identidad/credencial es una preocupación de `iam/` (cuenta de servicio), no un
agregado en `device/` (el kit y el edge son cosas distintas). No se modela el
edge como agregado por ahora. La autenticación de servicio, además, está
diferida (sección 1).

---

## 6. Modelo de dominio del edge (rediseño)

El edge fue una versión inicial para validar flujo. Se rehace con total
libertad, bajo el principio: **delgado en definiciones, rico en detección.**

- No replica definiciones (plan/rutina/ejercicio): eso es de `planning/`. El
  edge solo recibe el contexto de la serie activa vía
  `GET /active/by-device/{serial}` del backend, que ya snapshotea los targets de
  planning dentro de la sesión de therapy.
- Sí modela ejecución y detección.

### 6.1 Cambios concretos

- `device_id -> serial_number`, como clave natural (primaria) del dispositivo.
- `SerieExecution` está sobre-modelada (intenta ser registro analítico durable
  con avg ROM, valoración, conteos). Con per-rep, el backend calcula ese resumen.
  Se reduce a un **contexto de ejecución activa** correlacionado por `sessionId`
  + `serieId` del backend (con `targetRom`, `targetReps`, `maxSafeAngle`,
  `movementType`, `bodyPart`) usado para clasificar en vivo. El rollup durable
  sobra o queda como diagnóstico.
- Falta una entidad `DetectedRepetition` de primera clase: `sequenceId`,
  `achievedRom`, `peakAngle`, `classification`, `recordedAt` y estado de reenvío
  (para el outbox / reintento idempotente).
- Falta `CompensatoryMovement` (evento detectado + estado de reenvío).
- `MovementRecord` (un solo `angle`) se reemplaza por una muestra más rica
  suficiente para detectar compensación (segmento proximal). El buffer crudo pasa
  a ser una ventana transitoria, no una tabla durable grande.
- Outbox durable + cliente HTTP de forwarding + reintentos idempotentes.
- ACL real IAM <-> Monitoring (eliminar el import directo a la infraestructura de
  IAM).
- Alinear vocabulario con los enums del backend (`MovementType`, `BodyPart`).

### 6.2 Forma objetivo

- `Device(serialNumber, apiKey)` — IAM.
- `ExecutionContext(sessionId, serieId, targets)` — hidratado del backend.
- `DetectedRepetition(sequenceId, achievedRom, peakAngle, classification, recordedAt, forwardStatus)`.
- `CompensatoryMovement(tipo, magnitud, detectedAt, forwardStatus)`.
- `MovementSample` (enriquecida) + buffer transitorio.
- Outbox + cliente de forwarding.

---

## 7. Canales de comunicación

| Origen -> Destino | Transporte | Carga |
|---|---|---|
| embedded -> edge | WiFi local | Muestras de movimiento (enriquecidas para compensación) |
| edge -> backend | HTTP | Repeticiones (per-rep, idempotente), eventos de compensación |
| edge -> móvil | SSE (LAN local) | Progreso/validación en vivo (provisional) |
| edge -> embedded | WiFi local | Config de umbrales por serie (+ comando de feedback de rep, opcional) |
| embedded (local) | — | Enforcement de seguridad |
| kit -> móvil | BLE | Ángulo en vivo + estado de actuadores |
| móvil -> backend | HTTP+JWT | Ciclo de sesión, dolor, lecturas autoritativas |
| edge -> backend | HTTP | `GET /active/by-device/{serial}` para correlacionar serie/targets |

Notas:

- El backend es el único punto de contacto del edge para definiciones: le
  entrega el contexto de la serie activa, no el catálogo.
- El estado de actuadores se queda en BLE (kit -> móvil), tal como hoy. El edge
  no lo reenvía.

---

## 8. Contrato del payload por repetición

`POST /api/v1/therapy-sessions/{sessionId}/series/{serieId}/repetitions`

| Campo | Origen | Notas |
|---|---|---|
| `edgeSequenceId` | edge | Idempotencia; descarta duplicados |
| `peakAngle` (`achievedAngle`) | edge | Ángulo pico logrado |
| `achievedRom` | edge | `peak - baseline` de esa repetición |
| `recordedAt` | edge | Timestamp de la repetición |
| `classification` | edge | `good` / `incomplete` / `unsafe` |
| `unsafe`, `metTarget` | edge | Banderas derivadas de la clasificación |

El backend persiste de forma idempotente, avanza el estado de la serie y emite
sus eventos. La entidad `CompletedRepetition` se enriquece para llevar la
clasificación (hoy solo guarda `achievedAngle` + `recordedAt`).

Evento de compensación: `recordCompensatoryMovement` (endpoint renombrado desde
`anomalies`), con tipo/segmento, magnitud y `detectedAt`.

---

## 9. Comportamiento de actuadores

Dos disparos distintos, separados por criticidad:

| Disparo | Quién decide | Razón |
|---|---|---|
| Seguridad (ángulo inseguro, durante el movimiento) | Embedded, local, con umbral que baja el edge | Debe ser inmediato y tolerante a caída de red |
| Resultado de rep (buena/incompleta, post-rep) | Edge envía comando | Depende del conteo; la latencia es tolerable porque la rep ya terminó |

- Seguridad: el embedded ya calcula el ángulo en tiempo real; con el
  `maxSafeAngle` de la serie dispara localmente sin viaje de ida y vuelta y
  aunque el enlace falle. El edge no micromaneja el instante de la seguridad.
- Feedback de resultado de rep: opcional pero acordado (es bueno tenerlo). El
  edge es quien sabe si la rep fue buena/incompleta, así que es el origen
  correcto. El canal edge -> embedded ya existe para los umbrales; este feedback
  es un segundo uso del mismo canal.

El `maxSafeAngle` se deriva una vez en el backend y se propaga idéntico al edge
(clasificación `unsafe`) y al embedded (enforcement local), para que ambos usen
el mismo valor.

---

## 10. Tiempo real y transporte

- El lazo de feedback en vivo lo da el BLE (ángulo) y la reacción local del
  actuador. El conteo de repeticiones es confirmación, no el feedback primario,
  por lo que tolera 1-2 s.
- Progreso en vivo: SSE `edge -> móvil` por LAN local (provisional). El móvil
  pinta lo del edge al vuelo y reconcilia contra el backend.
- `GET /progress` del backend: lectura autoritativa para reconciliación,
  reanudar una sesión y otros clientes (web clínica). No es el lazo de polling en
  vivo.
- El progreso es estado (un contador acotado), no un stream de eventos: leer el
  estado actual es simple y auto-sanable ante reconexión.
- MQTT/Mosquitto: diferido. Su valor es fan-out a escala (dashboard
  multi-paciente), no este caso; no elimina la cadena, agrega un componente.
- WebSockets/SSE como upgrade del último salto solo si el lag del conteo molesta;
  SSE encaja mejor que WS por ser unidireccional.

---

## 11. Firmware y sensores

- Filtro de orientación: se mantiene **Mahony** (complementario explícito). Es la
  elección estándar y adecuada para seguimiento de ángulo articular en MCU.
  Cambiar a Madgwick no aporta mejora material.
- Las palancas reales (más que el algoritmo):
  1. Magnetómetro (AK8963) operativo y calibrado.
  2. Calibración de bias del giroscopio al arranque.
  3. Opcional: añadir el término integral (Ki) a Mahony para reducir deriva en
     sesiones largas (refinamiento, no cambio de filtro).
- Magnetómetro: el adaptador ya lo lee y el filtro lo consume cuando está
  disponible, pero hoy no opera de hecho (en simulación llega en cero -> fallback
  6-DOF con yaw a la deriva; en hardware hay un Error 263 pendiente). Como el
  movimiento compensatorio es una señal de yaw (rotación de hombro/torso),
  ponerlo operativo entra en alcance y es prácticamente prerrequisito de la
  detección de compensación.
- La muestra embedded -> edge debe enriquecerse para exponer el movimiento del
  segmento proximal (no basta el ángulo relativo único).

---

## 12. Detalles a especificar durante la implementación

Decididos en principio; su forma exacta se fija al construir:

- Formato exacto de la muestra embedded -> edge (campos para el segmento
  proximal).
- Formato del evento SSE edge -> móvil.
- Mecánica del `edgeSequenceId` (monótono por serie) y del outbox (orden de flush
  al reconectar).
- Marco de referencia del ángulo (objetivo relativo / techo absoluto, baseline
  aproximadamente 0).

---

## 13. Siguientes pasos por repositorio

**Estado actual (resumen — el detalle por slice está en la sección "Estado de
implementación (Olas 1–2)" más arriba):** el backend (§13.2), la auth edge→backend
(§13.0.a), la Fase 3 del móvil (§13.3) **y las Olas 1–2 completas** están **construidas y
verificadas headless**. El **lazo en vivo ya está cerrado en código**: el embedded
alimenta al edge con la muestra enriquecida `{target_angle, proximal_signal}` a ~10 Hz
(§13.4), el edge detecta reps **y compensación** y reenvía al backend (§13.1), y el móvil
recibe el progreso por **SSE** además del polling (§13.3). El **formato de la muestra
enriquecida** (antes §12/§13.4.1, "decisión abierta") quedó **resuelto e implementado**.
Lo que falta es la **validación en hardware real** — sobre todo el magnetómetro: en placa
el AK8963 inicializa pero su DRDY no llega a "ready" → yaw 6-DOF (detalle arriba) — y el
**follow-on de auth del SSE** (§13.0.b). Los marcadores de los sub-ítems de abajo se
actualizaron en consecuencia.

Cada ítem indica **qué** hacer, **cómo** hacerlo y **por qué**.

### 13.0 Autenticación

#### a) Edge -> backend (máquina a máquina) — HECHO Y VERIFICADO

Implementado y verificado end-to-end (sign-in del edge → `active-by-device`:
404 con su serial sin sesión activa, 403 con serial ajeno; provisión 201/409/401).
Diseño final:

- **Mecanismo híbrido:** el edge guarda una credencial de cuenta de servicio
  (email+password) y hace **sign-in** para un **JWT corto**; re-login ante 401. La
  credencial durable es revocable; el bearer corto acota la ventana de fuga.
- **Identidad por edge:** una cuenta de servicio `ROLE_EDGE` **por edge**
  (1 edge ↔ 1 kit ↔ 1 paciente), con `tenantId = clinicId`. Permite revocar/auditar
  un edge individual.
- **Least-privilege por serial (ahora):** un edge solo lee/escribe para su propio
  kit. El serial del principal se resuelve **desde su cuenta** (no desde un claim
  del JWT, que queda uniforme) y se compara contra el serial de la sesión/path →
  403 si no coincide. El scope por clínica ya lo da `tenantId`.
- **Provisión:** `POST /api/v1/iam/edge-service-accounts` —
  `ROLE_DEVELOPER` (con `clinicId` explícito) o `ROLE_CLINIC_ADMIN` (clínica
  inferida de su contexto); devuelve `{email, password}` una sola vez.
- **Backend:** `ROLE_EDGE` añadido (auto-sembrado); `@PreAuthorize('ROLE_EDGE')`
  en `recordRepetition`/`compensatory-movements` (antes **sin** auth) y añadido a
  `getActiveTherapySessionByDevice`; entidad `EdgeServiceAccount(userId, serial,
  clinicId)` en `iam/`. Sin cambios en la emisión del JWT (el tenant se resuelve por
  BD). **Gotcha:** agregar un `RoleName` exige re-crear el CHECK constraint
  `roles_name_check` en DBs existentes (Hibernate `update` no lo altera).
- **Edge:** `BackendClient` (sign-in lazy, bearer, refresh-on-401 con single-flight,
  token en memoria) + config por env (`UFLEX_BACKEND_URL/EDGE_EMAIL/EDGE_PASSWORD`).

#### b) Edge <-> móvil (canal SSE en LAN local) — RENDEZVOUS + TOKEN DE PAIRING: HECHO + VALIDADO E2E EN LAN (2026-06-23)

- **Estado:** el canal SSE está **asegurado**. Mecanismo elegido: **rendezvous mediado por el
  backend** (en lugar de mDNS/QR), reusando la confianza que móvil y edge ya tienen con el
  backend y esquivando el multicast.
- **Por qué:** es una tercera relación viva del móvil (además de backend y BLE); sin auth,
  cualquiera en la LAN doméstica podría leer o inyectar progreso (dato clínico).
- **Cómo (implementado):**
  - El **backend** acuña un **token opaco por sesión** (`TherapySession.edgePairingToken`, al
    iniciar la sesión); lo expone al edge en `GET /active/by-device` y al móvil en
    `GET /patients/me/edge-connection` (junto con la URL de LAN del edge). El token "desaparece"
    solo cuando la sesión deja de estar activa.
  - El **edge** reporta su URL de LAN (`PUT /iam/edge-service-accounts/me/lan-url`, `ROLE_EDGE`,
    serial resuelto del principal → solo actualiza la suya) → `EdgeServiceAccount.currentLanUrl`;
    **cachea el token** que llega en `active/by-device` y lo **valida** (`Authorization: Bearer`)
    al suscribirse a `progress-stream` (401 si falta/no coincide).
  - El **móvil** pide `{localEdgeUrl, pairingToken}` al backend y abre el SSE con ese token
    (fetch-then-stream; token fresco en cada reintento; fallback a `BuildConfig` si el edge aún
    no reportó URL).
- **Verificado headless:** backend (`mvnw test` + tests del handler de rendezvous), edge (`pytest`
  del validador/estado/poller), móvil (`compileDebugKotlin`/`assembleDebug`).
- **Validado E2E en LAN (2026-06-23):** dispositivo físico M2101K7BL + edge + backend en la misma LAN.
  La app pidió `GET /patients/me/edge-connection` (200, URL+token), abrió el SSE con
  `Authorization: Bearer` (200; el edge registró la suscripción desde la IP del teléfono) y el
  contador saltó **3/5→5/5** con reps inyectadas; token ausente/manipulado → **401**; reps reenviadas
  al backend (serie 5/5). El kit no alimentó el edge por WiFi (placeholder), así que las reps se
  inyectaron; el transporte/SSE/auth son reales.
- **Fuera de alcance (decidido):** **TLS** del canal (el token autentica; el tráfico sigue en
  claro en la LAN) y **mDNS** como descubrimiento sin nube (mejora de UX futura).

### 13.1 Edge (`uflex-edge-gateway`) — IMPLEMENTADO (detección + compensación + forwarding + SSE)

Principio: delgado en definiciones, rico en detección (§6). El §13.1 son 8 ítems; **los 8
están implementados** — el core (1, 2, 4, 5, 6) más 3 (compensación), 7 (SSE) y 8 (canal
de bajada), antes diferidos por depender de otros repos y ahora cerrados en las Olas 1–2.

**Hecho — core (ítems 1, 2, 4, 5, 6):** rename `serial_number`;
`ExecutionContext` / `DetectedRepetition` / `CompensatoryMovement` / `MovementSample`
enriquecida; `SerieExecution` eliminado (raw buffer en memoria); **detector
incremental**; **outbox + `edgeSequenceId`** (UUID por rep — el backend deduplica por
UUID, **no** es un contador monótono); **cliente de forwarding** autenticado (reusa el
`BackendClient` de §13.0.a); **poller** de `active-by-device`.

**Hecho — antes diferido (ítems 3, 7, 8):**
- **3 · detección de compensación (Ola 2):** `CompensationDetector` — el proximal barre
  (rango de yaw ≥15°) mientras el ángulo objetivo se estanca (≤10°); reenvía
  `ShoulderCompensation` al backend (entidad/endpoint ya existían). Consume el
  `proximal_signal` de la muestra enriquecida.
- **7 · SSE edge→móvil (Ola 2):** `ProgressBroker` + `GET /progress-stream` (evento `rep`
  + heartbeat). **Asegurado con token de pairing** (§13.0.b): valida `Authorization: Bearer`
  contra el token cacheado de `active/by-device`.
- **8 · bajar umbrales al embedded (Ola 1):** el edge deriva `maxSafeAngle = targetRom +
  margen` y lo sirve por `GET /active-context`; el firmware lo consume (§13.4.3).

**Estado de verificación:** todo el §13.1 está **implementado y unit-testeado** (pytest:
detector de reps **y de compensación** con streams sintéticos; outbox FIFO/idempotencia;
mapeo de forwarding; broker SSE), la **auth del edge** se probó en vivo (§13.0.a) y el
**SSE se probó E2E real por HTTP**. **Pendiente:** correr el lazo completo con **datos
reales de placa** de punta a punta (`embedded → detector → outbox → forwarder → backend`)
— bloqueado solo por la validación en hardware del firmware (§13.4), **no** por código.

1. **Rediseñar el dominio.**
   - *Qué:* `serial_number` como clave natural; `ExecutionContext(sessionId,
     serieId, targets)`, `DetectedRepetition`, `CompensatoryMovement`,
     `MovementSample` enriquecida; ACL real IAM <-> Monitoring.
   - *Cómo:* reemplazar la `SerieExecution` pesada por el contexto activo + outbox;
     sacar el import directo a la infra de IAM detrás de una interfaz.
   - *Por qué:* la versión actual fue un prototipo de validación de flujo; el
     backend ya es dueño del resumen, así que el edge no necesita rollup durable,
     sí detección y reenvío.
2. **Detección incremental de repeticiones.**
   - *Qué:* emitir cada rep en cuanto se completa (flexión + retorno), no en batch
     al `series/end`.
   - *Cómo:* máquina de estados sobre el stream de ángulos; la lógica de hysteresis
     ya existe — pasarla de batch a incremental.
   - *Por qué:* habilita el forwarding per-rep en vivo y el progreso del móvil.
3. **Detección de movimiento compensatorio.**
   - *Qué:* detectar compensación de hombro/torso (`ShoulderCompensation` /
     `TrunkCompensation`, los valores que el backend ya acepta).
   - *Cómo:* requiere la muestra enriquecida (segmento proximal); regla: el
     proximal se mueve mientras el ángulo objetivo no progresa.
   - *Por qué:* es un eje de calidad distinto del `unsafe` per-rep.
4. **`edgeSequenceId` + outbox durable.**
   - *Qué:* id de secuencia estable por rep + cola local persistente de pendientes.
   - *Cómo:* secuencia monótona por serie; persistir cada rep con su estado de
     reenvío; reintentar al reconectar.
   - *Por qué:* idempotencia (el backend descarta duplicados por `edgeSequenceId`,
     ya verificado) y resiliencia ante cortes (requisito: resiliencia, no
     offline-first).
5. **Cliente de forwarding al backend.**
   - *Qué:* POST per-rep a `recordRepetition` y de compensación a
     `compensatory-movements`, autenticado (§13.0.a).
   - *Cómo:* el cliente autenticado **ya existe** (`shared/infrastructure/backend_client.py`:
     sign-in + bearer + refresh-on-401); falta mapear `DetectedRepetition` -> payload
     (§8) y los reintentos idempotentes sobre el outbox.
   - *Por qué:* el backend es el sistema de registro durable.
6. **Correlación con la sesión activa.**
   - *Qué:* obtener `sessionId`, `serieId` y targets del kit.
   - *Cómo:* `GET /active/by-device/{serial}` — **ya devuelve las series embebidas
     con `targetRom`/`movementType`/`bodyPart`** (entregado en §13.2).
   - *Por qué:* el edge etiqueta cada rep con el `serieId` real del backend.
7. **Canal SSE edge -> móvil** para progreso en vivo (asegurar con §13.0.b).
8. **Bajar umbrales al embedded** por serie (config) + (opcional) comando de
   feedback de rep. El edge deriva `maxSafeAngle = targetRom + margen` y lo propaga
   idéntico al embedded.

### 13.2 Backend (`uflex-rest-api`) — HECHO Y VERIFICADO

Implementado y **verificado end-to-end** (login real, siembra de
ejercicio/plan/device, flujo completo de sesión, persistencia en Postgres).
Entregado:

1. Payload per-rep enriquecido: `RepetitionClassification {Good, Incomplete,
   Unsafe}`; `CompletedRepetition` con `peakAngle`/`achievedRom`/`classification`.
2. `targetRom` reemplaza `AngleThreshold`; `isWithinThreshold` eliminado; el
   backend **no** almacena `maxSafeAngle` (lo deriva el edge).
3. Snapshot `movementType`/`bodyPart` en `Serie` (lookup del `Exercise` en
   planning vía ACL).
4. Read-path del edge: `SerieDetailsResource` con targets; nuevo
   `ActiveTherapySessionResource` (series embebidas) en `GET /active/by-device`.
5. `SerieStatus.Validated -> Completed`.
6. `AnomalousMovement -> CompensatoryMovement` (entidad/VO/enum/evento + endpoint
   `POST /{id}/compensatory-movements`); `ExcessiveMovement` eliminado (lo inseguro
   va per-rep como `Unsafe`); el dolor sigue en `reportPainLevel`.
7. Métricas de calidad en el summary (good/incomplete/unsafe + `averageAchievedRom`
   + `compensatoryMovementsDetected`).
- Transversal: lazy-init resuelto con `@Transactional` en la interfaz (patrón de
  `getSessionProgress`); corrección del `GlobalExceptionHandler` +
  `ApiErrorCodeResolver` (validación de body -> 400, ruta desconocida -> 404).

**Follow-ups del backend:** auth de servicio del edge — **HECHA** (§13.0.a:
`ROLE_EDGE`, `EdgeServiceAccount`, provisión, `@PreAuthorize` + least-privilege por
serial). Pendiente (no bloqueante): documentar el contexto therapy en
`docs/reference/error-codes.md`.

### 13.3 Móvil (`uflex-patient-mobile`) — Fase 3 — HECHA Y VERIFICADA EN DISPOSITIVO

Verificada **end-to-end en un dispositivo físico** contra el backend real: Resumen →
"Comenzar ejercicios" → ejecución → iniciar serie → reportar dolor (`painLevel=7` en
backend) → 5 reps posteadas por la cuenta del edge → "Todos completados" → finalizar
(`status=Completed`, `good:5`, `avgRom:92`). (`compileDebugKotlin`/`assembleDebug`/KSP ✓.)

1. **Pantalla de ejecución** — HECHA: `startSerie` (inicio **manual** por serie),
   `reportPain` (diálogo 0–10), `finalize`; `SessionExecutionScreen`/`ViewModel`,
   ruta overlay en `MainShell` (handoff desde "iniciada" con botón "Comenzar
   ejercicios"), strings EN+ES.
2. **Progreso en vivo** — **polling de `GET /progress`** (~2.5 s, lectura **autoritativa**)
   **+ SSE del edge** (Ola 2): el SSE sube el contador de inmediato (optimista, toma el
   máximo, nunca retrocede) y el polling reconcilia; si el edge cae, degrada al polling.
3. **Cliente del canal edge (SSE en LAN)** — **HECHO (Ola 2):** `EdgeProgressDataSource`
   (`okhttp-sse`). **Auth añadida (§13.0.b):** el móvil resuelve `{localEdgeUrl, pairingToken}`
   vía `GET /patients/me/edge-connection` y abre el SSE con `Authorization: Bearer`. La URL del
   edge ya **no es fija**: viene del backend (fallback a `EDGE_BASE_URL` por build type para emulador).
4. **BLE vs. sesión backend** — HECHO: gauge BLE en vivo (reusa `features/device`,
   conexión singleton) + banner de desconexión y reconectar; son estados distintos.
5. **Calibración guiada (Ola 1)** — HECHO: paso "ponte en posición → Calibrar e iniciar"
   antes de `startSerie`, que ancla el cero de sesión del kit.

**Fix incluido durante la prueba:** "sin sesión activa" llega como 404 con código de
dominio (`THERAPY_SESSION_NOT_FOUND`) que el `ApiErrorMapper` convertía en
`AppError.Business` (no `NotFound`), haciendo fallar la preparación. Normalizado en
`TherapyRepositoryImpl.getActiveSession()` (404 → `NotFound`). **Follow-up recomendado:**
centralizarlo en `ApiErrorMapper` (preferir el status cuando el código de dominio no se
reconoce) para que ninguna otra feature repita el bug.

Nota: el conteo de reps solo avanza con el edge+embedded; el gauge BLE funciona con
un kit/sim conectado.

### 13.4 Embedded (`uflex-embedded-app`) — HECHO (Olas 1–2); LAZO OLA 1 VALIDADO EN PLACA (2026-06-23); WEARABLE + MUX ARMADO Y BRING-UP OK (2026-07-02) — FALTA EL DELTA DE FIRMWARE DEL MUX (ítem 2)

**Contexto para quien llega nuevo:** este era el último gran desbloqueo y **ya está hecho
en código** (Olas 1–2). El firmware ahora calcula el **ángulo articular absoluto** del par
activo (con cero de sesión) y publica al edge la **muestra enriquecida**
`{target_angle, proximal_signal}` a **~10 Hz** (antes era una sola `angle` cada ~5 s),
consume el `active-context` del edge y **aplica seguridad local**. El **lazo Ola 1 ya se validó en
placa el 2026-06-23** (kit con WiFi real → edge → backend → móvil, 5 reps reales contadas); lo único
pendiente es el **magnetómetro (ítem 2)**. Builds `esp32_sim` y `esp32_hw` en verde; lógica pura host-testeada.

1. **Muestra enriquecida + cadencia embedded→edge — HECHO (Ola 1).**
   - *Resuelto:* la muestra es `{target_angle, proximal_signal}` — `target_angle` = ángulo
     articular **absoluto** del par activo (codo = upper-middle, muñeca = middle-lower) con
     cero de sesión; `proximal_signal` = `yawDegrees` de la IMU proximal (segmento superior).
     Cadencia ~10 Hz al edge, en batch. El parser del edge ya la consume.
   - *Por qué:* sin la señal proximal el edge no distingue compensación del movimiento
     objetivo; sin cadencia, la detección incremental no tiene datos.
2. **Magnetómetro operativo (AK8963) + bias del giroscopio — CÓDIGO HECHO; DIAGNÓSTICO RESUELTO
   (2026-06-23); FALTA EL MUX.**
   - *Hecho:* lectura ASA, check de overflow ST2, bias del giro restado antes de Mahony, y ahora
     **I²C master mode** (cada MPU9250 lee su propio AK8963 vía `EXT_SENS_DATA`) + **reset de la MPU9250
     al arranque** (estado limpio entre reinicios). Compila sim+hw; master mode probado en placa.
   - *Diagnóstico real:* NO era el DRDY ni chip clónico — es **colisión I²C**: 2 MPU9250 en el bus
     primario, AK8963 fijo en `0x0C`. El master mode **lee mag real en una IMU aislada** (en placa:
     `HX/HY/HZ` no-cero), pero en el bus compartido hay contención multi-master (la 2ª IMU falla el
     init). Con **3 IMUs y 2 buses** hay que aislar cada AK8963.
   - *ACTUALIZACIÓN (2026-07-02) — MUX ARMADO Y VALIDADO:* se montó el wearable con el **TCA9548A** y el
     bring-up en placa confirmó que **2 de 3 magnetómetros leen** (bíceps+antebrazo, vía bypass-por-canal;
     el 3er IMU tiene el AK8963 muerto → a la mano, no crítico para codo). El mux **resuelve la colisión**.
     *Pendiente:* el **delta de firmware** — pasar de **dos buses** a **un bus + select de canal**
     (`1<<n → 0x70`) + **bypass-por-canal**; con el mux ya **no** hace falta el I²C master mode.
     Detalle del bring-up: `uflex-embedded-app/docs/arm-phase-assembly-plan.md`.
   - *[previo] Pendiente:* montar el **multiplexor I²C** (ya comprado) → el master mode lee los 3; o recablear la
     IMU **proximal** sola en el 2º bus (solo su mag, que es el que la compensación necesita); o **Plan
     B** (6-DOF). El binario del board quedó en **bypass** (estado validado de la Ola 1) a la espera del
     mux; el master mode vive en el código como base.
   - *Por qué:* la compensación es señal de **yaw**; sin magnetómetro el yaw deriva.
3. **Canal de bajada edge→embedded — HECHO (Ola 1):** el firmware poll-ea el
   `active-context` del edge (articulación activa + `maxSafeAngle`). El comando de feedback
   de rep queda como segundo uso opcional.
4. **Enforcement local de seguridad — HECHO (Ola 1):** el firmware dispara el actuador
   (vibración + buzzer) al cruzar `maxSafeAngle`, localmente.
   - *ACTUALIZACIÓN (2026-07-02):* **motor vibrador descartado** (interfiere con el magnetómetro +
     mete ruido de movimiento) → `GPIO32` sin uso; la **seguridad local pasa a ser solo el buzzer**.
   - *Por qué:* debe ser **inmediato y tolerante a caída de red**; el embedded ya calcula
     el ángulo en tiempo real, así que decide localmente sin viaje al edge.

**Sim vs hardware:** los ítems 1, 3 y 4 quedaron **validados en placa el 2026-06-23** (muestra
enriquecida real al edge, down-channel `active-context`, y seguridad local — que de hecho disparó al
cruzar el techo). El **ítem 2 (magnetómetro)** quedó **diagnosticado y con base de firmware lista**
(I²C master mode), pero validar los 3 mags necesita aislar cada AK8963 (**mux** ya comprado, sin montar)
— ver ítem 2.

**Ya habilitó:** la **detección de compensación en el edge** (§13.1, ítem 3) — que
dependía de la muestra enriquecida de aquí — ya está construida (Ola 2); su validación con
señal real depende del ítem 2 (magnetómetro en placa).

---

## 14. Resumen de decisiones

- Registro por repetición, idempotente.
- Edge = detección; backend = registro; móvil = orquestación/UI; embedded =
  sensar/actuar.
- `serialNumber` como identidad global; `serial_number` clave natural en el edge.
- Camino vivo `edge -> móvil` (SSE, LAN) + BLE; registro autoritativo en el
  backend.
- Seguridad local en el embedded; feedback de resultado de rep desde el edge
  (opcional).
- Completitud de serie por totales, con clasificación por repetición.
- ROM objetivo (span relativo) + techo seguro derivado (no almacenado); sin
  mínimo prescrito; `AngleThreshold(0, ROM)` reemplazado.
- Resumen de serie calculado por el backend desde las repeticiones.
- Movimiento compensatorio en alcance (muestra enriquecida + detección en edge +
  rename en backend).
- Magnetómetro operativo en alcance; filtro Mahony se mantiene.
- Backend: implementado y verificado end-to-end (§13.2).
- Auth edge→backend: implementada y verificada (§13.0.a). Edge↔móvil (SSE): **asegurada con rendezvous por backend + token de pairing por sesión** (§13.0.b) — **validada E2E en LAN (2026-06-23)** en dispositivo físico; TLS/mDNS = follow-ons.
- Edge (§13.1): dominio + detección incremental **+ compensación** + outbox + forwarding + correlación **+ SSE** — **todo implementado y unit-testeado** (Olas 1–2).
- Móvil (§13.3): ejecución, gauge BLE, dolor, finalizar, **calibración guiada** y **progreso por polling + SSE** — hecho; Fase 3 verificada en dispositivo.
- Embedded (§13.4): muestra enriquecida + cadencia, canal de bajada, seguridad local y magnetómetro (código) — **hecho** (Olas 1–2); builds sim+hw OK.
- **Próximo paso:** **integración de hardware en una tanda** — montar el **multiplexor I²C** (ya comprado) para los 3 magnetómetros (el master mode ya está en el firmware), la **batería/telemetría** y lo que toque. Mientras tanto el board corre el firmware **bypass** validado (Ola 1); alternativa para la compensación sin mux: **Plan B** (6-DOF). El rendezvous+SSE ya quedó **validado E2E en LAN (2026-06-23)**; restan **TLS** y **mDNS** como follow-ons. Ver "Estado de implementación (Olas 1–2)" arriba.
