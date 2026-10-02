# Implementation Plan: Paginación en cliente — Frontend

**Branch**: `017-paginacion` | **Date**: 2026-09-25 | **Spec**: `specs/017-paginacion/spec.md`

## Summary
Añadir `paginaActual` y `paginaMeta` a `useEventosInvitado` y `useMisEventos`, incluir `?page=` en cada fetch, exponer `paginaSiguiente()`/`paginaAnterior()`, y añadir el control de paginación en las plantillas. Sin librerías nuevas.

## Technical Context
**Language/Version**: JS plano, Vue 3 ESM, sin build.
**Dependencies**: ninguna nueva.
**Testing**: `node --check` + prueba manual con backend corriendo (152 tests backend ya cubren la lógica de paginación del servidor).

## Project Structure
```text
frontend/js/composables/useEventosInvitado.js  # + paginaActual, paginaMeta, paginaSiguiente, paginaAnterior
frontend/js/composables/useMisEventos.js        # + misma lógica
frontend/js/api/eventos.js                       # buscar() y mios() pasan page como parámetro
frontend/views/usuarioEstandar.html              # + bloque de paginación
frontend/views/organizador.html                   # + bloque de paginación en "Mis eventos"
```

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| Ninguna | — | — |