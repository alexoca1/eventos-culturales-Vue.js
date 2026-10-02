# Tasks: Refactor a ES Modules + Composables — Frontend

**Input**: `specs/008-refactor-modular-esm/spec.md` + `plan.md`
**Regla de oro**: comportamiento idéntico al actual en cada fase — si algo cambia de comportamiento, es un bug del refactor, no una mejora "de paso".

---

## Phase 1: Vendorizar Vue ESM + capa base (sin tocar páginas todavía)
- [x] F075 Descargar `vue.esm-browser.js` v3.5.42 (misma versión que `vue.global.js` actual) a `frontend/lib/`
- [x] F076 Crear `api/http.js`: `authFetch(url, opts)` que centraliza try/catch de red, 401/403 (limpia token + redirige), y devuelve `{ ok, status, data }` o lanza según convenga; `authHeader()` movido aquí
- [x] F077 Crear `store.js`: `reactive({ token, roles, miEmail })` inicializado desde `sessionStorage` al importarse, con `setSession()`/`clearSession()` que mantienen `sessionStorage` sincronizado

**Checkpoint**: nada de esto se usa todavía en ninguna página — solo existen los ficheros, verificables con `node --check`.

---

## Phase 2: Capa API (Repository)
- [x] F078 Crear `api/auth.js`: `login, register, logout, perfil, listarUsuarios, crearAdmin, actualizarUsuario` — todos usando `authFetch`
- [x] F079 Crear `api/eventos.js`: `apiToView` (copiado tal cual, sin cambios de lógica) + `buscar, listarTodos, crear, editar, eliminar, mios, pendientes, aprobar, rechazar, listarFavoritos, marcarFavorito, desmarcarFavorito` — todos usando `authFetch`

**Checkpoint**: la capa API está completa y aislada; comparar cada función contra el `fetch` original correspondiente en `eventos.js` para confirmar que la URL, el método HTTP y el body son idénticos.

---

## Phase 3: Capa UI (helpers puros, sin estado)
- [x] F080 [P] Crear `ui/format.js`: `formatDate, formatRango, formatHorario, textoEstado, tipoSolicitud, textoRol` (copiados tal cual)
- [x] F081 [P] Crear `ui/mapa.js`: `processMap` (copiado tal cual)
- [x] F082 [P] Crear `ui/imagenes.js`: `handleFileUpload, webify` (copiados tal cual)

**Checkpoint**: 3 módulos sin estado, fácilmente verificables por lectura (son funciones puras copiadas literalmente).

---

## Phase 4: Composables (Composition API)
- [x] F083 Crear `composables/useAuth.js`
- [x] F084 Crear `composables/useEventoForm.js`
- [x] F085 Crear `composables/useEventosInvitado.js`
- [x] F086 Crear `composables/useGestionEventos.js`
- [x] F087 Crear `composables/useMisEventos.js`
- [x] F088 Crear `composables/useModeracion.js`
- [x] F089 Crear `composables/useUsuarios.js`
- [x] F090 Crear `composables/useFavoritos.js`
- [x] F091 Crear `composables/useLightbox.js`

**Checkpoint**: cada composable expone exactamente el mismo estado reactivo y las mismas funciones (incluyendo su firma) que tenía en el `eventos.js` original, según el Module Map de `plan.md` — sin renombrar nada todavía, para minimizar el riesgo del siguiente paso (las plantillas HTML referencian estos nombres tal cual).

---

## Phase 5: Entrypoints por página + actualización de HTML
- [x] F092 Crear `pages/login.js` (usa `useAuth`); actualizar `index.html`: `<script src="lib/vue.global.js">` → eliminado, `<script src="js/eventos.js">` → `<script type="module" src="js/pages/login.js"></script>`
- [x] F093 Crear `pages/invitado.js`; actualizar `usuarioEstandar.html` igual que F092
- [x] F094 Crear `pages/admin.js`; actualizar `administrador.html` igual que F092
- [x] F095 Crear `pages/organizador.js`; actualizar `organizador.html` igual que F092

**Checkpoint**: las 4 páginas cargan y montan Vue vía módulos, ya sin `eventos.js` ni `vue.global.js`.

---

## Phase 6: Limpieza + regresión completa
- [ ] F096 Eliminar `frontend/js/eventos.js` y `frontend/lib/vue.global.js` (ya sin referencias)
- [ ] F097 `node --check` sobre cada fichero `.js` nuevo
- [ ] F098 **Checklist de regresión manual — las 8 features de frontend, sin excepción**:
  - [ ] Login admin/organizador/usuario redirige correctamente según rol
  - [ ] Registro + login automático funciona
  - [ ] Invitado: búsqueda por fecha, por categoría, y combinada; lightbox del cartel HD
  - [ ] Admin: crear/editar/eliminar evento (con y sin cartel, con horario, con rango de días, con categoría)
  - [ ] Admin: cola de moderación (aprobar/rechazar con motivo)
  - [ ] Admin: gestión de usuarios (ascender a organizador, activar/desactivar, crear admin, auto-bloqueo bloqueado)
  - [ ] Organizador: crear con previsualización + confirmación, editar, solicitar eliminación, "Mis eventos" con badges de estado
  - [ ] Usuario estándar: marcar/desmarcar favorito, "Mis favoritos", cerrar sesión

## Dependencies & Execution Order
Fase 1 → 2 → 3 → 4 → 5 → 6, estrictamente en ese orden (cada fase depende de que la anterior exista). No se toca ninguna página hasta la Fase 5 — todo lo previo es aditivo y de riesgo bajo.

## Notas
- Un prompt por fase (6 prompts).
- Si la Fase 6 revela una regresión, no se hace commit hasta corregirla — el comportamiento idéntico es un requisito duro (FR-004), no una aspiración.