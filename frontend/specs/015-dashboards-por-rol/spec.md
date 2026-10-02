# Feature Specification: Dashboards por rol — Frontend

**Feature Branch**: `015-dashboards-por-rol`
**Created**: 2026-09-25
**Status**: Implemented (2026-09-25, ver tasks.md)
**Input**: Punto 7 de mejoras: cada rol tiene su sección "Mi panel" con contadores y accesos; la barra de navegación no cambia. Sin backend nuevo (todo se deriva de endpoints ya existentes).

## User Scenarios & Testing

### User Story 1 - Mi panel según el rol (Priority: P1)
El item "Mi panel" del menú abre una sección dashboard en la propia página (sin recargar), con contadores frescos y botones a lo que ese rol más usa.

**Independent Test**: abrir "Mi panel" en cada página → contadores correctos; cerrarlo vuelve a la portada.

**Acceptance Scenarios**:
1. **Given** usuario normal, **When** abre su panel, **Then** saludo + nº próximos + nº favoritos + accesos (Verlos / Editar mis datos).
2. **Given** organizador, **When** abre su panel, **Then** sus eventos por estado + próximos en cartelera (con enlace a la cartelera pública, que conserva la sesión) + Nuevo evento / Editar mis datos.
3. **Given** admin, **When** abre su panel, **Then** totales (eventos, pendientes, usuarios, etiquetas) + accesos a las 6 secciones.
4. **Given** cualquier sección (buscar/gestionar/perfil…), **When** se abre, **Then** el panel se oculta (y viceversa).

---

### Edge Cases
- Los contadores son una foto al abrir (no live); las listas se refrescan al navegar a ellas como siempre.
- Si falla la red al abrir, el panel se abre igual con los contadores previos (los errores ya los gestiona `http.js`).

## Requirements

### Functional Requirements
- **FR-001**: `usePanel` MUST gestionar `showPanel`, contadores por rol (vía api directa, sin efectos laterales) y `abrirPanel()`/`cerrarPanel()`; abrir MUST ocultar las demás secciones.
- **FR-002**: "Mi panel" del menú MUST llamar a `irAPanel()` (sección, no recarga); se elimina `panelUrl`.
- **FR-003**: Cada página MUST tener su sección dashboard con sus contadores y accesos; la barra de navegación MUST quedar igual.
- **FR-004**: Abrir cualquier otra sección MUST ocultar el panel.

## Success Criteria

### Measurable Outcomes
- **SC-001**: Panel correcto en las 3 páginas con contadores que cuadran con las listas.
- **SC-002**: `node --check` sin errores.
- El admin puede acceder a la vista pública de eventos desde su dashboard.

## Assumptions
- Sin backend nuevo: los contadores salen de `proximos/mios/listarTodos/pendientes/listarUsuarios/listarEtiquetas` (+ favoritos ya cargados).
- El enlace "cartelera pública" del organizador resuelve su queja de no ver el buscador por fecha (misma sesión, sin duplicar la búsqueda).
