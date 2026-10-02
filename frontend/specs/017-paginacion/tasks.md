# Tasks: Paginación en cliente — Frontend

**Input**: `specs/017-paginacion/spec.md` + `plan.md`

---

## Phase 1: Capa API (P1)
- [x] F168 En `api/eventos.js`: `buscar({ fecha, etiquetas, futuros, page })` incluye `?page=N` en la URL cuando se pasa; `mios(page)` igual. Ambas funciones devuelven el objeto `Page<>` completo (con `content`, `totalPages`, `last`, etc.) en vez de solo el array.

**Checkpoint**: `api/eventos.js` pasa `?page=` y devuelve el objeto de página completo.

---

## Phase 2: Composables (P1)
- [x] F169 En `useEventosInvitado`: añadir `paginaActual` (ref, default 0), `paginaMeta` (ref, default `{ totalPages:1, totalElements:0, first:true, last:true }`); actualizar `searchEvent()` y `cargarProximos()` para pasar `paginaActual.value` al fetch y guardar los metadatos devueltos en `paginaMeta`; añadir `paginaSiguiente()` (incrementa y relanza) y `paginaAnterior()` (decrementa y relanza); reiniciar `paginaActual` a 0 cuando cambie fecha o etiqueta antes de llamar a `searchEvent()`; exponer los 4 nuevos en el return del composable
- [x] F170 En `useMisEventos`: misma lógica con `misPaginaActual`, `misPaginaMeta`, `misPaginaSiguiente()`, `misPaginaAnterior()` aplicada a `loadMisEventos()`

**Checkpoint**: los composables manejan la página y los metadatos correctamente.

---

## Phase 3: Plantillas (P1)
- [x] F171 En `usuarioEstandar.html`: añadir bloque de paginación (solo visible cuando `paginaMeta.totalPages > 1`) después del listado de eventos y del listado de "Mis favoritos", con botón "Anterior" (`:disabled="paginaMeta.first"`, `@click="paginaAnterior()"`) + indicador `"Página {{paginaActual + 1}} de {{paginaMeta.totalPages}} ({{paginaMeta.totalElements}} eventos)"` + botón "Siguiente" (`:disabled="paginaMeta.last"`, `@click="paginaSiguiente()"`)
- [x] F172 En `organizador.html`: mismo bloque de paginación en la sección "Mis eventos" usando `misPaginaMeta`/`misPaginaSiguiente`/`misPaginaAnterior`

**Checkpoint**: los controles de paginación aparecen, funcionan y se ocultan cuando no hay más de una página.

---

## Phase 4: Polish
- [x] F173 `node --check` sobre `useEventosInvitado.js`, `useMisEventos.js` y `api/eventos.js`
- [x] F174 Prueba manual: con backend corriendo, confirmar los 4 escenarios del spec (primera carga, siguiente, última página, sin botones si ≤10 eventos) y que cambiar el filtro reinicia a página 1

## Dependencies & Execution Order
F168 → F169/F170 → F171/F172 → F173/F174. Requiere `backend/specs/014-paginacion` implementado y corriendo.