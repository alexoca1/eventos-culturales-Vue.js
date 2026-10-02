# Implementation Plan: Editar perfil — Frontend

**Branch**: `012-editar-perfil` | **Date**: 2026-09-24 | **Spec**: `specs/012-editar-perfil/spec.md`

## Summary
`usePerfil` nuevo + secciones en las 3 páginas + teléfono en altas + `creadoPor` visible.

## Technical Context
**Language/Version**: JS plano, Vue 3, sin build.
**Testing**: `node --check` + prueba manual.

## Project Structure
```text
specs/012-editar-perfil/
├── plan.md
└── tasks.md
```
```text
frontend/js/api/auth.js                # actualizarPerfil() + telefono en register/crearAdmin
frontend/js/composables/usePerfil.js    # nuevo
frontend/js/composables/useUsuarios.js  # nuevoAdminTelefono + org en editarRolesUsuario
frontend/js/pages/*.js                  # exponer usePerfil (3 páginas)
frontend/views/*.html + index.html      # secciones, botones, inputs de teléfono
frontend/js/api/eventos.js              # creadoPor prefiere organización
```

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Ninguna | — | — |
