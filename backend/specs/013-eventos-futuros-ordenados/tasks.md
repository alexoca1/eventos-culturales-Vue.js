# Tasks: Eventos futuros ordenados — Backend

**Input**: `specs/013-eventos-futuros-ordenados/spec.md` + `plan.md`

---

## Phase 1: US1 (P1)
- [x] T148 4 queries `findFuturos[*]` (fechaFin ≥ hoy, orden fecha/id ASC, DISTINCT en ANY)
- [x] T149 Param `futuros` en `findAll` (+ ramas admin/público ± etiquetas; 400 si viene con `fecha`)
- [x] T150 4 tests (público, público+etiquetas, admin, 400 combo)
- [x] T151 Contrato (`?futuros=true`) + `./mvnw test` en verde (152/152)
