# Implementation Plan: Eventos de varios días (fechaFin) — Frontend

**Branch**: `007-eventos-varios-dias` | **Date**: 2026-09-23 | **Spec**: `specs/007-eventos-varios-dias/spec.md`

> Plan retrodocumentado: describe la implementación ya existente en `js/eventos.js` y las vistas.

## Summary
Un dato `inputFechaFin` + un input `type="date"` opcional en los formularios de admin y organizador (con validación cliente `fin < inicio` y `fechaFin: null` = un día), y una función `formatRango(ev)` que pinta "DD/MM/AA → DD/MM/AA" en las tarjetas cuando `fechaFin` difiere de `fecha`, usando directamente lo que ya trae la API.

## Technical Context
**Language/Version**: JS plano, Vue 3 global, sin build (igual que `002-horario-eventos`).
**Dependencies**: ninguna nueva.
**Testing**: `node --check js/eventos.js` + prueba manual, mismo gate que las features anteriores.

## Constitution Check
(mismos principios que ya siguen las specs anteriores: sin librerías nuevas, sin router/store nuevos, YAGNI)
- [x] Sin reescritura del enfoque existente.
- [x] Sin recalcular en JS lo que el backend ya trae (`fechaFin` real por evento).

## Project Structure

### Documentation (this feature)
```text
specs/007-eventos-varios-dias/
├── plan.md
└── tasks.md
```

### Source Code
```text
frontend/js/eventos.js               # inputFechaFin, apiToView(), submitForm(), previsualizar(), editEvento(), formatRango()
frontend/views/administrador.html    # input fecha fin en el formulario + línea de rango en tarjetas
frontend/views/usuarioEstandar.html  # línea de rango en resultado y en "Mis favoritos"
frontend/views/organizador.html      # input fecha fin en el formulario + rango en previsualización
```

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Ninguna | — | — |
