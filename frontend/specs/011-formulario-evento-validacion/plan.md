# Implementation Plan: Validación visible del formulario — Frontend

**Branch**: `011-formulario-evento-validacion` | **Date**: 2026-09-24 | **Spec**: `specs/011-formulario-evento-validacion/spec.md`

## Summary
`validarObligatorios()` en `useEventoForm` (5 refs de error + `getElementById().focus()`), llamado lo primero en `previsualizar()`/`submitForm()`; ids en los inputs y `<p style="color:red">` en ambas plantillas; copy de 2 pasos en organizador.

## Technical Context
**Language/Version**: JS plano, Vue 3, sin build.
**Testing**: `node --check` + prueba manual (formulario vacío → rojos + foco).

## Constitution Check
(mismos principios de las specs anteriores: sin librerías, YAGNI)
- [x] Sin reescritura — se añade una función y se cablea en 2 puntos existentes.

## Project Structure
```text
specs/011-formulario-evento-validacion/
├── plan.md
└── tasks.md
```
```text
frontend/js/composables/useEventoForm.js  # validarObligatorios() + refs err*
frontend/views/administrador.html         # ids, mensajes rojos, label cartel/descripción
frontend/views/organizador.html           # ids, mensajes rojos, label cartel/descripción, copy 2 pasos
```

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Ninguna | — | — |
