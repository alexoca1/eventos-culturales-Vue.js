# Implementation Plan: Buscador de eventos por texto

**Branch**: `016-buscador-texto` | **Date**: 2026-09-25

## Summary
2 métodos nuevos en `EventoRepository` (@Query con LIKE escapado) +
1 rama nueva en `findAll()` de `EventoController` (cuando `q` no es
vacío) + validaciones de longitud y exclusividad con `fecha`/`futuros`.

## Métodos nuevos en EventoRepository

```java
// Público (solo APROBADO)
@Query("SELECT e FROM Evento e WHERE e.estado = :estado AND " +
       "(LOWER(e.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
       " LOWER(e.establecimiento) LIKE LOWER(CONCAT('%', :q, '%'))) " +
       "ORDER BY e.fecha ASC, e.id ASC")
Page<Evento> buscarPorTextoYEstado(String q, EstadoEvento estado, Pageable p);

// Admin (todos los estados)
@Query("SELECT e FROM Evento e WHERE " +
       "(LOWER(e.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
       " LOWER(e.establecimiento) LIKE LOWER(CONCAT('%', :q, '%'))) " +
       "ORDER BY e.fecha ASC, e.id ASC")
Page<Evento> buscarPorTexto(String q, Pageable p);
```

## Escape de caracteres LIKE
```java
private static String escaparLike(String q) {
    return q.replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
}
```

## Project Structure
```text
backend/.../repositories/EventoRepository.java   # + 2 métodos
backend/.../controller/EventoController.java      # + 1 rama en findAll() + validaciones
backend/src/test/.../EventoControllerPublicTest.java  # tests nuevos
docs/api-contract.md                              # documentar ?q=
```