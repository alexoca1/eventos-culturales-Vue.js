# Feature Specification: Perfil editable + datos de organización — Backend

**Feature Branch**: `012-perfil-usuario-organizacion`
**Created**: 2026-09-24
**Status**: Implemented (2026-09-24, ver tasks.md)
**Input**: Punto 4 de mejoras: cada usuario edita sus propios datos; teléfono obligatorio; al pasar a ORGANIZADOR se exigen nombre de la organización + datos del encargado (nombre, teléfono, correo); el nombre visible pasa a ser el de la organización.

## User Scenarios & Testing

### User Story 1 - Editar el propio perfil (Priority: P1)
Cualquier autenticado puede editar su nombre, apellidos y teléfono vía `PUT /auth/perfil` (nunca rol ni estado).

**Independent Test**: `PUT /auth/perfil {"telefono":"600111222"}` → 200 y queda guardado.

**Acceptance Scenarios**:
1. **Given** usuario autenticado, **When** `PUT /auth/perfil` con nombre/teléfono válidos, **Then** 200 con el perfil actualizado.
2. **Given** envía `telefono` vacío, **When** ocurre, **Then** 400 (el teléfono es obligatorio, no se puede vaciar).
3. **Given** no-organizador envía datos de organización, **When** ocurre, **Then** 200 pero se ignoran (siguen a null).
4. **Given** `GET /auth/perfil`, **When** se consulta, **Then** incluye `telefono` y los 4 campos de organización.

---

### User Story 2 - Teléfono obligatorio en el registro (Priority: P1)
`POST /auth/register` y `POST /auth/usuarios-admin` rechazan con 400 sin teléfono.

**Acceptance Scenarios**:
1. **Given** registro sin `telefono`, **When** `POST /auth/register`, **Then** 400.
2. **Given** usuarios legacy con teléfono null, **When** editan su perfil, **Then** deben informarlo (no se inventa ningún valor en migración).

---

### User Story 3 - Datos de organización al pasar a organizador (Priority: P1)
Promocionar a `ROLE_ORGANIZADOR` (vía admin) o editarse como organizador exige los 4 datos; el nombre visible es el de la organización.

**Acceptance Scenarios**:
1. **Given** admin, **When** `PUT /auth/usuarios/{id}` con `roles=[ORGANIZADOR]` sin datos org, **Then** 400 sin guardar (valen los ya guardados si completan el estado final).
2. **Given** con los 4 datos, **When** ocurre, **Then** 200 y quedan guardados.
3. **Given** organizador edita su perfil incompleto, **When** `PUT /auth/perfil`, **Then** 400 hasta completarlo.
4. **Given** la autoprotección 409, **When** coincide con falta de datos org, **Then** manda el 409 (va antes).

## Requirements

### Functional Requirements
- **FR-001**: `Usuario` MUST tener `nombreOrganizacion, encargadoNombre, encargadoTelefono, encargadoEmail` (nullables en BD).
- **FR-002**: `RegisterRequest.telefono` MUST ser `@NotBlank` (aplica a `/register` y `/usuarios-admin`).
- **FR-003**: `PUT /auth/usuarios/{id}` MUST rechazar con 400 si el rol final es `ORGANIZADOR` y el estado final de los 4 datos queda incompleto; MUST aplicar los datos cuando vienen.
- **FR-004**: `PUT /auth/perfil` (autenticado) MUST permitir nombre/apellidos/teléfono (no vacíos) y, solo si es organizador, los 4 datos (completos); MUST ignorar datos org de no-organizadores.
- **FR-005**: `GET /auth/usuarios` y `GET /auth/perfil` MUST exponer `telefono` y los 4 datos.

### Key Entities
- **Usuario**: + 4 campos org (nullables; la obligatoriedad es de API, no de columna, para no romper filas legacy).

## Success Criteria

### Measurable Outcomes
- **SC-001**: 7 tests nuevos en verde (registro, perfil x4, promoción x2).
- **SC-002**: `./mvnw test` en verde (148/148).

## Assumptions
- Los teléfonos null legacy no se rellenan en migración (se exigen al editar).
- El email sigue siendo el identificador/login; solo el nombre *visible* cambia a la organización.
