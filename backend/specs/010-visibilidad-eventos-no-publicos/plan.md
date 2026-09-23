# Implementation Plan: Visibilidad de eventos no públicos en GET /eventos/{id}

**Branch**: `010-visibilidad-eventos-no-publicos` | **Date**: 2026-09-23 | **Spec**: `specs/010-visibilidad-eventos-no-publicos/spec.md`

## Summary
`findById` en `EventoController` reutiliza el mismo patrón de `isAdmin(authentication)` + comprobación de ownership que ya usan `updateEvento`/`deleteEvento`, devolviendo 404 en vez del evento cuando no aplica ninguno de los 2 casos permitidos.

## Technical Context
**Language/Version**: Java 21, sin dependencias nuevas.
**Constraints**: no cambiar el comportamiento de eventos `APROBADO` (deben seguir siendo públicos sin autenticación); no romper la suite existente.

## Constitution Check
- [x] III. Sin over-engineering: reutiliza los helpers `isAdmin()` ya existentes en el propio controller, sin servicio nuevo.
- [x] IV. Tests obligatorios: los 4 escenarios del spec.

## Project Structure
```text
backend/src/main/java/.../controller/EventoController.java          # findById() con el filtro nuevo
backend/src/test/java/.../controller/EventoControllerAuthTest.java  # tests nuevos
docs/api-contract.md                                                  # documentar el 404 para no-APROBADO
```

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Ninguna | — | — |