# Feature Specification: Visibilidad de eventos no públicos en GET /eventos/{id}

**Feature Branch**: `010-visibilidad-eventos-no-publicos`
**Created**: 2026-09-23
**Status**: Implemented (2026-09-23, ver tasks.md)
**Input**: GET /eventos/{id} (detalle individual) no filtra por estado, a diferencia de GET /eventos (listado). Cualquiera que conozca o adivine un id puede leer eventos PENDIENTE_REVISION/RECHAZADO/PENDIENTE_ELIMINACION, incluyendo motivoRechazo y el email del organizador (creadoPor).

## User Scenarios & Testing

### User Story 1 - El detalle de un evento no aprobado no es público (Priority: P1)
`GET /eventos/{id}` de un evento no `APROBADO` MUST devolver 404 salvo que quien pregunta sea `ROLE_ADMIN`, o el `ROLE_ORGANIZADOR` dueño de ese evento.

**Independent Test**: un evento `RECHAZADO` de un organizador → `GET /eventos/{id}` sin login devuelve 404; el mismo organizador autenticado lo ve con su `motivoRechazo`; un admin también lo ve.

**Acceptance Scenarios**:
1. **Given** un evento `APROBADO`, **When** cualquiera (con o sin login) hace `GET /eventos/{id}`, **Then** se devuelve normalmente (sin cambios respecto a hoy).
2. **Given** un evento `PENDIENTE_REVISION`/`RECHAZADO`/`PENDIENTE_ELIMINACION`, **When** un invitado sin login o un usuario que no es su dueño hace `GET /eventos/{id}`, **Then** 404 (no 403 — no se confirma que el id existe).
3. **Given** el mismo evento, **When** el organizador dueño (`creadoPor`) lo consulta autenticado, **Then** se devuelve completo, incluyendo `motivoRechazo` si lo tiene.
4. **Given** el mismo evento, **When** un admin lo consulta, **Then** se devuelve completo (comportamiento actual, sin cambios).

---

### Edge Cases
- Un `ROLE_ORGANIZADOR` que no es dueño del evento (ni admin) MUST recibir 404, igual que un invitado — no debe distinguirse "existe pero no es tuyo" de "no existe".
- `/eventos/{id}/cartel` y `/eventos/{id}/cartel-hd` NO se tocan en esta feature (ver Assumptions).

## Requirements

### Functional Requirements
- **FR-001**: `GET /eventos/{id}` MUST devolver 404 cuando `evento.estado != APROBADO`, salvo que el llamante sea `ROLE_ADMIN` o el `ROLE_ORGANIZADOR` cuyo `creadoPor` coincida con el evento.
- **FR-002**: Un evento `APROBADO` MUST seguir siendo visible para cualquiera, con o sin autenticación (sin cambios).

## Success Criteria

### Measurable Outcomes
- **SC-001**: Los 4 escenarios de la Historia 1 pasan en test automatizado.
- **SC-002**: `./mvnw test` en verde (suite completa).

## Assumptions
- `/eventos/{id}/cartel` y `/cartel-hd` se dejan sin cambios: son endpoints `<img src>` sin `Authorization`, y el organizador necesita ver el cartel de sus propios eventos no aprobados en su panel ("Mis eventos"). El riesgo residual (una imagen de cartel accesible por id, sin el resto de datos del evento) se acepta como bajo — no hay texto sensible en una imagen de cartel.