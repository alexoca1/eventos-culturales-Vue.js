# Feature Specification: Pantalla de Gestión de Usuarios — Frontend

**Feature Branch**: `005-gestion-admins`
**Created**: 2026-09-18
**Status**: Implemented (2026-09-22, ver tasks.md)
**Input**: Consumir el backend de 006-gestion-admins (GET /auth/usuarios con enabled, PUT /auth/usuarios/{id}) y el ya existente POST /auth/usuarios-admin. Pantalla en el panel de admin para listar usuarios, ascender a ROLE_ORGANIZADOR, activar/desactivar cuentas, y crear nuevos admins.

## User Scenarios & Testing

### User Story 1 - Listado de usuarios con su rol y estado (Priority: P1)
Nueva sección "Gestión de usuarios" en `administrador.html` que lista todos los usuarios con email, roles y si están activos.

**Acceptance Scenarios**:
1. **Given** admin autenticado, **When** abre "Gestión de usuarios", **Then** ve `GET /auth/usuarios` renderizado como una tabla/lista: email, roles (badges), estado (Activo/Desactivado).
2. **Given** la lista está cargada, **When** el admin filtra visualmente (sin llamada nueva al backend) por rol, **Then** puede localizar rápido a los organizadores.

---

### User Story 2 - Cambiar el rol de un usuario (Priority: P1)
El admin puede cambiar el rol (único) de un usuario desde la propia lista, con radiobuttons.

**Acceptance Scenarios**:
1. **Given** un usuario con `ROLE_USER`, **When** el admin pulsa "Editar" y marca el radio `ROLE_ORGANIZADOR`, **Then** se llama a `PUT /auth/usuarios/{id}` con `roles=["ROLE_ORGANIZADOR"]` y la lista se actualiza.
2. **Given** el backend rechaza el cambio (ej. sin rol seleccionado), **When** ocurre, **Then** se muestra el mensaje de error sin cerrar el formulario de edición.

---

### User Story 3 - Activar/desactivar una cuenta (Priority: P1)
Un toggle por usuario para activar/desactivar, con confirmación.

**Acceptance Scenarios**:
1. **Given** un usuario activo, **When** el admin pulsa "Desactivar" y confirma, **Then** se llama a `PUT /auth/usuarios/{id}` con `enabled=false` y el badge de estado cambia.
2. **Given** el admin intenta desactivarse a sí mismo, **When** el backend devuelve 409, **Then** se muestra un mensaje claro ("No puedes desactivar tu propia cuenta") sin romper la UI.

---

### User Story 4 - Crear un nuevo admin (Priority: P2)
Reutiliza el endpoint ya existente `POST /auth/usuarios-admin` con un formulario simple.

**Acceptance Scenarios**:
1. **Given** admin autenticado, **When** rellena email/password en "Crear admin" y confirma, **Then** se llama a `POST /auth/usuarios-admin` y el nuevo admin aparece en la lista tras recargar.

---

### Edge Cases
- El propio usuario admin logueado MUST verse en la lista pero con las acciones de desactivar/quitar-admin deshabilitadas en la UI (evita que el admin intente algo que el backend igualmente rechazaría con 409 — mejor feedback inmediato que esperar el error).
- Los roles se eligen con radiobuttons (`ROLE_USER`, `ROLE_ORGANIZADOR`, `ROLE_ADMIN`, mismo `name`): un usuario solo puede tener un único rol. Al abrir la edición se preselecciona el rol actual (el primero si hubiera varios legacy).

## Requirements

### Functional Requirements
- **FR-001**: `administrador.html` MUST incluir una sección "Gestión de usuarios" con la lista de `GET /auth/usuarios`.
- **FR-002**: Cada usuario de la lista MUST mostrar email, roles (badges) y estado (Activo/Desactivado).
- **FR-003**: El admin MUST poder cambiar el rol (único) de cualquier usuario (excepto el suyo propio) mediante `PUT /auth/usuarios/{id}` con `roles` de exactamente 1 elemento.
- **FR-004**: El admin MUST poder activar/desactivar cualquier cuenta (excepto la suya propia) con confirmación previa.
- **FR-005**: La UI MUST deshabilitar (no solo depender del backend) las acciones de auto-desactivación/auto-quitar-admin sobre la fila del propio usuario logueado.
- **FR-006**: MUST existir un formulario para crear un nuevo admin, reutilizando `POST /auth/usuarios-admin` sin cambios en ese endpoint.

## Success Criteria

### Measurable Outcomes
- **SC-001**: Ascender un usuario a `ROLE_ORGANIZADOR` desde esta pantalla le permite loguearse en `organizador.html` (cierra el hueco dejado por `004-flujo-aprobacion-organizadores`, que asumía esto creado manualmente).
- **SC-002**: Desactivar una cuenta desde esta pantalla le impide loguearse.
- **SC-003**: `node --check js/eventos.js` sin errores.

## Assumptions
- No se implementa búsqueda/paginación del listado de usuarios en esta feature — para el volumen esperado (decenas de usuarios, no miles) una lista simple basta (YAGNI).