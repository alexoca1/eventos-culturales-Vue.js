# Implementation Plan: Navegación y sesión unificada — Frontend

**Branch**: `013-navegacion-sesion-unificada` | **Date**: 2026-09-25 | **Spec**: `specs/013-navegacion-sesion-unificada/spec.md`

## Summary
Sesión del header en `useAuth` (nombre/panel/menú/carga) + headers idénticos en las 3 vistas + `.acciones` separada + CSS mínimo (header-bar, dropdown, toolbar, panel-tag).

## Technical Context
**Language/Version**: JS plano, Vue 3, sin build.
**Testing**: `node --check` + prueba manual (menú, logout, título).

## Project Structure
```text
specs/013-navegacion-sesion-unificada/
├── plan.md
└── tasks.md
```
```text
frontend/js/composables/useAuth.js   # sesionNombre/panelUrl/menuAbierto/cargarSesion/irAPerfil + redirect en logout
frontend/js/pages/*.js               # exponer useAuth (admin/organizador) + cargarSesion en onMounted
frontend/views/*.html + index.html   # headers, .acciones, subtítulo organizador
frontend/css/eventos.css/index.css   # estilos nuevos (mismos vars, sin valores nuevos)
```

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Ninguna | — | — |
