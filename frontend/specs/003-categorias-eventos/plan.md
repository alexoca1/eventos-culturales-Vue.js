# Implementation Plan: Filtro por categoría — Frontend

**Branch**: `003-categorias-eventos` | **Date**: 2026-09-18 | **Spec**: `specs/003-categorias-eventos/spec.md`

## Summary
Un `<select>` de categoría reutilizado en el formulario de admin (para asignar) y en la vista de invitado (para filtrar), con las 7 opciones hardcodeadas en el HTML (lista fija y pequeña — no justifica traerlas dinámicamente de un endpoint de metadatos, consistente con YAGNI).

## Technical Context
**Language/Version**: JS plano, Vue 3 global, sin build.
**Dependencies**: ninguna nueva.
**Testing**: `node --check js/eventos.js` + prueba manual.

## Project Structure

### Documentation (this feature)
```text
specs/003-categorias-eventos/
├── plan.md
└── tasks.md
```

### Source Code
```text
frontend/js/eventos.js               # apiToView(), searchEvent(), submitForm(), editEvento()
frontend/views/administrador.html    # <select> categoria en el formulario + filtro en el buscador
frontend/views/usuarioEstandar.html  # <select> categoria en el buscador
```

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Ninguna | — | — |