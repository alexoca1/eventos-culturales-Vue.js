# Feature Specification: Cartel obligatorio al crear — Backend

**Feature Branch**: `011-cartel-obligatorio`
**Created**: 2026-09-24
**Status**: Implemented (2026-09-24, ver tasks.md)
**Input**: Hoy se puede crear un evento sin cartel (`post_admin_sinImagen_201`). Punto 3 de mejoras: la imagen del cartel pasa a ser obligatoria al crear; en edición se sigue conservando la existente si no se sube otra.

## User Scenarios & Testing

### User Story 1 - Crear sin cartel se rechaza (Priority: P1)
`POST /eventos` sin parte `file` (o vacía) MUST devolver 400 con mensaje claro. `PUT /eventos/{id}` no cambia (sin ficheros conserva carteles).

**Independent Test**: `POST /eventos` con JSON válido pero sin `file` → 400 "El cartel es obligatorio al crear el evento".

**Acceptance Scenarios**:
1. **Given** admin autenticado, **When** `POST /eventos` sin `file`, **Then** 400 y no se guarda nada.
2. **Given** admin autenticado, **When** `PUT /eventos/{id}` sin ficheros, **Then** 200 y conserva los carteles (comportamiento actual, sin cambios).
3. **Given** el resto de validaciones, **When** fallan además del cartel, **Then** el cartel se valida primero (400 igual, mensaje de cartel).

## Requirements

### Functional Requirements
- **FR-001**: `POST /eventos` MUST rechazar con 400 si `file` es nulo o vacío.
- **FR-002**: `PUT /eventos/{id}` MUST seguir aceptando ausencia de ficheros (conserva existentes).

## Success Criteria

### Measurable Outcomes
- **SC-001**: Test `post_admin_sinCartel_400` en verde; tests de 201 adaptados con `.file(imagenJpeg())`.
- **SC-002**: `./mvnw test` en verde (suite completa).

## Assumptions
- Solo se exige `file` (display); `fileHd` sigue opcional (el frontend lo manda igual).
