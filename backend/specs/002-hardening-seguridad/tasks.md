# Tasks: Hardening de Seguridad — Eventos Culturales Puertollano

**Input**: `specs/002-hardening-seguridad/spec.md` + `plan.md`
**Tests**: Obligatorios por constitución IV.

## Formato: `[ID] [P?] [Story] Descripción`

---

## Phase 1: US1 — JWT_SECRET sin fallback inseguro (P1)
- [x] T039 [US1] Quitar el valor de fallback de `jwt.secret` en `backend/src/main/resources/application.properties` (queda `jwt.secret=${JWT_SECRET:}` con default vacío a propósito: no es una clave usable, el fail-fast con mensaje claro lo hace `JwtSecretKeyProvider`)
- [x] T040 [US1] Actualizar `backend/specs/001-eventos-crud/quickstart.md`: `JWT_SECRET` pasa a obligatoria también en local (incluidos los tests)
- [x] T041 [US1] Verificar manualmente que arrancar sin `JWT_SECRET` falla con `IllegalStateException` ("Falta la variable de entorno JWT_SECRET... openssl rand -base64 32") — verificado 2026-09-18

**Checkpoint**: la app no arranca sin `JWT_SECRET` real.

---

## Phase 2: US2 — Errores 500 genéricos (P1)
- [x] T042 [US2] Test nuevo (MockMvc) que fuerza una excepción no controlada y comprueba que el body 500 NO contiene el nombre de clase ni el mensaje de la excepción original
- [x] T043 [US2] Modificar `handleGlobalException` en `GlobalExceptionHandler.java`: body fijo `Map.of("error", "Error interno del servidor")`; mantener el detalle completo solo en el log del servidor

**Checkpoint**: 500 genérico verificado por test; resto de handlers (400/403) sin cambios.

---

## Phase 3: US3 — Validación estricta de `mapaEmbed` (P1)
- [x] T044 [US3] Test nuevo en `EventoControllerAuthTest`: `mapaEmbed` con HTML adicional alrededor del iframe → 400; iframe válido de Google Maps → 200/201
- [x] T045 [US3] Definir patrón estricto de iframe válido de Google Maps (atributos permitidos: `src`, `width`, `height`, `style`, `allowfullscreen`, `loading`, `referrerpolicy`) en `EventoDTO`
- [x] T046 [US3] Actualizar `docs/api-contract.md` con la nueva regla de validación de `mapaEmbed`

**Checkpoint**: `mapaEmbed` fuera del patrón se rechaza con 400; ya no depende de `.contains(...)`.

---

## Phase 4: Polish
- [x] T047 `./mvnw test` completo en verde (38 tests previos + nuevos de esta feature)

## Dependencies & Execution Order
- US1, US2 y US3 son independientes entre sí — cualquier orden vale.
- T047 depende de que T039–T046 estén completas.

## Notas
- Cada prompt ≈ 1 historia completa (T039–T041, T042–T043, T044–T046).
- Commit tras cada historia.