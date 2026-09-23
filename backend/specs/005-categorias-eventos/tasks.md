# Tasks: Categorías de Eventos — Eventos Culturales Puertollano

**Input**: `specs/005-categorias-eventos/spec.md` + `plan.md`

---

## Phase 1: US1 — Asignar categoría (P1)
- [x] T069 [US1] Crear enum `CategoriaEvento { MUSICA, TEATRO, EXPOSICION, CINE, LITERATURA, INFANTIL, OTROS }`
- [x] T070 [US1] Añadir `categoria` (`@Enumerated(EnumType.STRING)`, nullable) a `Evento.java` y a `EventoDTO`
- [x] T071 [US1] Test: crear evento con cada una de las 7 categorías → se guarda y recupera igual; crear sin categoría → sigue igual que antes; enviar un valor inválido → 400

**Checkpoint**: categoría opcional se guarda, se lee, y rechaza valores fuera de la lista.

---

## Phase 2: US2 — Filtrar por categoría (P1)
- [x] T072 [US2] Test en `EventoControllerPublicTest`: los 4 escenarios de combinación fecha/categoría del spec
- [x] T073 [US2] `EventoRepository`: añadir `findByCategoriaOrderByIdAsc` y `findByFechaAndCategoriaOrderByIdAsc`
- [x] T074 [US2] `EventoController.findAll(...)`: añadir `@RequestParam(required = false) CategoriaEvento categoria` y ramificar entre los 4 casos (ninguno / solo fecha / solo categoría / ambos), sin tocar el comportamiento de los dos casos que ya existían

**Checkpoint**: los 4 escenarios de combinación de filtros funcionan y ninguno de los 2 casos previos (ya cubiertos por `001`) se rompe.

---

## Phase 3: Polish
- [x] T075 [P] Asignar una categoría a cada uno de los 5 eventos semilla en `DataInitializer`
- [x] T076 Actualizar `docs/api-contract.md`: documentar `categoria` en el modelo y el nuevo query param en `GET /eventos`
- [x] T077 `./mvnw test` completo en verde (suite `001` a `005`)

## Dependencies & Execution Order
US1 antes de US2 (el campo debe existir antes de poder filtrar por él). T075-T077 al final.

## Notas
- Cada prompt ≈ 1 historia completa (T069–T071, T072–T074).