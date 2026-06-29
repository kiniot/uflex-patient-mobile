# uFlex — Qué sigue (próximos pasos)

Estado y siguiente trabajo del ecosistema uFlex. Para el detalle de diseño y el roadmap completo,
ver [`EXECUTION-CONTRACT.md`](EXECUTION-CONTRACT.md); para el progreso del móvil, [`PROGRESS.md`](PROGRESS.md);
para probar el lazo de cero, [`E2E-TESTING.md`](E2E-TESTING.md).

## Dónde estamos
El **lazo de ejecución de terapia está funcionalmente completo y validado E2E en LAN con kit real**
(kit → edge → backend → móvil, reps reales por SSE; rendezvous + token de pairing). La única pieza
funcional pendiente del lazo es la **detección de compensación**, que depende del magnetómetro.

Además, se investigó en placa el **disparo en falso del buzzer** (seguridad local del kit): el fix de
firmware ya está commiteado, pero reveló dos huecos de fondo — **la sesión no termina al salir** y la
**deriva de yaw**. Detalle y próximos pasos en §3 (es el foco inmediato).

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

## 3. Seguridad local del kit (buzzer) — investigado en placa 2026-06-28
Se reprodujo en placa el "el buzzer se vuelve loco / suena sin hacer nada / suena aunque el móvil ya
no esté en tratamiento". **Dos causas raíz** (medidas en serial):
1. **La sesión nunca termina.** Cerrar la app **no** finaliza/cancela la sesión (no hay acción de
   terminar en la UI de ejecución), así que el backend/edge mantienen la serie activa
   (`active-context` sigue devolviendo `hasCtx=1`) y el firmware sigue **legítimamente armado**.
   Es la causa dominante de "suena cuando ya terminé".
2. **Deriva de yaw.** Con el magnetómetro roto (6-DOF), el ángulo articular calculado **oscila solo
   ~±10° en reposo** y cruza `maxSafeAngle` una y otra vez (visto: TRIGGER 110→clear 99→TRIGGER 108…).
   `absoluteFlexionDegrees` usa la **magnitud completa del cuaternión** (incluye yaw), así que el yaw
   contamina el ángulo de seguridad. La imu2 (`0x69`) además tiene un **bias de giro ~800 LSB** en reposo.

**Fix de firmware — HECHO y commiteado** en `uflex-embedded-app` rama `feature/safety-false-trigger-fix`:
**TTL del contexto** (desarma + `JointAngleCalculator::reset()` si no hay poll OK en 15 s),
**calibración diferida hasta quietud** (settle por gyro, umbral 1200 LSB para librar el bias de imu2,
con fallback por timeout), **histéresis** (limpia ~5° bajo el techo), y **logs de transición**
(`calib:`/`ctx:`/`safety:`). Verificado en placa. **Reemplaza** el viejo "guard de calibración" de
esta lista. **No alcanza solo**: no frena la causa dominante porque el edge mantiene viva la sesión
(el TTL no expira) y la deriva es de hardware.

**Lo nuevo que falta (los arreglos de fondo):**
- **Ciclo de vida de la sesión (móvil) — PRÓXIMO, mayor impacto:** exponer **terminar/cancelar
  sesión** en la pantalla "En sesión" y **cancelar al salir/cerrar** la app. El backend ya tiene
  `PATCH /therapy-sessions/{id}/cancel`; falta cablearlo en el móvil. Al terminar la sesión, el
  firmware desarma por su ruta de pérdida de contexto (ya implementada).
- **Auto-expiración de sesión inactiva (backend/edge) — red de seguridad:** cerrar sola una sesión
  sin señales del kit/móvil por X min, para el caso "cerró la app de golpe" donde el móvil no alcanza
  a avisar.
- **Ángulo de seguridad robusto al yaw (firmware) / mux:** o el **mux/Plan B** (ver §1) para tener yaw
  real, o derivar el ángulo de seguridad solo de la flexión por gravedad (sin yaw) para que no oscile
  en reposo durante una sesión activa.
- **UX de la pantalla de ejecución (anotado como tareas):** no se distingue **cuál de los 3 ángulos**
  es el real (debería etiquetarse según la articulación activa — el edge sí lo sabe) ni **cuánto debe
  mover** el paciente (no se muestra el `targetRom` de la serie).

