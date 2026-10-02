# Implementation Plan: Próximos por defecto + anteriores en gestión — Frontend

**Branch**: `014-eventos-futuros-ordenados` | **Date**: 2026-09-25 | **Spec**: `specs/014-eventos-futuros-ordenados/spec.md`

## Summary
`proximos()` en api + estado/flags en `useEventosInvitado` (+ `volverBuscar` async) + sección en invitado + `computed` filtrado + toggle en gestión admin.

## Technical Context
**Language/Version**: JS plano, Vue 3 (`computed` del ESM ya vendorizado), sin build.
**Testing**: `node --check` + prueba manual.

## Project Structure
```text
specs/014-eventos-futuros-ordenados/
├── plan.md
└── tasks.md
```
```text
frontend/js/api/eventos.js                  # proximos()
frontend/js/composables/useEventosInvitado.js # proximosEventos/showProximos/cargarProximos
frontend/js/composables/useFavoritos.js       # volverBuscar async + oculta próximos
frontend/js/composables/useGestionEventos.js  # mostrarAnteriores + eventosGestion
frontend/js/pages/invitado.js                 # cargarProximos en onMounted
frontend/views/usuarioEstandar.html           # sección Próximos
frontend/views/administrador.html             # toggle + v-for eventosGestion
```

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Ninguna | — | — |
