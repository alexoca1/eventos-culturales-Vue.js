# Feature Specification: Derechos ARCO+ (Supresión y Portabilidad RGPD)

**Feature Branch**: `018-rgpd-supresion-portabilidad`
**Created**: 2026-10-03
**Status**: Completed

## User Scenarios & Testing

### User Story 1 - Supresión de Cuenta (Derecho al Olvido - Priority: P1)

Un usuario autenticado debe poder eliminar su propia cuenta (`DELETE /auth/perfil`). Al hacerlo, se anonimizan sus datos personales (email sustituido por `deleted_[timestamp]@removed.local`, nombre por "Usuario eliminado", contraseña vacía, borrado de refresh tokens y favoritos) mientras que sus eventos creados se disocian manteniendo la trazabilidad legal requerida por la LSSI/RGPD.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado, **When** solicita `DELETE /auth/perfil`, **Then** recibe 200 OK (o 204 No Content), su token/sesión queda invalidada y sus datos personales son anonimizados en base de datos.
2. **Given** un usuario anonimizado, **When** intenta hacer login con su email previo, **Then** responde 401 Unauthorized.

### User Story 2 - Portabilidad de Datos (Priority: P1)

Un usuario autenticado debe poder descargar un extracto completo de sus datos personales en formato JSON (`GET /auth/perfil/exportar`).

**Acceptance Scenarios**:

1. **Given** un usuario autenticado con perfil, eventos creados y favoritos, **When** `GET /auth/perfil/exportar`, **Then** recibe un JSON con su información personal, roles, fecha de registro, lista de sus eventos y sus favoritos.

---

## Requirements

### Functional Requirements

- **FR-001**: El sistema MUST exponer `DELETE /auth/perfil` (autenticado) que anonimiza al usuario (`email = deleted_{timestamp}@removed.local`, `nombre = "Usuario eliminado"`, `password = ""`, `enabled = false`), elimina sus refresh tokens y sus registros de favoritos.
- **FR-002**: El sistema MUST exponer `GET /auth/perfil/exportar` (autenticado) que devuelve un JSON con todos los datos procesados del usuario conforme al art. 20 del RGPD.
