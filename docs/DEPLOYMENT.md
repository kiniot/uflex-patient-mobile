# uFlex — Configuración por red y despliegue (LAN local vs backend en la nube)

Qué ajustar para probar el sistema **en otra red** (calle, expo, casa de un amigo) y qué cambia cuando el
**backend está en la nube** y la app va en **release** en un dispositivo físico. Para el bootstrap desde
cero (crear admin/paciente/plan/kit) ver [`E2E-TESTING.md`](E2E-TESTING.md); para el diseño del lazo,
[`EXECUTION-CONTRACT.md`](EXECUTION-CONTRACT.md).

---

## Modelo mental: dos planos

uFlex tiene **dos planos de comunicación**, y casi toda la configuración depende de cuál toques:

- **Plano nube (por internet):** móvil ↔ backend, y **edge ↔ backend**.
- **Plano LAN local (misma WiFi):** kit → edge (muestras), móvil → edge (SSE de progreso en vivo),
  móvil ↔ kit (BLE, gauge de ángulo).

> **El edge SIEMPRE corre local, junto al kit** — el kit lo alcanza por WiFi de la LAN; ese es su punto
> (detección de baja latencia + SSE local). El edge **no** se va a la nube aunque el backend sí.

**Identidad que ata todo:** el `serialNumber` del kit (dev: `uflex-kit-001`). Puertos: backend `8080`,
edge `5050`, Postgres `5432`.

---

## Lo que cambia por RED (y lo que no)

| Cosa | ¿Cambia por red? | Dónde | ¿Recompilar/reflashear? |
|---|---|---|---|
| WiFi (SSID/password) | **Sí** | firmware `.env` | **re-flashear** el kit |
| IP LAN del **edge** (para el kit) | **Sí** (si el backend NO está en la nube, también la del backend) | firmware `.env` (`UFLEX_EDGE_HOST`) | **re-flashear** el kit |
| URL del **backend** (para el móvil) | **Sí** si es local; **No** si está en la nube (dominio fijo) | móvil `build.gradle.kts` (`API_BASE_URL`) | **re-instalar** la app |
| URL del backend (para el edge) | igual que arriba | edge `.env` (`UFLEX_BACKEND_URL`) | reiniciar el edge |
| URL LAN del edge (para el móvil) | **automático** — el edge la reporta al backend y el móvil la pide | — | — |
| Data (admin/clínica/paciente/plan/device/cuenta edge) | **No** — persiste en Postgres | — | — |

**El teléfono debe estar en la misma WiFi que el edge + kit** para el SSE y el BLE, aunque el backend esté
en la nube (el lazo vivo es local). El **BLE (gauge)** funciona sin WiFi; solo el conteo de reps necesita
la ruta WiFi→edge.

---

## Escenario A — misma laptop, otra red (todo local)

La data no cambia; solo la WiFi y la IP de la laptop (que corre backend + edge + Postgres).

1. **IP de la laptop en la red nueva:**
   ```bash
   ipconfig getifaddr en0        # macOS
   ```
2. **Firmware — `uflex-embedded-app/.env`** (build-time → re-flashear):
   ```bash
   UFLEX_WIFI_SSID="red-nueva"          # entre comillas si tiene espacios
   UFLEX_WIFI_PASSWORD="password-nuevo"
   UFLEX_EDGE_HOST="192.168.x.x"        # ← IP nueva de la laptop (donde corre el edge)
   UFLEX_DEVICE_API_KEY="test-api-key-123"   # NO cambia
   ```
   ```bash
   cd uflex-embedded-app && ./scripts/upload_hw.sh
   ```
3. **Móvil — `uflex-patient-mobile/app/build.gradle.kts`** (buildConfig `debug` → re-instalar):
   ```kotlin
   buildConfigField("String", "API_BASE_URL", "\"http://192.168.x.x:8080/api/v1/\"")  // IP nueva
   ```
   ```bash
   cd uflex-patient-mobile && ./gradlew :app:installDebug
   ```
4. **Reiniciar el stack** (Postgres, backend, edge). El edge **reporta solo** su URL de LAN nueva al
   backend → no se toca. (Edge → backend usa `localhost:8080`, no cambia.)

---

## Escenario B — backend en la NUBE + app en release (dispositivo físico)

El backend en la nube **fija su URL** → desaparece la IP de la laptop para el backend (móvil y edge).

