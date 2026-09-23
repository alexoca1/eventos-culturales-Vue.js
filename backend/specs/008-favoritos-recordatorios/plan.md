# Implementation Plan: Favoritos y Recordatorio por Email — Eventos Culturales Puertollano

**Branch**: `008-favoritos-recordatorios` | **Date**: 2026-09-18 | **Spec**: `specs/008-favoritos-recordatorios/spec.md`

## Summary
Entidad `Favorito` (usuario, evento, recordatorioEnviado), 3 endpoints nuevos (`POST/DELETE /eventos/{id}/favorito`, `GET /eventos/favoritos`), limpieza de favoritos al borrar un evento, y un `@Scheduled` diario que reutiliza el `EmailService` ya creado en `007-flujo-aprobacion-organizadores`.

## Technical Context
**Language/Version**: Java 21.
**Primary Dependencies**: ninguna nueva — reutiliza `spring-boot-starter-mail`/`EmailService` de `007`. `@EnableScheduling` es parte de Spring Boot core, sin dependencia adicional.
**Storage**: nueva tabla `favorito` (FK a `usuario` y a `evento`, índice único compuesto).
**Testing**: JUnit 5 + Mockito + MockMvc; el job programado se testea invocando su método directamente (no esperando al cron real).

## Constitution Check
- [x] II. Stdlib-first: `@Scheduled` es de Spring Boot core (ya es una dependencia del proyecto), no se añade nada nuevo para la parte de scheduling.
- [x] III. Sin over-engineering: `Favorito` como entidad (no `@ManyToMany` simple) está justificado porque necesita un atributo propio (`recordatorioEnviado`) — no es una tabla de más, es el modelo correcto para esta relación.
- [x] IV. Tests obligatorios: cubiertos en tasks.md, incluyendo el caso de "no reenviar" (idempotencia) y el de "un fallo no bloquea al resto".
- [x] V. Seguridad explícita: favoritos requieren autenticación (`isAuthenticated()`), sin exponer eventos no aprobados vía 404 en vez de 403 (para no filtrar su existencia).

## Project Structure

### Documentation (this feature)
```text
specs/008-favoritos-recordatorios/
├── plan.md
└── tasks.md
```

### Source Code
```text
backend/src/main/java/.../entities/Favorito.java                    # nueva
backend/src/main/java/.../repositories/FavoritoRepository.java      # nueva
backend/src/main/java/.../services/RecordatorioService.java         # nuevo: @Scheduled, busca y envía
backend/src/main/java/.../controller/EventoController.java          # + favorito/desfavorito/mis-favoritos; limpieza en delete()
backend/src/main/java/.../EventosCulturalesApplication.java         # + @EnableScheduling
backend/src/main/resources/application.properties                   # app.recordatorios.cron (default 0 0 20 * * *)
backend/src/test/java/.../services/RecordatorioServiceTest.java     # nuevo
backend/src/test/java/.../controller/EventoControllerAuthTest.java  # tests de favoritos
docs/api-contract.md                                                  # documentar los 3 endpoints nuevos
```

**Structure Decision**: 3 ficheros nuevos pequeños (`Favorito`, `FavoritoRepository`, `RecordatorioService`); el resto son ediciones.

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Entidad `Favorito` en vez de `@ManyToMany` simple | Necesita el atributo `recordatorioEnviado` para evitar duplicar envíos | Un `@ManyToMany` puro no admite atributos propios de la relación; sería necesario igualmente una tabla intermedia con columna extra, que en JPA se modela como entidad — no hay alternativa más simple que siga cumpliendo FR-006 |