## 4. Medición por tipo de movimiento (firmware) — al planear la fase de brazo
Hoy el firmware solo recibe el **`active_joint`** (ELBOW/WRIST) del edge y con eso elige el par de
IMUs y mide la **magnitud** de su rotación relativa (codo→`upper-middle` brazo-antebrazo;
muñeca→`middle-lower` antebrazo-mano). **No recibe el `movementType`.** En el backend, `bodyPart` y
`movementType {FLEXION, EXTENSION, PRONATION, SUPINATION}` son campos **independientes** del `Exercise`
(sin acoplamiento). Como **cada serie es un solo movimiento conocido**, medir bien cada uno es tratable.

- **Flexión/extensión:** OK tal cual. La magnitud del par del joint es el ángulo del movimiento
  (codo→`upper-middle`, muñeca→`middle-lower`).
- **Pronación/supinación: hueco real.** Es rotación del antebrazo sobre su eje largo; **la mano gira
  junto con el antebrazo**, así que el par `middle-lower` (antebrazo-mano) ve **~0°**. Hay que medirlo
  con el **brazo como referencia quieta**: `upper-middle` (brazo-antebrazo, el más limpio, excluye la
  muñeca) o `upper-lower` (brazo-mano, alternativa robusta al montaje pero suma movimiento de muñeca).

**Mapeo recomendado (cada serie = un movimiento):** codo flex/ext → `upper-middle`; muñeca flex/ext →
`middle-lower`; pron/sup → `upper-middle` (o `upper-lower`). El `bodyPart` **no** debe decidir el par
para pron/sup — lo decide el **movimiento**.

**Arreglo:** el edge **ya tiene** `movement_type` en su `ExecutionContext`; falta (1) **incluirlo en el
payload `active-context`** y (2) que el **firmware elija el par por movimiento** (no solo por joint).
Convención de uso: en pron/sup, **mantener el brazo quieto** (es la referencia). (Calidad: el backend
no valida combos `bodyPart`×`movementType` → añadir validación o convención.)

> **Nota — la orientación de montaje de los sensores NO afecta la medición.** Ni la cara hacia la que
> apunta cada IMU, ni su giro sobre su propio eje. El sistema mide **rotación relativa** entre IMUs
> adyacentes y resta un **cero por sesión**, así que cualquier offset fijo de montaje se cancela (la
> magnitud del ángulo es invariante a esa "conjugación"). Lo único que importa al montar es la
> **rigidez** (que el sensor no se deslice respecto al segmento) y **calibrar en una pose de referencia
> consistente**. Conclusión para la fase de brazo: montar como sea más cómodo/estable/limpio de cable.

## 5. Calidad / pulido
- **Edge:** refactor de arquitectura (ACL real IAM↔Detection, app factory + sacar el bootstrap de
  `before_request`) + **OpenAPI generado** (spectree+Pydantic) + true-up de docs viejos del edge
  (`movement-monitoring-api.md` / `demo-expo.md` aún describen el lifecycle `series/start-end`).
- **Móvil:** centralizar el fix de 404 en `core/network/ApiErrorMapper` (preferir el status HTTP cuando
  el código de dominio no se reconoce); `requestConnectionPriority(HIGH)` para el gauge BLE.
- **Dev-data:** provisionar la cuenta `ROLE_EDGE` por el serial real del kit (evitar mismatches).

## 6. Superficie de producto fuera del lazo de terapia
- App del paciente: **tab "Inicio"** (hoy placeholder), **historial** de sesiones, **sign-up /
  verificación de email** (no implementados), **forgot-password**.
- **Web clínica** (`uflex-clinic-web`): historial/métricas.

---

**Prioridad sugerida:** (1) **ciclo de vida de la sesión** en el móvil (§3) — mata el síntoma del
buzzer "suena tras terminar" y es contenido; (2) **medición por tipo de movimiento** (§4) — chico
(edge+firmware) y desbloquea pron/sup correctamente; (3) la tanda de hardware (mux + batería, §1) que
además da el yaw real para la compensación y el ángulo de seguridad; (4) seguridad del SSE (§2);
(5)/(6) calidad y roadmap de producto.
