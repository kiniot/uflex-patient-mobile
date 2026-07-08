# uFlex — Guía de prueba E2E (de cero)

Cómo levantar y probar el **lazo completo de una sesión de terapia** de punta a punta:
**embedded (kit ESP32) → edge → backend → móvil**, incluyendo el **descubrimiento del edge**
(rendezvous + token), la conexión BLE, y el ciclo de sesión.

> Asume que en el backend **ya existen**: un **paciente** (con login), un **device/kit** registrado
> y **asignado** a ese paciente, y un **plan de tratamiento activo** con al menos una rutina.
> Para el diseño del lazo, ver [`EXECUTION-CONTRACT.md`](EXECUTION-CONTRACT.md).

---

## 0. El lazo en 30 segundos

```
EMBEDDED (ESP32)  --muestras {target_angle, proximal_signal} ~10Hz, WiFi-->  EDGE
       │  --ángulo en vivo + actuadores (BLE)-->  MÓVIL
EDGE   --detecta/clasifica reps + compensación-->  outbox
       --per-rep idempotente (HTTP, ROLE_EDGE)-->  BACKEND
       --progreso en vivo (SSE, LAN, con token)-->  MÓVIL
       <--GET active/by-device (serie+targets+token)--  BACKEND
       --reporta su URL de LAN (PUT lan-url)-->  BACKEND      ← descubrimiento
MÓVIL  --ciclo de sesión + dolor + lecturas (HTTP+JWT)-->  BACKEND
       --GET /patients/me/edge-connection {url, token}-->  abre el SSE del edge
```

**Identidad que ata todo:** el **`serialNumber` del kit** (dev: `uflex-kit-001`). Debe ser
**idéntico** en los 4 componentes (ver §1). Puertos: **backend 8080**, **edge 5050**,
**Postgres 5432**.

---

## 1. ⚠️ Consistencia de identidad y de red (la causa #1 de fallos)

Antes de nada, verifica que estos valores **coinciden**:

| Valor | Dónde se define | Dev |
|---|---|---|
| Serial del kit | firmware `include/config/build_config.h` (`UFLEX_SERIAL_NUMBER`) | `uflex-kit-001` |
| Serial del device | backend (device asignado al paciente) | `uflex-kit-001` |
| Serial de la cuenta del edge | backend `EdgeServiceAccount` (§3) | `uflex-kit-001` |
| Serial que sirve el edge | env `UFLEX_KIT_SERIAL` | `uflex-kit-001` |
| API key del kit | firmware `.env` `UFLEX_DEVICE_API_KEY` = edge seeded kit | `test-api-key-123` |

> Si el serial de la cuenta del edge **no** coincide con el del device del paciente, el backend
> devuelve **403** en `active/by-device` (least-privilege por serial) y el edge nunca obtiene la serie.

**Misma LAN:** el teléfono, el kit y la laptop (backend+edge) deben estar en la **misma red WiFi**.
Anota la **IP de LAN de la laptop** (la usarás en el firmware y en el móvil):

```bash
# macOS
ipconfig getifaddr en0        # p.ej. 192.168.1.4
# Linux
hostname -I | awk '{print $1}'
```

> La IP es DHCP y **cambia** entre redes/reinicios. Si cambia, actualiza el firmware `.env`
> (`UFLEX_EDGE_HOST`) y el `API_BASE_URL` del móvil (§5). La URL del edge para el móvil **no** se
> hardcodea (la descubre por el backend, §6), pero el `API_BASE_URL` del backend **sí** es de build.

---

## 2. Backend (`uflex-rest-api`)

```bash
cd uflex-rest-api
docker compose up -d                          # Postgres dev (uflex_db_dev / admin / password : 5432)
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run     # (Windows: ./run-dev.ps1)
```

- Requiere env: `SPRING_PROFILES_ACTIVE=dev`, `JWT_SECRET`, `JWT_EXPIRATION_DAYS` (ver
  `src/main/resources/application*.yaml` y el `.env` del repo). Las vars de DB están defaulteadas en `dev`.
- Escucha en `:8080`. Docs interactivos: **`http://localhost:8080/scalar`**.

**Verificar:**
```bash
curl -s -X POST http://localhost:8080/api/v1/authentication/sign-in \
  -H 'Content-Type: application/json' \
  -d '{"email":"<PACIENTE_EMAIL>","password":"<PACIENTE_PASS>"}'
# -> { "token": "...", ... }  (guárdalo como JWT del paciente)
```

---

