# Feature Specification: Mostrar y editar horario de eventos — Frontend

**Feature Branch**: `002-horario-eventos`
**Created**: 2026-09-18
**Status**: Implemented (2026-09-19, ver tasks.md)
**Input**: Consumir los nuevos campos `horaInicio`, `horaFin`, `fechaFin` de la API (backend `004-horario-eventos`) en el formulario de admin y en las vistas de invitado/gestión.

## User Scenarios & Testing

### User Story 1 - Admin indica el horario al crear/editar (Priority: P1)
El formulario de crear/editar evento incluye dos campos de hora opcionales.

**Independent Test**: Crear un evento con horario desde el formulario → se envía correctamente en el `multipart/form-data` junto al resto de campos.

**Acceptance Scenarios**:
1. **Given** el admin abre "Nuevo evento", **When** no rellena horario, **Then** el evento se crea igual que hoy (sin horario).
2. **Given** el admin rellena `horaInicio` y `horaFin`, **When** guarda, **Then** ambas viajan en el payload.

---

### User Story 2 - El horario (con aviso de día siguiente) se muestra al invitado (Priority: P1)
La vista de invitado muestra el horario del evento cuando existe, usando el `fechaFin` que ya calcula el backend — sin reimplementar la lógica de cruce de medianoche en JS.

**Independent Test**: Un evento con `horaInicio=22:00`, `horaFin=02:00` muestra algo como "22:00 - 02:00 (día siguiente)".

**Acceptance Scenarios**:
1. **Given** un evento sin horario, **When** se muestra, **Then** no aparece ninguna línea de horario (comportamiento actual, sin cambios).
2. **Given** un evento con horario en el mismo día (`fechaFin` == `fecha`), **When** se muestra, **Then** aparece "HH:mm - HH:mm" sin aviso adicional.
3. **Given** un evento con horario que cruza medianoche (`fechaFin` != `fecha`), **When** se muestra, **Then** aparece "HH:mm - HH:mm (día siguiente)".

---

### Edge Cases
- El mapeo `apiToView()` debe incluir `horaInicio`/`horaFin`/`fechaFin` sin romper el resto de campos ya mapeados.
- La vista de gestión de admin (tarjetas) puede mostrar el horario de forma compacta, no es obligatorio que sea tan detallado como en la vista de invitado.

## Requirements

### Functional Requirements
- **FR-001**: El formulario de crear/editar MUST incluir `horaInicio` y `horaFin` como inputs `type="time"`, ambos opcionales.
- **FR-002**: `apiToView()` MUST mapear `horaInicio`, `horaFin` y `fechaFin` desde la respuesta de la API.
- **FR-003**: La vista de invitado MUST mostrar el horario cuando exista, con el aviso "(día siguiente)" cuando `fechaFin` de la API sea distinto de `fecha`. La comparación de fechas la hace el frontend con lo que ya calculó el backend — el frontend NO MUST reimplementar la regla `horaFin <= horaInicio`.
- **FR-004**: Editar un evento existente MUST precargar `horaInicio`/`horaFin` en el formulario si están informados.

## Success Criteria

### Measurable Outcomes
- **SC-001**: Crear un evento con horario desde el formulario y verlo reflejado correctamente en la vista de invitado.
- **SC-002**: Un evento que cruza medianoche muestra el aviso "(día siguiente)"; uno que no cruza, no lo muestra.
- **SC-003**: `node --check js/eventos.js` sin errores de sintaxis (mismo gate que `001-panel-admin-eventos`).

## Assumptions
- Se asume que `backend/specs/004-horario-eventos` ya está implementado y desplegado en el entorno donde se prueba esto (dependencia entre features).