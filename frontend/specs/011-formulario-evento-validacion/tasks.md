# Tasks: Validación visible del formulario — Frontend

**Input**: `specs/011-formulario-evento-validacion/spec.md` + `plan.md`

---

## Phase 1: US1 — Rojo + foco (P1)
- [x] F145 [US1] `validarObligatorios()` en `useEventoForm` (5 refs `err*`, foco al primero vía `getElementById`); llamado lo primero en `previsualizar()` y `submitForm()`; limpieza al abrir el formulario
- [x] F146 [US1] Ids `f-establecimiento/f-direccion/f-fecha/f-contenido` + `<p style="color:red">` bajo cada input + cartel `(obligatorio)`/`(se conserva)` en ambas plantillas
- [x] F147 [US1] Etiqueta del campo: "Ingresar Descripción del Evento" (el campo ya existía)

## Phase 2: US2 — Copy en 2 pasos (P1)
- [x] F148 [US2] Botón "Previsualizar antes de confirmar" + aviso "Paso 1 de 2"; "Paso 2 de 2: Confirmar y enviar a revisión" en la previsualización

## Phase 3: Polish
- [x] F149 `node --check`; prueba manual (vacío → rojos + foco; sin cartel → mensaje)