## 3. Cuenta de servicio del edge (`ROLE_EDGE`) — una sola vez por kit

El edge se autentica contra el backend con una **cuenta de servicio `ROLE_EDGE`** ligada al serial del
kit. Provisiónala (necesitas un JWT de **`ROLE_DEVELOPER`** o **`ROLE_CLINIC_ADMIN`**):

```bash
# 1) login como developer/clinic-admin -> JWT_ADMIN
ADMIN=$(curl -s -X POST http://localhost:8080/api/v1/authentication/sign-in \
  -H 'Content-Type: application/json' \
  -d '{"email":"<ADMIN_EMAIL>","password":"<ADMIN_PASS>"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

# 2) provisionar la cuenta del edge para el serial del kit
#    (developer: clinicId obligatorio; clinic-admin: se infiere de su clínica)
curl -s -X POST http://localhost:8080/api/v1/iam/edge-service-accounts \
  -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"serialNumber":"uflex-kit-001","clinicId":"<CLINIC_ID_DEL_PACIENTE>"}'
# -> { "email":"edge-uflex-kit-001@uflex.local", "password":"<generado-una-sola-vez>", ... }
```

> Guarda ese `{email, password}`: **solo se muestra una vez**. La clínica (`clinicId`) debe ser la
> **misma** del paciente/device. Si ya existe una cuenta para ese serial, devuelve **409**.

---

## 4. Edge (`uflex-edge-gateway`)

```bash
cd uflex-edge-gateway
python -m venv .venv && source .venv/bin/activate     # (Windows: .venv\Scripts\activate)
pip install -r requirements.txt
```

Configura el entorno (el edge lee de **variables de entorno**; copia `.env.example` → `.env` y
**expórtalas** — no hay auto-carga de `.env`):

```bash
# .env
UFLEX_BACKEND_URL=http://localhost:8080
UFLEX_EDGE_EMAIL=edge-uflex-kit-001@uflex.local
UFLEX_EDGE_PASSWORD=<el-password-del-paso-3>
UFLEX_KIT_SERIAL=uflex-kit-001
# UFLEX_EDGE_LAN_PORT=5050   # opcional (default 5050)
```
```bash
set -a && source .env && set +a      # exporta las vars al shell
python -m app.main                   # escucha en 0.0.0.0:5050
```

Al primer request, el edge: crea su SQLite (`uflex_edge.db`), **siembra el kit dev**
(`uflex-kit-001` / `test-api-key-123`) y arranca los workers de fondo (poller + forwarding). El
**poller reporta automáticamente la URL de LAN del edge** al backend y poll-ea `active/by-device`.

**Verificar:**
```bash
curl -s http://localhost:5050/status            # {"status":"ok",...}
# confirmar que reportó su URL (como ROLE_EDGE no hace falta; míralo vía el móvil en §6, o en logs)
```
> Para arrancar en cero: borra `uflex_edge.db*` y reinicia (se reseembra el kit dev).
> Si ya corriste pruebas viejas y hay outbox stale (logs `Forward ... HTTP 400` en bucle), bórralo igual.

---

## 5. Embedded (`uflex-embedded-app`) — kit ESP32 real

```bash
cd uflex-embedded-app
cp .env.example .env
```
Edita `.env` con tu WiFi real y la **IP de LAN de la laptop** (§1). **Si el SSID tiene espacios,
ponlo entre comillas** (el `platformio.ini` ya soporta espacios en los build flags):

```bash
# .env
UFLEX_WIFI_SSID="MI WIFI"
UFLEX_WIFI_PASSWORD="mi-password"
UFLEX_EDGE_HOST="192.168.1.4"            # IP de la laptop (donde corre el edge)
UFLEX_DEVICE_API_KEY="test-api-key-123"  # = kit dev sembrado en el edge
```

Compila y flashea (con la placa por USB), luego mira el serial:

```bash
./scripts/build_hw.sh -t upload          # (Windows: .\scripts\build_hw.ps1 -t upload)
pio device monitor -b 115200             # serial @115200
```

**Verificar en el serial:** WiFi conecta, las IMUs inicializan, y aparecen líneas
`edge: batch published (... HTTP 201)` (el kit POSTea al edge) y el poll de `active-context` (200).

> **Magnetómetro/compensación:** verás `magnetometer read skipped` en las IMUs del bus compartido —
> es la limitación de hardware conocida (colisión I²C; necesita el **multiplexor**). **No afecta el
> conteo de reps** (usa el ángulo de flexión). Detalle en `EXECUTION-CONTRACT.md` §13.4 ítem 2.
> El target de simulación es `pio run -e esp32_sim` (Wokwi) si no tienes placa.

