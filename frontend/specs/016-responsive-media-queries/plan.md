# Implementation Plan: Responsive móvil/tablet — Frontend

**Branch**: `016-responsive-media-queries` | **Date**: 2026-09-25 | **Spec**: `specs/016-responsive-media-queries/spec.md`

## Summary
Extender el media 768px de `eventos.css` a los componentes nuevos (header, cards, rows, toolbar, dropdown, checkboxes) + bloque 480px; `index.css` sin cambios (ya cubierto).

## Technical Context
**Language/Version**: CSS puro, sin build.
**Testing**: balance de llaves + auditoría selector↔HTML (no hay navegador automatizado).

## Project Structure
```text
specs/016-responsive-media-queries/
├── plan.md
└── tasks.md
```
```text
frontend/css/eventos.css  # media 768px extendido + media 480px nuevo
```

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Ninguna | — | — |
