# Implementation Plan: Eventos futuros ordenados — Backend

**Branch**: `013-eventos-futuros-ordenados` | **Date**: 2026-09-25 | **Spec**: `specs/013-eventos-futuros-ordenados/spec.md`

## Summary
4 queries JPQL (`findFuturos*`, con/sin etiquetas y con/sin estado) + ramas en `findAll` + 400 para el combo con `fecha`.

## Technical Context
**Language/Version**: Java 21, sin dependencias nuevas.

## Constitution Check
- [x] IV. Tests obligatorios (4 nuevos).

## Project Structure
```text
backend/.../repositories/EventoRepository.java  # 4 queries
backend/.../controller/EventoController.java    # param futuros + ramas
```

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Ninguna | — | — |
