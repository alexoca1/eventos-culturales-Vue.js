# Implementation Plan: Paginación en servidor — Eventos Culturales Puertollano

**Branch**: `014-paginacion` | **Date**: 2026-09-25 | **Spec**: `specs/014-paginacion/spec.md`

## Summary
Migrar los 10 métodos de `EventoRepository` que usa `findAll()` de `List<Evento>` a `Page<Evento>` añadiendo `Pageable` como parámetro. En `EventoController`, construir un `PageRequest.of(page, size)` y pasarlo a cada rama. `GET /eventos/mios` recibe el mismo tratamiento. `GET /eventos/pendientes` no se toca.

## Technical Context
**Language/Version**: Java 21, sin dependencias nuevas (Spring Data `Pageable`/`Page` ya están en el classpath).
**Storage**: sin cambios de esquema — Spring Data genera el `LIMIT`/`OFFSET` automáticamente.
**Testing**: JUnit 5 + MockMvc, mismo estilo.
**Constraints**: no romper ningún filtro ya existente; `GET /eventos/pendientes` intacto.

## Constitution Check
- [x] II. Stdlib-first: `Pageable`/`Page` son Spring Data estándar, sin librería nueva.
- [x] III. Sin over-engineering: tamaño fijo, sin DTO envolvente (Spring serializa `Page<>` bien).
- [x] IV. Tests obligatorios: cubiertos en tasks.md.

## Métodos de EventoRepository a migrar (verificados contra el código actual)

| Método actual (devuelve `List<Evento>`) | Nuevo (devuelve `Page<Evento>`) |
|---|---|
| `findAllByOrderByFechaAscIdAsc()` | + `Pageable` |
| `findByEstadoOrderByFechaAscIdAsc(estado)` | + `Pageable` |
| `findFuturos(hoy)` | + `Pageable` |
| `findFuturosPorEstado(estado, hoy)` | + `Pageable` |
| `findFuturosPorEtiquetas(hoy, tags)` | + `Pageable` |
| `findFuturosPorEstadoYEtiquetas(estado, hoy, tags)` | + `Pageable` |
| `findDistinctByEtiquetasNombreInOrderByIdAsc(tags)` | + `Pageable` |
| `findDistinctByEstadoAndEtiquetasNombreInOrderByIdAsc(estado, tags)` | + `Pageable` |
| `findVigentesEn(fecha)` ¹ | sin cambios (devuelve List — búsqueda por fecha exacta, suele ser pequeña) |
| `findVigentesEnPorEstado(estado, fecha)` ¹ | sin cambios |
| `findVigentesEnPorEtiquetas(fecha, tags)` ¹ | sin cambios |
| `findVigentesEnPorEstadoYEtiquetas(estado, fecha, tags)` ¹ | sin cambios |
| `findByCreadoPorOrderByIdAsc(organizador)` | + `Pageable` (GET /eventos/mios) |

¹ Los métodos de búsqueda por fecha exacta devuelven los eventos de un día concreto — en la práctica suelen ser pocos y no justifican paginar.

## Project Structure

```text
backend/src/main/java/.../repositories/EventoRepository.java   # + Pageable en 9 métodos
backend/src/main/java/.../controller/EventoController.java      # + @RequestParam page, PageRequest.of(), devuelve Page<>
backend/src/main/resources/application.properties               # + app.paginacion.size=10
backend/src/test/java/.../controller/EventoControllerPublicTest.java  # tests nuevos
docs/api-contract.md                                             # documentar ?page=, formato Page<>
```

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| Ninguna | — | — |