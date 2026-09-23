# Tasks: Eventos de varios días — Eventos Culturales Puertollano

**Input**: `specs/006-eventos-varios-dias/spec.md` + `plan.md`

---

## Phase 1: Backend (US1 + US2)

- [x] B001 `Evento.fechaFin`: de getter `@Transient` a columna `LocalDate` nullable
- [x] B002 `EventoDTO.fechaFin` opcional + normalización null→fecha en `aplicar()` + `validarFechas` (fin < inicio → 400) en create/update
- [x] B003 `EventoRepository.findVigentesEn(+PorCategoria)` por solape (`@Query`, NULL-tolerante) + ramas en `findAll`; retirados los métodos de igualdad por fecha
- [x] B004 Backfill `fechaFin` null → fecha en `DataInitializer` (filas anteriores)
- [x] B005 Tests: roundtrip multi-día, normalización sin fecha fin, 400 fin anterior, ramas fechaFin real (68/68 en verde)

## Phase 2: Frontend (US3)

- [x] F001 Input fecha fin opcional + validación cliente fin ≥ inicio + envío `null` si vacío
- [x] F002 `formatRango()` (`"11/11/26 → 17/11/26"`, vacío si un día) en resultados invitado/admin y tarjetas de gestión; `editEvento` precarga fecha fin
- [x] F003 `node --check` verde

## Phase 3: Docs

- [x] D001 `docs/api-contract.md`: `fechaFin` real, solape en `?fecha=`, 400 fin anterior
- [x] D002 `backend/AGENTS.md`: modelo con `fechaFin` + búsqueda por vigencia
- [x] D003 Specs `006-eventos-varios-dias/` (spec/plan/tasks) en `Implemented`

**Checkpoint**: suite 68/68 + `node --check` + verificación en vivo del solape.
