# Navegación — shell de tabs + pantallas de detalle

Cómo está estructurada la navegación de la app del paciente, por qué, y una
limitación conocida de la animación de "atrás". Para la arquitectura general ver
[`ARCHITECTURE.md`](ARCHITECTURE.md) y [`../AGENTS.md`](../AGENTS.md).

## Estructura

La jerarquía es `RootNavGraph → MainGraph`, y **las pantallas de detalle son
hermanas de `MainShellRoute` dentro de `MainGraph`** (no están anidadas dentro del
shell):

```
RootNavGraph (NavHost raíz, start = Splash)
├── Splash
├── AuthGraph
└── MainGraph (start = MainShellRoute)
    ├── MainShellRoute        → MainShell  (tabs: Inicio/Dispositivos/Ejercicios/Historial)
    ├── ExerciseDetailRoute
    ├── SessionPreparationRoute
    ├── SessionExecutionRoute
    ├── ProfileRoute
    └── EditContactInfoRoute
```

- **`MainShell`** (`features/main/presentation/shell/MainShell.kt`) es **solo el
  shell de tabs**: top bar, bottom bar y las cuatro pantallas de tab. Navega a los
  detalles con el `navController` raíz vía callbacks (`onNavigateToExerciseDetail`,
  `onNavigateToSessionPreparation`, `onNavigateToProfile`).
- Los **detalles** se registran en `features/main/navigation/MainNavigation.kt`
  (`mainGraph`), cada uno envuelto en `OverlayScreenContainer` (dibuja su propio top
  bar, full-screen).
- **`OverlayScreenContainer.kt`** contiene ese contenedor + las cuatro transiciones
  de barrido cruzado compartidas.

### Por qué las detalle son hermanas del shell (y no un segundo NavHost)

La versión previa montaba **dos NavHost vivos a la vez** dentro de `MainShell` (uno
para tabs, otro "overlay" para detalles). Ambos registraban su callback en el
`OnBackPressedDispatcher`, así que el botón **atrás del sistema** se consumía dos
veces: el primer back popeaba el tab (Ejercicios→Inicio) *por detrás* del overlay
aún visible, y el segundo cerraba el overlay → **había que presionar atrás dos veces
y aterrizabas en Inicio en vez de Ejercicios**. La flecha del top bar no sufría
porque llamaba directo a `popBackStack()`.

Al promover los detalles a `MainGraph`, el shell y un detalle **nunca coexisten en
composición** → un único back stack, un único dispatcher: **un solo "atrás" vuelve
al tab de origen**, igual que la flecha del top bar.

### Tabs con conmutador ligero (no NavHost anidado)

`MainShell` cambia de tab con un índice (`rememberSaveable`) + `SaveableStateHolder`,
**no** con un `NavHost` anidado. Es intencional (ver la limitación de abajo): un
`NavHost` con rutas tipadas es caro de reconstruir. El estado por tab (scroll, etc.)
se preserva vía el `SaveableStateHolder`. *Comportamiento:* atrás desde un tab
secundario vuelve a **Inicio**; desde Inicio, sale de la app (estándar de bottom nav).

## Transiciones

Barrido cruzado, sin fade, coherente en ambos sentidos (`OverlayScreenContainer.kt`,
280 ms):

- **Abrir** (shell → detalle): el detalle entra desde la derecha; el shell sale hacia
  la izquierda.
- **Volver** (detalle → shell): el detalle sale a la derecha; el shell entra desde la
  izquierda.

El shell participa (`overlayShellExitTransition` / `overlayShellPopEnterTransition`)
para que los dos paneles encajen frame a frame y la animación no dependa del z-order.

## ⚠️ Limitación conocida: la animación de "volver" se ve seca (sobre todo en debug)

**Síntoma:** al abrir un detalle, la entrada desliza bien; al volver, el barrido se ve
brusco/instantáneo (peor en build **debug**).

**Causa raíz (diagnosticada frame a frame en dispositivo):** es una **asimetría de
recomposición**. Al volver, la pantalla que reaparece (el shell) **se reconstruye
desde cero** — Compose descarta de composición los destinos que no están visibles.
Esa reconstrucción **bloquea el hilo principal ~200 ms**, y durante un bloqueo del
hilo **no se dibuja ningún frame**, así que *cualquier* transición (slide, fade, la
default) se ve seca: a 280 ms el bloqueo se come casi todo el barrido; a ~1200 ms el
bloqueo es solo el ~17% y el barrido se ve entero. La **entrada** no sufre porque el
shell que se va ya está construido. Confirmado también con **Perfil → Inicio**
(pantalla trivial): también salta → el costo es del **shell**, no del contenido del
tab.

**Mitigaciones aplicadas:**
- Se reemplazó el `NavHost` anidado de tabs por el conmutador ligero → bajó el
  bloqueo de ~450 ms a ~200 ms.
- **No** se infla la duración para enmascararlo: un back de 500 ms+ se sentiría lento
  en release (donde el bloqueo es mucho menor). La duración queda natural (280 ms).

**Qué lo arreglaría del todo (no hecho — decisión consciente):** que el shell **no se
reconstruya** al abrir un detalle, es decir, renderizar los detalles como una capa
encima de un shell siempre compuesto (layout tipo overlay). Se descartó por ahora
porque es una reescritura mayor que **toca la navegación del flujo de terapia** (ya
validado en placa), y el síntoma es cosmético y mucho menor en **release**. No cambiar
la estructura por este motivo sin re-verificar el flujo de terapia.
