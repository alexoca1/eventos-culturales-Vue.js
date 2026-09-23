# Implementation Plan: Pantalla de Gestión de Usuarios — Frontend

**Branch**: `005-gestion-admins` | **Date**: 2026-09-18 | **Spec**: `specs/005-gestion-admins/spec.md`

## Summary
Una sección nueva dentro de `administrador.html` (no una vista aparte — es una pantalla de administración más, igual que "Gestionar eventos" ya existente) con lista de usuarios, edición inline de roles/estado, y un formulario de creación de admin reutilizando el endpoint ya existente.

## Technical Context
**Language/Version**: JS plano, Vue 3 global, sin build.
**Dependencies**: ninguna nueva.
**Testing**: `node --check js/eventos.js` + prueba manual de los 4 flujos.

## Constitution Check
- [x] Reutiliza el patrón visual ya existente de "Gestionar eventos" (tarjetas/lista + acciones), no introduce un componente de tabla nuevo.
- [x] Sin librerías de tablas/grid nuevas.

## Project Structure

### Documentation (this feature)
```text
specs/005-gestion-admins/
├── plan.md
└── tasks.md
```

### Source Code
```text
frontend/views/administrador.html    # + sección "Gestión de usuarios"
frontend/js/eventos.js               # loadUsuarios(), editarRolesUsuario(), toggleActivo(), crearAdmin()
frontend/css/eventos.css             # badges de rol/estado (reutilizando clases existentes donde se pueda)
```

**Structure Decision**: todo son ediciones sobre `administrador.html`/`eventos.js` ya existentes; no se crea ninguna vista nueva.

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Ninguna | — | — |