# Tasks: Filtro por categoría — Frontend

**Input**: `specs/003-categorias-eventos/spec.md` + `plan.md`

---

## Phase 1: US1 — Admin asigna categoría (P1)
- [x] F021 [US1] `<select>` de categoría (7 opciones + "Sin categoría") en el formulario de `administrador.html`
- [x] F022 [US1] `submitForm()`: incluir `categoria` en el `FormData` (vacío si "Sin categoría")
- [x] F023 [US1] `editEvento(ev)`: precargar la categoría actual en el `<select>`

**Checkpoint**: crear/editar con categoría funciona de punta a punta contra `005-categorias-eventos`.

---

## Phase 2: US2 — Invitado filtra por categoría (P1)
- [x] F024 [US2] `apiToView()`: mapear `categoria`
- [x] F025 [US2] `<select>` de categoría junto al buscador de fecha en `usuarioEstandar.html` (y en `administrador.html` para su propio buscador)
- [x] F026 [US2] `searchEvent()`: incluir `categoria` como query param combinado con `fecha` cuando esté seleccionado, sin cambiar el comportamiento cuando no lo está

**Checkpoint**: los 3 escenarios de búsqueda combinada funcionan correctamente.

---

## Phase 3: Polish
- [x] F027 `node --check js/eventos.js` sin errores; prueba manual de los 3 escenarios de US2

## Dependencies & Execution Order
US1 antes de US2. Requiere `backend/specs/005-categorias-eventos` ya implementado.

## Notas
- Cada prompt ≈ 1 historia completa (F021–F023, F024–F026).