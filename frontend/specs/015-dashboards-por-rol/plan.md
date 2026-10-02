# Implementation Plan: Dashboards por rol — Frontend

**Branch**: `015-dashboards-por-rol` | **Date**: 2026-09-25 | **Spec**: `specs/015-dashboards-por-rol/spec.md`

## Summary
`usePanel` nuevo (contadores api-directos por rol) + `irAPanel()` en `useAuth` + secciones en las 3 vistas + ocultado mutuo con las 8 secciones existentes.

## Technical Context
**Language/Version**: JS plano, Vue 3, sin build.
**Testing**: `node --check` + prueba manual (abrir/cerrar, contadores).

## Project Structure
```text
specs/015-dashboards-por-rol/
├── plan.md
└── tasks.md
```
```text
frontend/js/composables/usePanel.js   # nuevo
frontend/js/composables/useAuth.js    # irAPanel() (reemplaza panelUrl)
frontend/js/composables/*.js          # ocultar showPanel al abrir otra sección
frontend/js/pages/*.js                # exponer usePanel
frontend/views/*.html                 # secciones dashboard
```

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Ninguna | — | — |
