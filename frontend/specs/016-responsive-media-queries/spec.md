# Feature Specification: Responsive móvil/tablet — Frontend

**Feature Branch**: `016-responsive-media-queries`
**Created**: 2026-09-25
**Status**: Implemented (2026-09-25, ver tasks.md)
**Input**: Punto 8 de mejoras (último, sobre el layout ya definitivo): la única media query existente (768px para `.control`) no cubre el header unificado, las tarjetas (mínimo 360px que desborda en móvil), las filas de gestión, la toolbar `.acciones` ni el dropdown de sesión. Dos breakpoints: 768px (tablet retrato + móvil) y 480px (móvil pequeño).

## User Scenarios & Testing

### User Story 1 - La app se usa en móvil y tablet (Priority: P1)
En pantallas estrechas no hay desbordamiento horizontal y los controles siguen siendo pulsables sin zoom.

**Independent Test**: abrir cada página a 390px y 768px de ancho → sin scroll horizontal; header, filtros, tarjetas, gestión y formularios legibles y usables.

**Acceptance Scenarios**:
1. **Given** viewport ≤768px, **When** se abre cualquier página, **Then** el header se apila, las tarjetas ocupan el 100% y las filas de gestión ponen las acciones debajo.
2. **Given** viewport ≤480px, **When** se abre cualquier página, **Then** los botones de acciones van uno por fila y el `main` respira menos padding.
3. **Given** `index.css`, **When** se audita, **Then** su bloque 768px ya cubre la landing (apilado + full-width): sin cambios.

## Requirements

### Functional Requirements
- **FR-001**: El bloque `@media (max-width: 768px)` de `eventos.css` MUST cubrir `.header-bar` (columna), `.mostrar_evento > div` (100%), `.evento-row`/`.evento-acciones` (wrap + ancho completo), `.acciones button` (2 por fila), `.menu-sesion` (max 90vw) y `.control .fecha` (wrap de checkboxes).
- **FR-002**: Un bloque `@media (max-width: 480px)` MUST poner `.acciones button` a fila completa, repartir `.evento-acciones` y compactar `main`/`h1`.
- **FR-003**: Solo valores y vars ya existentes (cero colores/tamaños nuevos).

## Success Criteria

### Measurable Outcomes
- **SC-001**: Llaves equilibradas en ambas hojas + auditoría de que cada selector del media existe en el HTML.
- **SC-002**: Revisión visual en navegador (pendiente del usuario: no hay navegador automatizado en este proyecto).

## Assumptions
- Tablets en horizontal usan el layout de escritorio (el wrap natural de flex ya las atiende).
- La verificación pixel a pixel en dispositivo real la hace el usuario (F167 queda como guía + checklist hecho por inspección).
