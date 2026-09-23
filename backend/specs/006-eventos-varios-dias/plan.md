# Implementation Plan: Eventos de varios días — Eventos Culturales Puertollano

**Branch**: `006-eventos-varios-dias` | **Date**: 2026-09-19 | **Spec**: `specs/006-eventos-varios-dias/spec.md`

## Summary

`fechaFin` pasa de getter derivado a columna real nullable (null en entrada = un día, normalizado a `fecha` al guardar); validación fin ≥ inicio; `GET /eventos?fecha=` por solape de rangos (consultas `@Query`, NULL-tolerantes); frontend con input de fecha fin, validación cliente y pintado de rango `"11/11/26 → 17/11/26"`.

## Technical Context

**Language/Version**: Java 21 + JS plano, sin dependencias nuevas.
**Storage**: 1 columna nullable (`ddl-auto=update`) + backfill a un día en `DataInitializer`.
**Testing**: JUnit 5 + MockMvc (solape verificado por stubs del repositorio + 400 de fin anterior); `node --check` + prueba de render en frontend.
**Constraints**: no romper suite previa; el aviso "(día siguiente)" sigue usando `fechaFin` tal cual.

## Constitution Check

- [x] II. Stdlib-first: `LocalDate`, `@Query` JPQL estándar.
- [x] III. Sin over-engineering: una columna, sin entidad de rango ni servicio.
- [x] IV. Tests: roundtrip multi-día, 400 fin anterior, normalización, ramas de `findAll` intactas.

## Project Structure

```text
backend/.../entities/Evento.java            # fechaFin: campo real (adiós @Transient)
backend/.../dto/EventoDTO.java               # + fechaFin opcional
backend/.../controller/EventoController.java # validarFechas + normalización en aplicar()
backend/.../repositories/EventoRepository.java # findVigentesEn(+PorCategoria) por solape
backend/.../config/DataInitializer.java      # backfill fechaFin null → fecha
frontend/js/eventos.js                       # inputFechaFin, validación, formatRango, preload
frontend/views/administrador.html            # input fecha fin + rango en resultados y tarjetas
frontend/views/usuarioEstandar.html          # línea de rango
docs/api-contract.md                         # fechaFin real + semántica de solape
```

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Ninguna | — | — |
