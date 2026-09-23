# Tasks: Eventos de varios días (fechaFin) — Frontend

**Input**: `specs/007-eventos-varios-dias/spec.md` + `plan.md`

> Tareas retrodocumentadas (2026-09-23): describen trabajo ya hecho y verificado en el código.

---

## Phase 1: US1 — Fecha fin en formularios (P1)
- [x] F069 [US1] Dato `inputFechaFin` + input `type="date"` "Fecha fin (opcional, vacío = un día)" en los formularios de `administrador.html` y `organizador.html`
- [x] F070 [US1] `submitForm()` y `previsualizar()`: aviso "La fecha de fin no puede ser anterior a la de inicio" si `inputFechaFin < inputDate`, sin enviar; DTO con `fechaFin: inputFechaFin || null`
- [x] F071 [US1] `editEvento(ev)`: precarga `inputFechaFin` con `ev.fechaFin` si difiere de `ev.date` (vacío si es de un día); modo crear lo vacía

**Checkpoint**: crear/editar un evento con fecha fin funciona de punta a punta contra el backend `006-eventos-varios-dias`.

---

## Phase 2: US2 — Mostrar rango al invitado (P1)
- [x] F072 [US2] `apiToView()`: mapear `fechaFin: e.fechaFin || e.fecha` (null antiguo = un día)
- [x] F073 [US2] `formatRango(ev)`: `""` si `fechaFin` ausente o igual a `date`, si no `"DD/MM/AA → DD/MM/AA"`; línea de rango en `usuarioEstandar.html` (resultado + "Mis favoritos"), `administrador.html` (gestión) y previsualización de `organizador.html`, solo cuando no sea vacía

**Checkpoint**: el rango se ve correctamente en las tres vistas; los eventos de un día no muestran nada nuevo.

---

## Phase 3: Polish
- [x] F074 `node --check js/eventos.js` sin errores; prueba manual de los 3 escenarios (un día / varios días / fin anterior rechazado)

## Dependencies & Execution Order
US1 antes de US2 (necesitas poder crear eventos con fecha fin antes de poder mostrar el rango). Requiere `backend/specs/006-eventos-varios-dias` ya implementado y corriendo.

## Notas
- Numeración F069–F074 continúa la de `006-favoritos-recordatorios` (F067–F068).
