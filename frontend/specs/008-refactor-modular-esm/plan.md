# Implementation Plan: Refactor a ES Modules + Composables — Frontend

**Branch**: `008-refactor-modular-esm` | **Date**: 2026-09-23 | **Spec**: `specs/008-refactor-modular-esm/spec.md`

## Summary
Refactor estructural (no de comportamiento) del único `eventos.js` monolítico a un conjunto de módulos ES nativos organizados en 4 capas: `api/` (Repository + Facade), `store.js` (Observer/Singleton reactivo), `ui/` (helpers puros), `composables/` (Composition API, un composable por dominio funcional), y `pages/` (un entrypoint por página, solo importando lo que usa).

## Technical Context
**Language/Version**: JS plano con `import`/`export` nativos (ES Modules), sin bundler.
**Dependencies**: ninguna nueva — se sustituye `vue.global.js` por `vue.esm-browser.js` (misma versión 3.5.42, misma fuente, misma carpeta `lib/`).
**Testing**: `node --check` por fichero nuevo + checklist de regresión manual de las 8 features (no hay test runner de JS en este proyecto).
**Constraints**: comportamiento observable idéntico al actual; debe seguir sirviéndose por XAMPP/Apache tal cual (sin build).

## Constitution Check
- [x] Sin librerías nuevas.
- [x] Sin reescritura de producto — es refactor puro, mismo comportamiento.
- [x] Reduce duplicación real (DRY: el bloque fetch/401/403 repetido 15 veces pasa a vivir en un solo sitio).

## Module Map (verificado línea por línea contra `frontend/js/eventos.js` actual)

| Módulo nuevo | Contenido (origen en `eventos.js` actual) |
|---|---|
| `api/http.js` | Nuevo: `authFetch(url, opts)` — extrae el patrón try/catch+401/403+parseo de error repetido en `validateAdmin, register, searchEvent, submitForm, deleteOne, cargarPendientes, aprobar, confirmarRechazo, cargarUsuarios, crearAdmin, editarRolesUsuario, toggleActivo, loadMisEventos, verFavoritos, toggleFavorito` |
| `api/auth.js` | `validateAdmin` (fetch), `register` (fetch), `logout` (fetch), `cargarUsuarios` (fetch), `crearAdmin` (fetch), `editarRolesUsuario`/`toggleActivo` (fetch de `PUT /auth/usuarios/{id}`) |
| `api/eventos.js` | `apiToView`, `searchEvent` (fetch), `loadAllEvents` (fetch), `submitForm` (fetch), `deleteOne` (fetch), `cargarPendientes`/`aprobar`/`confirmarRechazo` (fetch), `loadMisEventos` (fetch), `verFavoritos`/`toggleFavorito`/`loadFavoritos` (fetch) |
| `store.js` | Nuevo: `reactive({ token, roles, miEmail })`, derivados `esAdmin`/`esOrganizador`; sustituye las lecturas directas de `sessionStorage.getItem("token")` repartidas en `mounted()`, `authHeader`, `tieneSesion`, `toggleFavorito`, etc. |
| `ui/format.js` | `formatDate`, `formatRango`, `formatHorario`, `textoEstado`, `tipoSolicitud`, `textoRol` |
| `ui/mapa.js` | `processMap` |
| `ui/imagenes.js` | `handleFileUpload`, `webify` |
| `composables/useAuth.js` | Envuelve `api/auth.js`: expone `login`, `register`, `logout`, `tieneSesion` con estado reactivo (`username`, `password`, `showRegister`, `regEmail`, `regPassword`, `regNombre`, `regApellidos`, `regError`) |
| `composables/useEventoForm.js` | `addEvent, cancelForm, previsualizar, volverAEditar, confirmarEnvio, cumple48h, submitForm, editEvento` + todo el estado `input*`, `file`, `fileHd`, `showForm`, `showPreview`, `previewEv`, `editingId` |
| `composables/useEventosInvitado.js` | `searchEvent` + `showEvent`, `showNoEvent`, `showCover`, `dayEvents`, `searchDate`, `searchCategoria`, `date` |
| `composables/useGestionEventos.js` | `loadAllEvents, manageEvents, deleteOne` + `showManage`, `showEventSaved`, `events` |
| `composables/useMisEventos.js` | `loadMisEventos, pedirEliminarOrg, editEventoOrg` + `misEventos` |
| `composables/useModeracion.js` | `cargarPendientes, aprobar, pedirRechazo, confirmarRechazo` + `showPendientes`, `pendientes`, `rechazoId`, `rechazoMotivo` |
| `composables/useUsuarios.js` | `cargarUsuarios, crearAdmin, empezarEditarRoles, cancelarEditarRoles, editarRolesUsuario, esUnoMismo, toggleActivo` + `showUsuarios`, `usuarios`, `nuevoAdmin*`, `crearAdminError`, `editandoUsuarioId`, `editRoles`, `editError` |
| `composables/useFavoritos.js` | `verFavoritos, volverBuscar, esFavorito, toggleFavorito, loadFavoritos` + `showFavoritos`, `favoritosIds`, `favoritosEventos` |
| `composables/useLightbox.js` | `openLightbox, closeLightbox` + `lightboxSrc` (+ el listener global de `Escape`, hoy en `mounted()`) |
| `pages/login.js` | `mount("#eventos")` de `index.html`, usa solo `useAuth` |
| `pages/invitado.js` | `mount("#eventos")` de `usuarioEstandar.html`, usa `useEventosInvitado`, `useFavoritos`, `useLightbox`, `store` |
| `pages/admin.js` | `mount("#eventos")` de `administrador.html`, usa `useEventoForm`, `useGestionEventos`, `useModeracion`, `useUsuarios`, `useLightbox` |
| `pages/organizador.js` | `mount("#eventos")` de `organizador.html`, usa `useEventoForm`, `useMisEventos`, `useLightbox` |

## Project Structure

### Documentation (this feature)
```text
specs/008-refactor-modular-esm/
├── plan.md
└── tasks.md
```

### Source Code
```text
frontend/lib/vue.esm-browser.js      # nuevo (misma versión 3.5.42)
frontend/js/api/http.js               # nuevo
frontend/js/api/auth.js                # nuevo
frontend/js/api/eventos.js              # nuevo
frontend/js/store.js                     # nuevo
frontend/js/ui/format.js                  # nuevo
frontend/js/ui/mapa.js                     # nuevo
frontend/js/ui/imagenes.js                  # nuevo
frontend/js/composables/*.js                 # 9 ficheros nuevos (ver mapa)
frontend/js/pages/*.js                        # 4 ficheros nuevos (ver mapa)
frontend/js/eventos.js                         # ELIMINADO al final (Fase 6)
frontend/index.html, views/*.html               # script tags actualizados
```

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| 9 composables en vez de 1 fichero | Cada uno mapea 1:1 a un dominio funcional ya identificable en el código actual (favoritos, moderación, usuarios...) — fusionarlos reproduciría el mismo God Object que se está eliminando | — |