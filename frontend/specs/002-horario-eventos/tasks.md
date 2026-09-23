# Tasks: Mostrar y editar horario de eventos — Frontend

**Input**: `specs/002-horario-eventos/spec.md` + `plan.md`

---

## Phase 1: US1 — Formulario de admin (P1)
- [x] F014 [US1] Añadir inputs `type="time"` para `horaInicio`/`horaFin` en el formulario de `administrador.html` (marcados como opcionales)
- [x] F015 [US1] `submitForm()` en `eventos.js`: incluir `horaInicio`/`horaFin` en el `FormData` enviado (vacío si no se rellenan)
- [x] F016 [US1] `editEvento(ev)`: precargar `horaInicio`/`horaFin` en el formulario si el evento los tiene

**Checkpoint**: crear/editar un evento con horario funciona de punta a punta contra el backend `004-horario-eventos`.

---

## Phase 2: US2 — Mostrar horario al invitado (P1)
- [x] F017 [US2] `apiToView()`: mapear `horaInicio`, `horaFin`, `fechaFin` desde la respuesta de la API
- [x] F018 [US2] En `usuarioEstandar.html`, mostrar `"{horaInicio} - {horaFin}"` cuando existan, añadiendo `" (día siguiente)"` cuando `fechaFin !== fecha` (comparación directa de los dos valores que ya trae la API, sin recalcular la regla de cruce)
- [x] F019 [US2] [P] Mostrar el horario de forma compacta también en las tarjetas de gestión de `administrador.html`

**Checkpoint**: el horario y el aviso de día siguiente se ven correctamente en ambas vistas.

---

## Phase 3: Polish
- [x] F020 `node --check js/eventos.js` sin errores; prueba manual de los 3 escenarios de US2 (sin horario / mismo día / cruza medianoche)

## Dependencies & Execution Order
US1 antes de US2 (necesitas poder crear eventos con horario antes de poder mostrarlos). Requiere `backend/specs/004-horario-eventos` ya implementado y corriendo.

## Notas
- Cada prompt ≈ 1 historia completa (F014–F016, F017–F019).