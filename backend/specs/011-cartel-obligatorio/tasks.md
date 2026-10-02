# Tasks: Cartel obligatorio al crear — Backend

**Input**: `specs/011-cartel-obligatorio/spec.md` + `plan.md`

---

## Phase 1: US1 — Cartel obligatorio en POST (P1)
- [x] T139 [US1] En `createEvento`: si `file` nulo/vacío → 400 "El cartel es obligatorio al crear el evento" (antes que el resto de validaciones); `updateEvento` sin cambios
- [x] T140 [US1] Test `post_admin_sinCartel_400`; añadir `.file(imagenJpeg())` (+ stub `OTROS` donde aplique) a los 8 tests de 201 sin fichero
- [x] T141 Actualizar `docs/api-contract.md` (`POST /eventos`: `file` obligatorio)
- [x] T142 `./mvnw test` completo en verde (141/141)