---

## 6. Móvil (`uflex-patient-mobile`)

Apunta las URLs del build **al backend en la LAN** (para dispositivo físico). En
[`app/build.gradle.kts`](../app/build.gradle.kts), build type `debug`:

```kotlin
buildConfigField("String", "API_BASE_URL",  "\"http://192.168.1.4:8080/api/v1/\"")  // IP de la laptop
buildConfigField("String", "EDGE_BASE_URL", "\"http://192.168.1.4:5050/\"")          // solo fallback (ver nota)
```

> **Descubrimiento del edge:** la app **ya no usa `EDGE_BASE_URL` como fuente principal**. Pide al
> backend `GET /patients/me/edge-connection` → `{localEdgeUrl, pairingToken}` y abre el SSE en esa
> URL con `Authorization: Bearer <pairingToken>`. `EDGE_BASE_URL` queda como **fallback** (emulador o
> si el edge aún no reportó su URL). El `API_BASE_URL` **sí** debe ser la IP de LAN de la laptop.
> En **emulador** se usan `10.0.2.2` (release build type); para **dispositivo físico**, la IP de LAN.

Instala (teléfono por USB, en la misma WiFi):
```bash
./gradlew :app:installDebug              # o: ./gradlew :app:assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 7. Correr el flujo en la app

1. **Login** con el paciente.
2. Tab **"Dispositivos"** → conectar al kit por **BLE** (escanear → conectar → confirma serial). El
   gauge de **"Ángulo en vivo"** valida la conexión BLE. *(El conteo de reps NO depende del BLE; va
   por SSE. Pero la preparación pide confirmar sensores.)*
3. Tab **"Ejercicios"** → tarjeta del plan activo → **"Iniciar sesión de hoy"**.
   - Esto dispara la preparación: `initiate` → conectar BLE → confirmar sensores → `start`.
   - Si ya hay una sesión activa, la **reanuda** (cae en la pantalla "iniciada").
4. En **"iniciada"** → **"Comenzar ejercicios"** → pantalla **"En sesión"**. Aquí la app:
   - llama a `GET /patients/me/edge-connection`, obtiene `{localEdgeUrl, pairingToken}` y **abre el
     SSE** del edge con el token (descubrimiento + auth, automático).
5. **Iniciar la serie** → paso de **calibración guiada**: *"ponte en posición → Calibrar e iniciar"*,
   **mantén el brazo en el cero ~5 s**. ⚠️ **No te lo saltes:** ancla el cero de sesión del kit; sin
   eso el ángulo queda mal y la **seguridad (buzzer) puede dispararse en falso**.
6. **Mueve el kit** haciendo las repeticiones del ejercicio (flexiones completas). El **contador sube
   por SSE** (optimista) y el **polling de ~2.5 s** reconcilia contra el backend (autoritativo).
7. **"Reportar dolor"** (0–10) cuando quieras. Al completar todas las series → **"Finalizar sesión"**.

> **Gotcha del día de la rutina:** "Iniciar sesión de hoy" requiere que el plan tenga una **rutina
> programada para el día de hoy** (día de la semana). Si no, la app muestra `NO_ROUTINE_FOR_DAY` y no
> deja iniciar → ajusta el horario de la rutina (web clínica/backend) o crea la sesión por curl (§9).

---

## 8. Verificación por tramo (curl / serial / logcat)

```bash
JWT=<jwt-del-paciente>; B=http://localhost:8080/api/v1
# Leg descubrimiento (móvil): URL del edge + token
curl -s "$B/patients/me/edge-connection" -H "Authorization: Bearer $JWT"
#  -> {"localEdgeUrl":"http://192.168.1.4:5050","pairingToken":"...","expiresAt":null}

# Leg SSE (con token): debe responder 200 y emitir 'event: rep' cuando haya reps
TOK=<pairingToken-de-arriba>
curl -N -H "Authorization: Bearer $TOK" \
  "http://192.168.1.4:5050/api/v1/movement-monitoring/progress-stream?serial_number=uflex-kit-001"
# sin token / token malo -> 401

# Leg backend (progreso autoritativo): reps registradas
curl -s "$B/therapy-sessions/<SESSION_ID>/progress" -H "Authorization: Bearer $JWT"

