# Feature Specification: Filtro por categoría — Frontend

**Feature Branch**: `003-categorias-eventos`
**Created**: 2026-09-18
**Status**: Implemented (2026-09-22, ver tasks.md)
**Input**: Consumir el campo `categoria` y el filtro `?categoria=` de la API (backend `005-categorias-eventos`).

## User Scenarios & Testing

### User Story 1 - Admin asigna categoría al crear/editar (Priority: P1)
El formulario incluye un desplegable con las 7 categorías fijas + opción "Sin categoría".

**Acceptance Scenarios**:
1. **Given** el admin crea un evento, **When** elige una categoría del desplegable, **Then** se envía en el payload.
2. **Given** el admin edita un evento existente, **When** abre el formulario, **Then** el desplegable precarga la categoría actual (o "Sin categoría" si no tiene).

---

### User Story 2 - Invitado filtra por categoría (Priority: P1)
La vista de invitado añade un selector de categoría junto al buscador de fecha, combinable con ella.

**Acceptance Scenarios**:
1. **Given** el invitado elige una categoría sin fecha, **When** busca, **Then** ve todos los eventos de esa categoría.
2. **Given** el invitado elige fecha + categoría, **When** busca, **Then** ve solo los que cumplen ambas.
3. **Given** el invitado no elige categoría, **When** busca solo por fecha, **Then** comportamiento actual sin cambios.

---

### Edge Cases
- El desplegable de categorías MUST usar exactamente los mismos 7 valores que el backend (evitar un value hardcodeado que se desincronice si el backend cambia la lista).

## Requirements

### Functional Requirements
- **FR-001**: El formulario de admin MUST incluir un `<select>` con las 7 categorías + "Sin categoría".
- **FR-002**: `apiToView()` MUST mapear `categoria`.
- **FR-003**: La vista de invitado MUST incluir un selector de categoría que se combine con la búsqueda por fecha existente, enviando `?categoria=` en el `fetch` cuando esté seleccionado.
- **FR-004**: Buscar sin categoría seleccionada MUST comportarse exactamente igual que antes de esta feature.

## Success Criteria

### Measurable Outcomes
- **SC-001**: Crear un evento con categoría y verlo reflejado en el filtro de invitado.
- **SC-002**: Los 3 escenarios de la Historia 2 devuelven los resultados esperados.
- **SC-003**: `node --check js/eventos.js` sin errores.