**Lo que MEJORA:**
- **Móvil `API_BASE_URL`** → dominio de la nube (`https://api.tudominio.com/api/v1/`), **estable en
  cualquier red**. Se pone **una vez** en el buildConfig de **release**.
- **Edge `.env` `UFLEX_BACKEND_URL`** → el dominio de la nube (antes `localhost:8080`). También fijo.
- El **móvil solo necesita internet** para el backend (ya no la misma LAN que el backend).

**Lo que SIGUE local/manual** (porque el edge sigue local):
- **Firmware `.env`:** WiFi + `UFLEX_EDGE_HOST` (IP LAN del edge) + **re-flashear**, por sitio.
- El **teléfono debe estar en la misma WiFi que el edge + kit** (SSE + BLE).

**Cosas nuevas por nube/release:**
- **HTTPS para el backend**, pero **mantener cleartext permitido para el edge en la LAN** (el SSE al edge
  es `http://` sobre IP privada; TLS del edge es follow-on). Vive en
  `app/src/main/res/xml/network_security_config.xml` — **no** quitar el cleartext, se rompería el SSE local.
- **Release del móvil:** poner el `API_BASE_URL` de la nube en el buildConfig de `release` (hoy release usa
  `10.0.2.2`, de emulador), y usar una **llave de firma** (release no usa la debug).
- **Backend en prod:** su propia config — `SPRING_PROFILES_ACTIVE=prod`, `JWT_SECRET`, DB de prod,
  `EMAIL_*` (contraseñas temporales de paciente/fisio), Stripe (o bypass). Y la **migración de esquema**:
  prod usa `ddl-auto: validate` **sin** Flyway → aplicar a mano el `roles_name_check` (ROLE_DEVELOPER/
  ROLE_EDGE), `devices.clinic_id` nullable, `devices.status` con `IN_STOCK`. Ver `uflex-rest-api`.
- **Provisión de data:** se hace **una vez** contra la nube (developer-web/clinic-web apuntando al dominio)
  y persiste — no se rehace por sitio.

**Por sitio (con backend en nube):** correr un **edge local** (apuntando a la nube + su cuenta `ROLE_EDGE`),
**flashear el kit** para la WiFi del sitio + la IP LAN del edge, y el **móvil (release) apunta a la nube**
(fijo). Teléfono + kit + edge en la misma WiFi.

---

## ⚠️ Gotcha de expos: WiFi público con aislamiento de clientes

Muchas redes públicas/de eventos tienen **AP isolation**: los dispositivos **no se ven entre sí**, así que
el teléfono y el kit **no alcanzan al edge/laptop** aunque las IPs estén bien → nada funciona.

**Solución (y ahorra re-flashear cada vez): lleva tu propio router de viaje o usa el hotspot de un teléfono.**
- **SSID/password fijos** → flasheas el firmware **una sola vez**.
- IP **estable** → `UFLEX_EDGE_HOST` (y `API_BASE_URL` si es local) se ponen **una vez**.
- Sin aislamiento → los 3 se ven.

---

## Otros detalles
- **Firewall de macOS:** permitir entrantes al backend (:8080) / edge (:5050) desde teléfono/kit (o
  desactivarlo para la demo).
- **AirPlay :5000:** ya evitado — el stack usa :5050. No volver a 5000.
- **El único "IP hardcodeado" que sobrevive** con backend en la nube es el `UFLEX_EDGE_HOST` del firmware.
  Se eliminaría el día que se implemente **mDNS** (descubrimiento del edge sin IP fija — follow-on en
  `EXECUTION-CONTRACT.md` §13.0.b) o usando un router de IP fija.

---

## Checklist rápido por sitio nuevo

- [ ] Todos (teléfono, kit, edge/laptop) en la **misma WiFi**, **sin** aislamiento de clientes.
- [ ] Firmware `.env`: WiFi + `UFLEX_EDGE_HOST` (IP del edge) → **re-flashear**.
- [ ] Edge `.env`: `UFLEX_BACKEND_URL` (localhost si local, dominio si nube) → correr con
      `set -a && source .env && set +a; python -m app.main`.
- [ ] Móvil: `API_BASE_URL` (IP de la laptop si local; dominio fijo si nube) → re-instalar (o ya fijo en release).
- [ ] Backend + Postgres arriba (local) **o** alcanzable en la nube.
- [ ] Firewall permite :8080 / :5050 (si es local).