# En el dispositivo (logcat): la app pidiendo edge-connection y abriendo el SSE
adb logcat | grep -iE "edge-connection|progress-stream"
```

---

## 9. (Avanzado) Crear/arrancar la sesión por curl

Útil si hoy no es el día de la rutina, o para automatizar. La app **reanuda** la sesión que crees aquí.

```bash
JWT=<jwt-del-paciente>; B=http://localhost:8080/api/v1
PID=<patientId>; PLAN=<treatmentPlanId>; ROUTINE=<routineId>
SID=$(curl -s -X POST "$B/therapy-sessions" -H "Authorization: Bearer $JWT" -H 'Content-Type: application/json' \
  -d "{\"patientId\":\"$PID\",\"treatmentPlanId\":\"$PLAN\",\"iotDeviceId\":\"uflex-kit-001\",\"routineId\":\"$ROUTINE\"}" \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['id'])")
curl -s -X PATCH "$B/therapy-sessions/$SID/hardware" -H "Authorization: Bearer $JWT" -H 'Content-Type: application/json' -d '{"sensorsPlaced":true}'
curl -s -X PATCH "$B/therapy-sessions/$SID/start"    -H "Authorization: Bearer $JWT"
# obtener el serieId del progreso y arrancarla:
SERIE=$(curl -s "$B/therapy-sessions/$SID/progress" -H "Authorization: Bearer $JWT" | python3 -c "import sys,json;print((json.load(sys.stdin)['seriesProgress'] or [{}])[0]['serieId'])")
curl -s -X PATCH "$B/therapy-sessions/$SID/series/$SERIE/start" -H "Authorization: Bearer $JWT"
```

Para **inyectar reps sin mover el kit** (simular el kit, como en pruebas headless):
```bash
curl -s -X POST http://localhost:5050/api/v1/movement-monitoring/data-records \
  -H "X-API-Key: test-api-key-123" -H 'Content-Type: application/json' \
  -d '{"serial_number":"uflex-kit-001","samples":[{"target_angle":0},{"target_angle":70},{"target_angle":140},{"target_angle":70},{"target_angle":0}]}'
```

---

## 10. Troubleshooting

| Síntoma | Causa probable / arreglo |
|---|---|
| El contador no sube por SSE | El edge no reportó su URL (¿corriendo + autenticado?), no hay sesión activa (token), o el teléfono no está en la misma WiFi. Verifica §8. |
| `GET edge-connection` → 404 | No hay **sesión activa** del paciente (el token se acuña al `initiate`). |
| SSE → 401 | Token ausente/incorrecto, o el edge aún no cacheó el token (espera ~1 poll, ~3 s; el cliente reintenta solo). |
| `active/by-device` → 403 | El serial de la **cuenta del edge** ≠ serial del device del paciente (§1/§3). |
| El kit no llega al edge | WiFi/`UFLEX_EDGE_HOST` mal en el firmware `.env`, o IP de la laptop cambió. Re-flashea (§5). |
| Buzzer suena sin parar | Te saltaste la **calibración guiada** → cero mal anclado → el ángulo cruza `maxSafeAngle`. Re-calibra (§7.5) o power-cicla el kit. |
| `NO_ROUTINE_FOR_DAY` en la app | El plan no tiene rutina para hoy → ajusta el horario o usa la ruta curl (§9). |
| App física no llega al backend | `API_BASE_URL` debe ser la **IP de LAN** de la laptop, no `10.0.2.2` (eso es solo emulador). |
| `magnetometer read skipped` | Esperado sin el **multiplexor** (colisión I²C). No bloquea el conteo de reps; bloquea solo la compensación con señal real. |

---

## 11. Estado y notas

- **Validado E2E en LAN (2026-06-23):** rendezvous+SSE con token y lazo completo con **kit real**
  (reps reales kit→edge→backend→móvil).
- **Re-validado (2026-06-27)** en una **red y dispositivo nuevos** siguiendo esta guía (red `SALIM SW`,
  edge en `192.168.1.10`): kit en WiFi → edge → backend → app, con el contador de reps subiendo por SSE.
  Confirma que el runbook funciona desde cero.
- **Compensación (movimiento compensatorio):** pendiente del **multiplexor I²C** (o del Plan B 6-DOF)
  para tener el yaw proximal real. Ver `EXECUTION-CONTRACT.md` §13.4 ítem 2.
- **Follow-ons de seguridad del canal SSE:** TLS + mDNS (hoy el token autentica; el tráfico va en
  claro en la LAN). Ver `EXECUTION-CONTRACT.md` §13.0.b.
