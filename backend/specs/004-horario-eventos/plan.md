# Implementation Plan: Horario de Eventos (inicio/fin) — Eventos Culturales Puertollano

**Branch**: `004-horario-eventos` | **Date**: 2026-09-18 | **Spec**: `specs/004-horario-eventos/spec.md`

## Summary
Añadir `horaInicio`/`horaFin` opcionales a `Evento`, validar que se informen juntas, y exponer un getter derivado `fechaFin` (`@Transient`, calculado) para que el cliente no reimplemente la regla de cruce de medianoche.

## Technical Context
**Language/Version**: Java 21, sin dependencias nuevas (`java.time.LocalTime` es stdlib).
**Storage**: `ALTER` implícito vía `ddl-auto=update` (Hibernate añade 2 columnas nullable, no rompe datos existentes).
**Testing**: JUnit 5 + MockMvc, mismo estilo que features previas.
**Constraints**: no romper la suite `001`+`002`+`003`; el controller sigue devolviendo la entidad `Evento` directamente (no se introduce un DTO de respuesta nuevo, coherente con el estilo actual del proyecto).

## Constitution Check
- [x] I. Constructor injection: sin cambios.
- [x] II. Stdlib-first: `LocalTime`/`LocalDate` de `java.time`, sin librerías nuevas.
- [x] III. Sin over-engineering: getter derivado en la propia entidad (`@Transient`), no un DTO de respuesta nuevo ni un servicio de cálculo aparte.
- [x] IV. Tests obligatorios: cubiertos en tasks.md.
- [x] V/VI/VII: sin cambios.

## Project Structure

### Documentation (this feature)
```text
specs/004-horario-eventos/
├── plan.md
└── tasks.md
```

### Source Code
```text
backend/src/main/java/.../entities/Evento.java              # + horaInicio, horaFin, + getFechaFin() @Transient
backend/src/main/java/.../dto/EventoDTO.java                 # + horaInicio, horaFin (record de entrada)
backend/src/main/java/.../controller/EventoController.java   # validación FR-002 en create/update
backend/src/test/java/.../controller/EventoControllerAuthTest.java  # tests nuevos
docs/api-contract.md                                          # documentar horaInicio/horaFin/fechaFin
```

**Structure Decision**: todo son ediciones sobre ficheros ya existentes; ningún paquete nuevo.

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Ninguna | — | — |