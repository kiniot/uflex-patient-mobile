# uFlex — Qué sigue (próximos pasos)

Estado y siguiente trabajo del ecosistema uFlex. Para el detalle de diseño y el roadmap completo,
ver [`EXECUTION-CONTRACT.md`](EXECUTION-CONTRACT.md); para el progreso del móvil, [`PROGRESS.md`](PROGRESS.md);
para probar el lazo de cero, [`E2E-TESTING.md`](E2E-TESTING.md).

## Dónde estamos
El **lazo de ejecución de terapia está funcionalmente completo y validado E2E en LAN con kit real**
(kit → edge → backend → móvil, reps reales por SSE; rendezvous + token de pairing). La única pieza
funcional pendiente del lazo es la **detección de compensación**, que depende del magnetómetro.

## 1. Integración de hardware (la próxima tanda) — recomendado hacerlo junto
- **Multiplexor I²C (TCA9548A, ya comprado, sin montar):** aísla cada AK8963 → habilita los **3
  magnetómetros** y por tanto la **compensación** con yaw real. El firmware ya tiene el **I²C master
  mode** como base; al montar el mux hay que añadir el **select de canal** (o simplificar a
  bypass-por-canal). Detalle: `EXECUTION-CONTRACT.md` §13.4 ítem 2.
  - *Alternativa sin mux:* **Plan B** — compensación por rango de yaw sobre 6-DOF con el bias del giro
    calibrado (cierra la compensación sin hardware nuevo).
- **Batería / telemetría de kit-status:** hoy no implementada (edge README "Not implemented yet").
- Tras montar: validar la **compensación E2E** (mover el brazo proximal con el codo estancado →
  `ShoulderCompensation` → edge → backend).

## 2. Seguridad del canal SSE (follow-ons) — `EXECUTION-CONTRACT.md` §13.0.b
El token de pairing ya autentica el SSE. Falta:
- **TLS** del canal en la LAN (hoy el tráfico va en claro; el token solo autentica).
- **mDNS** para descubrir el edge sin pasar por la nube (hoy el descubrimiento es por el backend).

## 3. Firmware — guard de seguridad/calibración
Hallazgo de las pruebas: si la serie arranca **sin la calibración guiada** (cero de sesión mal
anclado), el `target_angle` puede cruzar `maxSafeAngle` y el **buzzer suena en continuo**. Añadir un
guard: no enforcar la seguridad hasta una primera lectura/calibración válida.

## 4. Calidad / pulido
- **Edge:** refactor de arquitectura (ACL real IAM↔Detection, app factory + sacar el bootstrap de
  `before_request`) + **OpenAPI generado** (spectree+Pydantic) + true-up de docs viejos del edge
  (`movement-monitoring-api.md` / `demo-expo.md` aún describen el lifecycle `series/start-end`).
- **Móvil:** centralizar el fix de 404 en `core/network/ApiErrorMapper` (preferir el status HTTP cuando
  el código de dominio no se reconoce); `requestConnectionPriority(HIGH)` para el gauge BLE.
- **Dev-data:** provisionar la cuenta `ROLE_EDGE` por el serial real del kit (evitar mismatches).

## 5. Superficie de producto fuera del lazo de terapia
- App del paciente: **tab "Inicio"** (hoy placeholder), **historial** de sesiones, **sign-up /
  verificación de email** (no implementados), **forgot-password**.
- **Web clínica** (`uflex-clinic-web`): historial/métricas.

---

**Prioridad sugerida:** (1) la tanda de hardware (mux + batería) cierra la última funcionalidad del
lazo; luego (2) seguridad del SSE y (3) el guard de calibración; (4)/(5) según el roadmap del producto.
