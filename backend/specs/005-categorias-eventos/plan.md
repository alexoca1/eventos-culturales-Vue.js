# Implementation Plan: Categorías de Eventos — Eventos Culturales Puertollano

**Branch**: `005-categorias-eventos` | **Date**: 2026-09-18 | **Spec**: `specs/005-categorias-eventos/spec.md`

## Summary
Enum `CategoriaEvento` con 7 valores fijos, campo nullable en `Evento`, y extensión del `GET /eventos` existente (`fecha` opcional) para aceptar también `categoria` opcional, combinable.

## Technical Context
**Language/Version**: Java 21, sin dependencias nuevas.
**Storage**: nueva columna `categoria` (String, vía `@Enumerated(EnumType.STRING)` — nunca `ORDINAL`, para no romper datos si se reordena el enum en el futuro).
**Testing**: JUnit 5 + MockMvc, mismo estilo que features previas.

## Constitution Check
- [x] II. Stdlib-first: enum de Java puro, sin librería de validación adicional (Spring ya devuelve 400 al fallar la deserialización de un enum inválido).
- [x] III. Sin over-engineering: nada de tabla `categoria` aparte ni relación — un enum basta para una lista fija y pequeña.
- [x] IV. Tests obligatorios: cubiertos en tasks.md.

## Project Structure

### Documentation (this feature)
```text
specs/005-categorias-eventos/
├── plan.md
└── tasks.md
```

### Source Code
```text
backend/src/main/java/.../entities/CategoriaEvento.java      # enum nuevo
backend/src/main/java/.../entities/Evento.java                # + categoria (@Enumerated(EnumType.STRING))
backend/src/main/java/.../dto/EventoDTO.java                  # + categoria
backend/src/main/java/.../repositories/EventoRepository.java  # + findByCategoriaOrderByIdAsc, findByFechaAndCategoriaOrderByIdAsc
backend/src/main/java/.../controller/EventoController.java    # GET /eventos: + @RequestParam categoria, branching
backend/src/main/java/.../config/DataInitializer.java         # asignar categoría a los 5 eventos semilla
backend/src/test/java/.../controller/EventoControllerPublicTest.java  # tests de filtro
docs/api-contract.md                                           # documentar categoria + el nuevo query param
```

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Ninguna | — | — |