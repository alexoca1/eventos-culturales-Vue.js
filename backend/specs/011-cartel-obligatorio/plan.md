# Implementation Plan: Cartel obligatorio al crear — Backend

**Branch**: `011-cartel-obligatorio` | **Date**: 2026-09-24 | **Spec**: `specs/011-cartel-obligatorio/spec.md`

## Summary
Comprobación de `file` nulo/vacío al inicio de `createEvento` (antes que el resto de validaciones); `updateEvento` intacto.

## Technical Context
**Language/Version**: Java 21, sin dependencias nuevas.
**Testing**: `./mvnw test` (reutiliza `imagenJpeg()` en los tests de 201).

## Constitution Check
- [x] IV. Tests obligatorios: `post_admin_sinCartel_400` + adaptación de los 201 sin fichero.

## Project Structure
```text
backend/src/main/java/.../controller/EventoController.java   # check en createEvento
backend/src/test/java/.../controller/EventoControllerAuthTest.java  # adapta 201 + nuevo 400
```

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Ninguna | — | — |
