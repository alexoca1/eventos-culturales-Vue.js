# Tasks: Horario de Eventos (inicio/fin) — Eventos Culturales Puertollano

**Input**: `specs/004-horario-eventos/spec.md` + `plan.md`

---

## Phase 1: US1 — Registrar horario (P1)
- [x] T060 [US1] Añadir `horaInicio`, `horaFin` (`LocalTime`, nullable) a `Evento.java`
- [x] T061 [US1] Añadir `horaInicio`, `horaFin` a `EventoDTO` (record de entrada)
- [x] T062 [US1] Test: crear evento con horario válido → se guarda y se recupera igual; crear sin horario → sigue funcionando igual que antes

**Checkpoint**: horario opcional se guarda y se lee correctamente; eventos existentes sin horario no se rompen.

---

## Phase 2: US2 — Validación de horario coherente (P1)
- [x] T063 [US2] Test: solo `horaInicio` informado (sin `horaFin`) → 400; solo `horaFin` → 400; ambas o ninguna → aceptado
- [x] T064 [US2] Validación en `EventoController` (create/update): rechazar con 400 si solo una de las dos está informada

**Checkpoint**: no es posible guardar un evento con horario a medias.

---

## Phase 3: US3 — `fechaFin` derivado (P2)
- [x] T065 [US3] Test: evento 22:00→02:00 (mismo `horaFin <= horaInicio`) → `fechaFin` = `fecha + 1`; evento 20:00→22:30 → `fechaFin` = `fecha`; evento sin horario → `fechaFin` = `fecha`; caso borde `horaInicio == horaFin` → `fechaFin` = `fecha + 1`
- [x] T066 [US3] Añadir `@Transient public LocalDate getFechaFin()` en `Evento.java` con la lógica: si `horaInicio`/`horaFin` son null → `fecha`; si `horaFin > horaInicio` → `fecha`; si `horaFin <= horaInicio` → `fecha.plusDays(1)`
- [x] T067 [US3] Actualizar `docs/api-contract.md`: documentar `horaInicio`, `horaFin` y el campo derivado `fechaFin` (solo lectura, no se envía en `POST`/`PUT`)

**Checkpoint**: la API expone `fechaFin` correctamente calculado en todos los casos de `spec.md`.

---

## Phase 4: Polish
- [x] T068 `./mvnw test` completo en verde (suite `001`+`002`+`003`+`004`)

## Dependencies & Execution Order
US1 → US2 → US3, en ese orden (cada una depende de que el campo exista antes de validarlo o calcular sobre él). T068 al final.

## Notas
- Cada prompt ≈ 1 historia completa (T060–T062, T063–T064, T065–T067).