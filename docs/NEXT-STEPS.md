# uFlex — Qué sigue (próximos pasos)

Estado y siguiente trabajo del ecosistema uFlex. Para el detalle de diseño y el roadmap completo,
ver [`EXECUTION-CONTRACT.md`](EXECUTION-CONTRACT.md); para el progreso del móvil, [`PROGRESS.md`](PROGRESS.md);
para probar el lazo de cero, [`E2E-TESTING.md`](E2E-TESTING.md).

## Dónde estamos (2026-07-06)

El **lazo de terapia + detección está funcionalmente completo y validado en placa de punta a punta**,
incluida la **detección de compensación** (la última pieza funcional, ahora cerrada). Lo que se cerró en
la última tanda:

- **Mux TCA9548A en firmware — HECHO y validado.** Migrado de dos buses a **un bus + select de canal**
  (`1<<n → 0x70`) con **bypass-por-canal** para el magnetómetro; tolera el IMU con el AK8963 muerto
  (mano/ch2 → 6-DOF sin spam). Mapeo: bíceps=ch1 (proximal), antebrazo=ch0, mano=ch2. Motor vibrador
  descartado → **seguridad local = solo buzzer**.
- **Ángulo de flexión anclado a gravedad — HECHO.** `JointAngleCalculator` ya no usa la magnitud completa
  del cuaternión (que incluía yaw y se inflaba); ahora es la **distancia accel-based (pitch,roll) desde el
  cero de sesión**, inmune a la deriva de yaw, acotada a [0,180]. Esto **resolvió el buzzer en falso**
  (ya no cruza `maxSafeAngle` en reposo) y el **`achieved_rom` inflado** (reps "good" sin llegar al ROM).
- **Gauge del móvil correcto — HECHO.** El firmware manda ese ángulo calibrado por BLE (frame 53→59 bytes:
  `jointFlexionDegrees`+`isCalibrated`+`activeJoint`) y el gauge lo muestra: **0 al calibrar, sigue el joint
  activo (no la muñeca), sin drift**. Reemplaza el `upperLowerRotation` crudo anterior.
- **Compensación E2E — VALIDADA en placa.** Con el yaw proximal (bíceps) ya real (mux), el
  `CompensationDetector` del edge dispara `ShoulderCompensation` (proximal barre ≥15° con el codo
  estancado ≤10°) → forward al backend. Logs de observabilidad añadidos (detectada / reenviada).
- **Ciclo de vida de la sesión (móvil) — HECHO.** Botón **Terminar sesión** + back con confirmación en la
  pantalla "En sesión" → `PATCH .../cancel` + desconecta BLE. Mata la causa dominante del buzzer
  "suena tras terminar".
- **Robustez del outbox (edge) — HECHO.** El `ForwardingWorker` **cuarentena** (marca FAILED, salta) los
  rechazos permanentes 4xx (body inválido, 404 de sesión cancelada) en vez de reintentar para siempre;
  ya no hay *head-of-line blocking* que atasque la cola.

## 1. Medición por tipo de movimiento — pron/sup (el último hueco funcional)

Hoy el firmware elige el par de IMUs por **articulación** (`active_joint` ELBOW/WRIST) y mide su ángulo
relativo. **Codo flex/ext y muñeca flex/ext funcionan** (`upper-middle` y `middle-lower`). El hueco es la
**pronación/supinación**: es rotación del antebrazo sobre su eje, y **la mano gira junto con el antebrazo**,
así que el par `middle-lower` ve **~0°**. Hay que medirla con el **brazo como referencia quieta** →
par `upper-middle` (o `upper-lower`).

- **Arreglo:** el edge ya tiene `movement_type` en su `ExecutionContext`; falta (1) **incluirlo en
  `active-context`** y (2) que el **firmware elija el par por movimiento, no solo por articulación**.
  Convención: en pron/sup, mantener el brazo quieto (es la referencia). *(Calidad: el backend no valida
  combos `bodyPart`×`movementType` → añadir validación o convención.)*
- Chico (edge + firmware) y completa la medición de los 4 movimientos.

> **Nota — el montaje de los sensores NO afecta la medición.** El sistema mide **rotación relativa** entre
> IMUs adyacentes y resta un **cero por sesión**, así que cualquier offset fijo de montaje se cancela. Lo
> que importa es **rigidez** (que no se deslice) y **calibrar en una pose de referencia consistente**.

## 2. Auto-expiración de sesión inactiva (backend/edge) — red de seguridad

El móvil ya cancela al terminar/salir, pero **cerrar la app de golpe** (kill del proceso) no avisa. Falta
una red de seguridad server-side: **cerrar sola una sesión sin señales del kit/móvil por X min**. Cubre el
caso donde el móvil no alcanza a mandar el `cancel`. (Decidido fuera de alcance del fix de móvil; es su
complemento natural.)

## 3. Seguridad del canal SSE (follow-ons) — `EXECUTION-CONTRACT.md` §13.0.b

El token de pairing ya autentica el SSE. Falta:
- **TLS** del canal en la LAN (hoy el tráfico va en claro; el token solo autentica).
- **mDNS** para descubrir el edge sin pasar por la nube (hoy el descubrimiento es por el backend).

## 4. Calidad / pulido

- **Edge:** refactor de arquitectura (ACL real IAM↔Detection, **app factory** + sacar el bootstrap de
  `before_request`) + **OpenAPI generado** (spectree+Pydantic) + true-up de docs viejos del edge
  (`movement-monitoring-api.md` / `demo-expo.md` aún describen el lifecycle `series/start-end`).
  *(Reintento/purga de las entradas FAILED en cuarentena queda como follow-on del outbox.)*
- **Móvil:** centralizar el fix de 404 en `core/network/ApiErrorMapper` (preferir el status HTTP cuando el
  código de dominio no se reconoce); `requestConnectionPriority(HIGH)` para suavizar el gauge BLE; mostrar
  el **`targetRom` de la serie** en ejecución (cuánto debe mover el paciente).
- **Compensación:** afinar umbrales (`COMPENSATION_PROXIMAL_RANGE_DEG`, ventana) con más datos reales; el
  primer disparo en placa fue `proximal_range≈23.5`, `angle_range≈9.9`.
- **Dev-data:** provisionar la cuenta `ROLE_EDGE` por el serial real del kit (evitar mismatches).
- **Hardware:** batería / telemetría de kit-status (edge README "Not implemented yet").

## 5. Superficie de producto fuera del lazo de terapia

- App del paciente: **tab "Inicio"** (hoy placeholder), **historial** de sesiones, **sign-up /
  verificación de email** (no implementados), **forgot-password**.
- **Web clínica** (`uflex-clinic-web`): historial/métricas.

---

**Prioridad sugerida:** (1) **pron/sup** (§1) — el último hueco funcional de medición, chico; (2)
**auto-expiración de sesión** (§2) — cierra el caso "cerró la app de golpe"; (3) **calidad del edge** (§4:
refactor + OpenAPI + docs); (4) **seguridad del SSE** (§3); (5) **roadmap de producto** (§5). El lazo
funcional (reps + seguridad + gauge + compensación + ciclo de sesión) ya está cerrado y validado en placa.
