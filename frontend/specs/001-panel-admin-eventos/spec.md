# Feature Specification: Panel admin + conexión API del frontend

**Feature Branch**: `001-panel-admin-eventos`

**Created**: 2026-09-17

**Status**: Implemented (ver tasks.md)

**Input**: Conectar el frontend Vue existente a la API (`docs/api-contract.md`): login JWT, búsqueda por fecha con varios eventos/día, CRUD completo del admin, carteles WebP con lightbox.

## User Scenarios & Testing

### User Story 1 - Invitado consulta eventos por fecha (Priority: P1)

Invitado sin login elige fecha y ve TODOS los eventos del día (cartel display, datos, mapa). Clic en el cartel abre la HD en un lightbox.

**Independent Test**: Con el backend sembrado, buscar `2026-10-01` pinta el evento; un día vacío muestra la imagen de aviso.

**Acceptance Scenarios**:

1. **Given** hay 2 eventos ese día, **When** se busca la fecha, **Then** se pintan los 2.
2. **Given** clic en un cartel, **When** se abre el lightbox, **Then** se ve la versión HD; clic/Esc lo cierra.

---

### User Story 2 - Admin hace login y gestiona el CRUD (Priority: P1)

Login con email contra `POST /auth/login` (token en `sessionStorage`); sin token la vista admin redirige a `index.html`. Crear con formulario multipart; gestionar lista tarjetas compactas con Editar (formulario precargado → `PUT`) y Eliminar (confirm → `DELETE`).

**Independent Test**: Login demo → crear evento con foto → aparece en búsqueda; editarlo; eliminarlo.

**Acceptance Scenarios**:

1. **Given** credenciales malas, **When** login, **Then** aviso y sin redirección.
2. **Given** token caducado en una mutación, **When** responde 401/403, **Then** aviso + vuelta al login.
3. **Given** edición sin foto nueva, **When** se guarda, **Then** conserva los carteles.

---

### User Story 3 - Subida de cartel optimizada (Priority: P2)

Elegir cualquier foto la redimensiona en navegador a WebP (display 400px + HD 1600px) antes de subir; nunca se rechaza por tamaño, solo por tipo (sin GIF). La previsualización respeta la proporción.

**Independent Test**: Subir foto de móvil de 10 MB → viajan ~350 KB y la preview no se deforma.

---

### Edge Cases

- Backend caído → aviso "¿Está arrancado el backend?" en cada acción.
- Fecha vacía en el buscador → no hace nada.
- Eliminar con búsqueda activa → refresca búsqueda y mantiene la vista de gestión.

## Requirements

### Functional Requirements

- **FR-001**: El frontend MUST consumir la API vía `fetch` (base autodetectada: local/`file://` → `localhost:8081`, si no `RENDER_API`).
- **FR-002**: El frontend MUST pintar la lista completa del día (`dayEvents`) y el lightbox HD (`/cartel-hd`).
- **FR-003**: El admin MUST autenticarse con email (`POST /auth/login`) y guardar el JWT en `sessionStorage`; la vista admin sin token MUST redirigir.
- **FR-004**: Crear/editar MUST usar `FormData` multipart (`evento` JSON + `file`/`fileHd`); editar precarga el formulario (`editingId`) y conserva carteles sin fotos nuevas.
- **FR-005**: Gestionar MUST listar tarjetas compactas (mini 48px, fecha, establecimiento, contenido) con Editar/Eliminar; sin panel de checkboxes.
- **FR-006**: La subida MUST redimensionar a WebP en canvas y rechazar solo por tipo (sin GIF).

### Key Entities

- **Vista Evento** (`apiToView`): `{id, establishment, address, date, content, poster: "<img /cartel>", map}` mapeado desde el JSON de la API.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Invitado ve todos los eventos del día y amplía carteles al HD.
- **SC-002**: Admin completa crear → editar → eliminar sin recargar ni errores.
- **SC-003**: Una foto de 10 MB se sube como ~350 KB y la preview no se deforma.
- **SC-004**: `node --check js/eventos.js` en verde.

## Assumptions

- Backend corriendo según `backend/specs/001-eventos-crud/quickstart.md`.
- Sin build de frontend: JS plano + Vue global, despliegue estático en Netlify.
- Credenciales demo publicadas en `README.md` para la demo abierta.
