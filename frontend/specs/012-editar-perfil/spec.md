# Feature Specification: Editar perfil — Frontend

**Feature Branch**: `012-editar-perfil`
**Created**: 2026-09-24
**Status**: Implemented (2026-09-24, ver tasks.md)
**Input**: Consumir `PUT /auth/perfil` (backend `012-perfil-usuario-organizacion`): sección "Mi perfil" en las 3 páginas, teléfono obligatorio también en los formularios de alta, y nombre de organización como visible en tarjetas.

## User Scenarios & Testing

### User Story 1 - Mi perfil en cada página (Priority: P1)
Cada página logueada tiene botón "Mi perfil" y sección con nombre/apellidos/teléfono (+ bloque organización si es organizador).

**Acceptance Scenarios**:
1. **Given** cualquier página con sesión, **When** pulsa "Mi perfil", **Then** se abre la sección precargada y se ocultan las demás.
2. **Given** guarda con teléfono vacío, **When** el backend devuelve 400, **Then** se muestra el error sin cerrar.
3. **Given** organizador, **When** abre su perfil, **Then** ve el bloque de organización (obligatorio); resto de roles no lo ven.
4. **Given** guarda OK, **When** ocurre, **Then** aviso "Guardado.".

---

### User Story 2 - Teléfono en las altas (Priority: P1)
Registro público y "Crear admin" piden teléfono obligatorio.

**Acceptance Scenarios**:
1. **Given** registro sin teléfono, **When** envía, **Then** el backend devuelve 400 y se muestra el error (además del `required` del input).

---

### User Story 3 - Nombre de organización visible (Priority: P1)
Las tarjetas muestran el nombre de la organización del creador cuando lo tiene.

**Acceptance Scenarios**:
1. **Given** evento creado por organización con nombre, **When** se muestra, **Then** "De:" es el nombre, no el email.

## Requirements

### Functional Requirements
- **FR-001**: `usePerfil` MUST gestionar `showPerfil`, precarga (`abrirPerfil`), guardado (`guardarPerfil(esOrg)`) y mensajes; abrir MUST ocultar las demás secciones.
- **FR-002**: Las 3 páginas MUST tener botón y sección "Mi perfil" (con bloque org solo en organizador).
- **FR-003**: Registro y crear-admin MUST enviar `telefono` (input `required`).
- **FR-004**: `apiToView()` MUST preferir `nombreOrganizacion` sobre `email` en `creadoPor`.

## Success Criteria

### Measurable Outcomes
- **SC-001**: Editar y guardar perfil en las 3 páginas sin errores.
- **SC-002**: `node --check` sin errores.

## Assumptions
- El dropdown de sesión unificada (editar/dashboards/cerrar) es del punto 5 — aquí el acceso es un botón por página.
