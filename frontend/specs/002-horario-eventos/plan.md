# Implementation Plan: Mostrar y editar horario de eventos — Frontend

**Branch**: `002-horario-eventos` | **Date**: 2026-09-18 | **Spec**: `specs/002-horario-eventos/spec.md`

## Summary
Dos inputs `type="time"` opcionales en el formulario de admin, y mostrar el horario (con aviso de día siguiente) en la vista de invitado, usando directamente el `fechaFin` que ya calcula el backend.

## Technical Context
**Language/Version**: JS plano, Vue 3 global, sin build (igual que `001-panel-admin-eventos`).
**Dependencies**: ninguna nueva.
**Testing**: `node --check js/eventos.js` + prueba manual, mismo gate que la feature anterior (justificado igual en su día: desproporcionado montar un framework de test para 1 fichero JS).

## Constitution Check
(mismos principios que ya sigue `001-panel-admin-eventos`: sin librerías nuevas, sin router/store nuevos, YAGNI)
- [x] Sin reescritura del enfoque existente.
- [x] Sin reimplementar en JS lo que el backend ya calcula (`fechaFin`).

## Project Structure

### Documentation (this feature)
```text
specs/002-horario-eventos/
├── plan.md
└── tasks.md
```

### Source Code
```text
frontend/js/eventos.js               # apiToView(), submitForm(), editEvento()
frontend/views/administrador.html    # inputs horaInicio/horaFin en el formulario
frontend/views/usuarioEstandar.html  # mostrar horario + aviso día siguiente
```

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Ninguna | — | — |