# Tasks: Visibilidad de eventos no públicos en GET /eventos/{id}

**Input**: `specs/010-visibilidad-eventos-no-publicos/spec.md` + `plan.md`

---

## Phase 1: US1 — Filtro de visibilidad (P1)
- [x] T125 [US1] Test: evento APROBADO visible para cualquiera (sin cambios); evento no APROBADO → 404 para invitado y para organizador que no es el dueño; visible completo (con motivoRechazo) para el organizador dueño; visible completo para admin
- [x] T126 [US1] En `EventoController.findById(...)`: añadir `Authentication authentication` como parámetro, y devolver 404 si `evento.getEstado() != APROBADO` y el llamante no es admin ni el dueño (`evento.getCreadoPor()` == usuario autenticado)

**Checkpoint**: `GET /eventos/{id}` respeta las mismas reglas de visibilidad que `GET /eventos`.

---

## Phase 2: Polish
- [x] T127 Actualizar `docs/api-contract.md`: documentar que `GET /eventos/{id}` devuelve 404 para eventos no `APROBADO` fuera de admin/dueño
- [x] T128 `./mvnw test` completo en verde

## Notas
- Un solo prompt cubre toda la feature (es pequeña).