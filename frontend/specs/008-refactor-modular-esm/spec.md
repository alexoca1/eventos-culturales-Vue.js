# Feature Specification: Refactor a ES Modules + Composables — Frontend

**Feature Branch**: `008-refactor-modular-esm`
**Created**: 2026-09-23
**Status**: Draft
**Input**: eventos.js (868 líneas) es un único objeto Options API compartido por las 4 páginas, con ~50 campos de estado y ~45 métodos sin separación de responsabilidades, y con el manejo de fetch/401/error duplicado 15 veces. Refactorizar a ES Modules nativos + Composition API (composables), sin bundler, preservando el comportamiento exacto de las 8 features ya implementadas.

## User Scenarios & Testing

### User Story 1 - Cada página carga solo lo que necesita (Priority: P1)
Cada una de las 4 páginas (`index.html`, `usuarioEstandar.html`, `administrador.html`, `organizador.html`) tiene su propio entrypoint de módulo, que importa únicamente los composables que usa esa página.

**Independent Test**: inspeccionar el Network tab al cargar cada página — no debería instanciarse estado ni lógica de las otras 3 páginas.

**Acceptance Scenarios**:
1. **Given** se abre `index.html`, **When** se inspecciona `pages/login.js`, **Then** solo importa `useAuth` — nada de eventos, favoritos, moderación o usuarios.
2. **Given** las 4 páginas, **When** se comparan sus entrypoints, **Then** ninguno importa un composable que su página no usa según el mapa de módulos.

---

### User Story 2 - Sin duplicación del manejo de fetch/errores (Priority: P1)
Todas las llamadas a la API pasan por `api/http.js`, que centraliza: error de red, 401/403 (logout + redirect), y parseo de error del backend.

**Acceptance Scenarios**:
1. **Given** cualquier llamada a la API, **When** el backend no responde (red caída), **Then** se comporta igual que hoy (alerta "No se pudo conectar con el servidor").
2. **Given** cualquier llamada autenticada, **When** el backend devuelve 401/403, **Then** se limpia el token y redirige a login, igual que hoy en los 15 sitios donde ocurre actualmente.
3. **Given** el código de `api/*.js` y `composables/*.js`, **When** se audita, **Then** ningún módulo repite el bloque try/catch+401/403 — todos delegan en `http.js`.

---

### User Story 3 - Comportamiento idéntico a antes del refactor (Priority: P1)
Este refactor NO cambia ninguna funcionalidad — es un cambio de estructura interna, no de producto.

**Independent Test**: repetir manualmente el flujo end-to-end de cada una de las 8 features de frontend ya implementadas (ver checklist de regresión en `tasks.md`) y confirmar que el resultado observable es idéntico al de antes del refactor.

**Acceptance Scenarios**:
1. **Given** cualquiera de las 8 features ya implementadas, **When** se prueba manualmente tras el refactor, **Then** el comportamiento (UI, llamadas a la API, mensajes de error) es indistinguible del que había antes.

---

### Edge Cases
- `apiToView()` sigue siendo la única función que traduce la forma de la API (español) a la forma de la vista (inglés) — vive en `api/eventos.js`, no se duplica en ningún composable.
- El estado de sesión (`token`, roles derivados) deja de leerse directamente de `sessionStorage` en cada método — pasa a vivir en `store.js` como único punto de verdad reactivo, y `sessionStorage` se usa solo como persistencia (leída una vez al iniciar cada página).
- Ningún composable importa directamente `fetch` — todos pasan por `api/*.js`, que a su vez pasa por `http.js`.

## Requirements

### Functional Requirements
- **FR-001**: El sistema MUST sustituir el único `eventos.js` monolítico por los módulos listados en el mapa de `plan.md`, sin dejar código muerto (ningún método del `eventos.js` original MUST quedar sin una ubicación nueva).
- **FR-002**: `api/http.js` MUST ser el único lugar que haga `fetch` directamente para llamadas autenticadas, centralizando red/401/403/error.
- **FR-003**: Cada página MUST tener su propio entrypoint (`pages/*.js`) que solo importe los composables que usa.
- **FR-004**: El comportamiento observable (UI, mensajes, llamadas a la API) MUST ser idéntico al de antes del refactor, para las 8 features de frontend ya implementadas.
- **FR-005**: `vue.global.js` (UMD) se sustituye por `vue.esm-browser.js` (misma versión 3.5.42), vendorizado en local igual que hoy.

## Success Criteria

### Measurable Outcomes
- **SC-001**: `eventos.js` (el fichero monolítico) queda eliminado del repositorio, sustituido por los módulos del mapa.
- **SC-002**: Ningún fichero nuevo repite el bloque de manejo de fetch/401/403 (verificable por inspección — no hay una métrica automática, dado que no hay test runner de JS en este proyecto).
- **SC-003**: Las 8 features de frontend pasan el checklist de regresión manual de `tasks.md` sin ninguna diferencia de comportamiento.
- **SC-004**: `node --check` sobre cada fichero `.js` nuevo, sin errores de sintaxis.

## Assumptions
- No se introduce Vue Router ni Pinia en esta feature — eso es alcance de la futura migración a Vite (Opción 3, ya descartada por ahora). Aquí solo se moderniza la organización del código manteniendo la navegación multi-página actual.
- No hay suite de tests automatizados de frontend (solo `node --check`), así que la regresión se verifica manualmente con el checklist de `tasks.md` — es el mismo criterio que ya se ha usado en todas las features de frontend anteriores.
- Dos pestañas duplicadas (Ctrl+D) comparten el mismo sessionStorage por comportamiento nativo del navegador — no es un bug del código. Abrir una pestaña nueva sí empieza sin sesión. Este comportamiento es inevitable y se acepta.