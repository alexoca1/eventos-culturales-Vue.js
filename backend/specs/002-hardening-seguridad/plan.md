# Implementation Plan: Hardening de Seguridad — Eventos Culturales Puertollano

**Branch**: `002-hardening-seguridad` | **Date**: 2026-09-18 | **Spec**: `specs/002-hardening-seguridad/spec.md`

**Input**: Feature specification + revisión de código manual (3 hallazgos: JWT_SECRET fallback inseguro, GlobalExceptionHandler filtra ex.toString(), mapaEmbed validación débil / v-html XSS).

## Summary
Tres correcciones puntuales y acotadas sobre código ya existente, sin tocar el modelo de datos ni los endpoints públicos: (1) quitar el fallback de `jwt.secret` en `application.properties`; (2) hacer genérico el mensaje del handler 500; (3) sustituir la validación `.contains("google.com/maps/embed")` de `mapaEmbed` por un patrón estricto de iframe.

## Technical Context
**Language/Version**: Java 21, sin nuevas dependencias.
**Primary Dependencies**: ninguna nueva; se reutiliza `jakarta.validation` y `java.util.regex.Pattern` (stdlib).
**Storage**: sin cambios de esquema.
**Testing**: JUnit 5 + Mockito + MockMvc + Spring Security Test, mismo estilo que `001-eventos-crud`.
**Target Platform**: mismo Docker/Render, sin cambios de infraestructura.
**Constraints**: no romper ningún test existente de `001-eventos-crud` (38/38 en verde debe seguir en verde).

## Constitution Check
- [x] I. Constructor injection: sin cambios, no se tocan constructores.
- [x] II. Stdlib-first: `java.util.regex.Pattern` para validar `mapaEmbed`, sin librería de sanitización HTML externa.
- [x] III. Sin ServiceImpl nuevo: cambios dentro de ficheros ya existentes (`GlobalExceptionHandler`, `application.properties`, `EventoDTO`).
- [x] IV. Tests obligatorios: test nuevo para cada uno de los 3 hallazgos.
- [x] V. Seguridad explícita: refuerza, no debilita, la seguridad existente.
- [x] VI. Fail-fast JWT_SECRET: completa lo que dejó a medias `JwtSecretKeyProvider`.
- [x] VII. BCrypt: sin cambios.

## Project Structure

### Documentation (this feature)
```text
specs/002-hardening-seguridad/
├── plan.md              # This file
└── tasks.md             # Phase 2
```
(Sin `research.md`/`data-model.md`/`contracts/`: no hay decisiones de diseño abiertas ni cambios de modelo — es spec de corrección, no de feature nueva.)

### Source Code (archivos afectados, todos ya existentes)
```text
backend/src/main/resources/application.properties                          # quitar default de jwt.secret
backend/src/main/java/.../exception/GlobalExceptionHandler.java            # mensaje genérico en handleGlobalException
backend/src/main/java/.../dto/EventoDTO.java                               # patrón estricto para mapaEmbed
backend/src/test/java/.../exception/GlobalExceptionHandlerTest.java        # nuevo
backend/src/test/java/.../controller/EventoControllerAuthTest.java         # casos nuevos de mapaEmbed inválido
backend/specs/001-eventos-crud/quickstart.md                               # JWT_SECRET ahora obligatoria en local
docs/api-contract.md                                                       # documentar regla de mapaEmbed
```

**Structure Decision**: no se crean paquetes nuevos; todos los cambios son ediciones puntuales sobre ficheros de `001-eventos-crud`.

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Ninguna | — | — |