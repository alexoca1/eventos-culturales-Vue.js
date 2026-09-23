# Feature Specification: Gestión de Usuarios (roles y estado) — Eventos Culturales Puertollano

**Feature Branch**: `009-gestion-admins`
**Created**: 2026-09-18
**Status**: Implemented (2026-09-22, 107/107 tests en verde; ver tasks.md)
**Input**: El admin hoy no puede editar usuarios (ni roles ni estado). Se necesita para poder ascender a ROLE_ORGANIZADOR y para desactivar cuentas problemáticas. GET /auth/usuarios tampoco expone `enabled`.

## User Scenarios & Testing

### User Story 1 - Listado de usuarios incluye su estado (Priority: P1)
`GET /auth/usuarios` incluye `enabled` en cada usuario devuelto, además de los campos que ya expone.

**Independent Test**: `GET /auth/usuarios` como admin devuelve `enabled` (true/false) por cada usuario.

**Acceptance Scenarios**:
1. **Given** admin autenticado, **When** `GET /auth/usuarios`, **Then** cada elemento incluye `id, email, nombre, apellidos, roles, telefono, enabled`.

---

### User Story 2 - Admin edita roles y estado de un usuario (Priority: P1)
Un admin puede cambiar los roles de cualquier usuario (excepto los suyos propios en ciertos casos, ver Historia 3) y activar/desactivar su cuenta.

**Independent Test**: `PUT /auth/usuarios/{id}` con `{"roles": ["ROLE_ORGANIZADOR"], "enabled": true}` sobre un usuario `ROLE_USER` → pasa a `ROLE_ORGANIZADOR`.

**Acceptance Scenarios**:
1. **Given** admin autenticado y un usuario `ROLE_USER`, **When** `PUT /auth/usuarios/{id}` con `roles=["ROLE_ORGANIZADOR"]`, **Then** el usuario pasa a tener ese rol (puede loguearse y usar el panel de organizador).
2. **Given** admin autenticado, **When** `PUT /auth/usuarios/{id}` con `enabled=false`, **Then** ese usuario ya no puede autenticarse (`login` devuelve 401/403 — reutiliza el mecanismo ya existente de `UserDetails.isEnabled()`).
3. **Given** admin autenticado, **When** envía `roles=[]` (vacío) o un valor fuera de `ROLE_USER/ROLE_ORGANIZADOR/ROLE_ADMIN`, **Then** 400.
4. **Given** un usuario `ROLE_ORGANIZADOR` (no admin), **When** intenta llamar a este endpoint, **Then** 403.

---

### User Story 3 - Un admin no puede auto-bloquearse (Priority: P1)
Un admin no puede, mediante este endpoint, desactivarse a sí mismo ni quitarse su propio `ROLE_ADMIN`.

**Independent Test**: admin autenticado intenta `PUT /auth/usuarios/{su propio id}` con `enabled=false` o sin `ROLE_ADMIN` en `roles` → 409.

**Acceptance Scenarios**:
1. **Given** admin autenticado, **When** `PUT` sobre su propio `id` con `enabled=false`, **Then** 409, sin aplicar el cambio.
2. **Given** admin autenticado, **When** `PUT` sobre su propio `id` con `roles` que no incluye `ROLE_ADMIN`, **Then** 409, sin aplicar el cambio.
3. **Given** admin autenticado, **When** `PUT` sobre su propio `id` con `enabled=true` y `roles` incluyendo `ROLE_ADMIN` (sin cambios reales, o añadiendo otro rol adicional), **Then** se acepta (no se bloquea editarse a sí mismo si no compromete su propio acceso de admin).

---

### Edge Cases
- Un usuario puede tener varios roles a la vez (ej. `ROLE_ADMIN` + `ROLE_ORGANIZADOR`) — el endpoint no lo impide, aunque no es un caso de uso esperado en el flujo normal.
- Cambiar los roles de un usuario que tiene eventos creados (`Evento.creadoPor`) no afecta a esos eventos ya existentes ni a su historial.

## Requirements

### Functional Requirements
- **FR-001**: `GET /auth/usuarios` MUST incluir `enabled` en cada elemento devuelto.
- **FR-002**: `PUT /auth/usuarios/{id}` (solo `ROLE_ADMIN`) MUST permitir actualizar `roles` (conjunto no vacío, valores dentro de `ROLE_USER, ROLE_ORGANIZADOR, ROLE_ADMIN`) y `enabled` (boolean).
- **FR-003**: El sistema MUST rechazar con 400 un `roles` vacío o con valores fuera de la lista permitida.
- **FR-004**: El sistema MUST rechazar con 409 cualquier intento de un admin de desactivarse a sí mismo o quitarse su propio `ROLE_ADMIN` mediante este endpoint.
- **FR-005**: Un usuario sin `ROLE_ADMIN` MUST recibir 403 al llamar a este endpoint.

### Key Entities
- Sin entidades nuevas — se reutiliza `Usuario` (ya tiene `roles: Set<String>` desde `003-robustez-auth-cors` y `enabled` desde el inicio del proyecto).

## Success Criteria

### Measurable Outcomes
- **SC-001**: Ascender a un `ROLE_USER` a `ROLE_ORGANIZADOR` vía este endpoint le permite loguearse y acceder a `organizador.html`.
- **SC-002**: Desactivar una cuenta le impide loguearse inmediatamente después.
- **SC-003**: Un admin nunca puede quedarse sin acceso de admin a través de este endpoint.
- **SC-004**: `./mvnw test` en verde (suite `001` a `009`).

## Assumptions
- No se crea un endpoint de "crear organizador directamente" — el flujo es: la persona/organización se registra como `ROLE_USER` (ya público) y el admin la asciende. Si en el futuro se necesita invitar directamente sin autoregistro, es una feature aparte.
- No se implementa borrado de usuarios en esta feature — solo edición de roles/estado. Desactivar (`enabled=false`) cubre el caso de "quitar acceso" sin los problemas de integridad referencial de borrar un usuario que tiene eventos asociados (`creadoPor`).