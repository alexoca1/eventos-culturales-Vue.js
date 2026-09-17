# Feature Specification: Backend REST Eventos Culturales Puertollano

**Feature Branch**: `001-eventos-crud`

**Created**: 2026-09-16

**Status**: Implemented (2026-09-17, 38/38 tests en verde; ver tasks.md)

**Input**: User description: "Backend REST para "Eventos Culturales Puertollano": gestión y consulta de eventos culturales de la localidad..."

## User Scenarios & Testing

### User Story 1 - Invitado consulta eventos por fecha (Priority: P1)

Invitado sin autenticación elige una fecha (YYYY-MM-DD) y ve la lista completa de eventos de ese día: establecimiento, dirección, fecha, descripción, cartel y ubicación/mapa.

**Why this priority**: Es el caso de uso principal del frontend actual (`usuarioEstandar.html` + `searchEvent()`); sin esto no hay producto.

**Independent Test**: `GET /eventos?fecha=2026-10-01` sin token devuelve 200 con todos los eventos de ese día.

**Acceptance Scenarios**:

1. **Given** existen 2 eventos el 2026-10-01, **When** invitado pide `GET /eventos?fecha=2026-10-01` sin token, **Then** recibe 200 con los 2 eventos completos.
2. **Given** no hay eventos ese día, **When** invitado pide esa fecha, **Then** recibe 200 con lista vacía (el frontend muestra la imagen de aviso).

---

### User Story 2 - Admin gestiona eventos (Priority: P1)

Administrador autenticado (JWT) crea, edita, elimina y consulta eventos.

**Why this priority**: Sustituye el array en memoria y el CRUD incompleto del frontend (falta editar); sin persistencia no hay backend.

**Independent Test**: Con token `ROLE_ADMIN` se puede crear (`POST`), editar (`PUT`), borrar (`DELETE`) y ver (`GET`) un evento.

**Acceptance Scenarios**:

1. **Given** admin autenticado, **When** `POST /eventos` con datos válidos, **Then** recibe 201 con el evento persistido.
2. **Given** petición sin token o con `ROLE_USER`, **When** `POST/PUT/DELETE /eventos`, **Then** recibe 401/403 y el dato no cambia.
3. **Given** admin autenticado, **When** `PUT /eventos/{id}` o `DELETE /eventos/{id}`, **Then** el evento se actualiza/elimina y persiste.

---

### User Story 3 - Siembra y gestión de admins sin auto-registro público (Priority: P2)

El primer admin se siembra al arrancar; crear admins adicionales exige estar ya autenticado como admin. No existe endpoint público de auto-registro de administradores.

**Why this priority**: Corrige el defecto de padel (`/auth/register-admin` sin protección efectiva); seguridad desde el día uno.

**Independent Test**: Arrancar en BD vacía crea el admin semilla; `POST /auth/register` público nunca otorga `ROLE_ADMIN`; crear un admin exige token admin.

**Acceptance Scenarios**:

1. **Given** BD vacía al arrancar, **When** la app inicializa, **Then** existe un admin con password BCrypt de variable de entorno.
2. **Given** petición anónima, **When** intenta crear un admin, **Then** es rechazada (401/403).

---

### Edge Cases

- Varios eventos en la misma fecha: la consulta por fecha devuelve todos, no solo uno (rompe el supuesto 1-fecha=1-evento del frontend actual).
- Fecha con formato inválido en query → 400 con mensaje claro.
- Crear/editar con campos obligatorios vacíos → 400 por validación.
- `id` inexistente en GET/PUT/DELETE → 404.
- Cartel o ubicación con contenido malformado no corrompe el resto de campos del evento (se valida y se rechaza solo ese campo).
- Cartel GIF o de más de 2 MB por parte → 400 con mensaje claro (el frontend ya redimensiona, es red de seguridad).

## Requirements

### Functional Requirements

- **FR-001**: El sistema MUST exponer `GET /eventos?fecha=YYYY-MM-DD` público sin autenticación que devuelva todos los eventos de ese día.
- **FR-002**: El sistema MUST exponer `GET /eventos` y `GET /eventos/{id}` (públicos, el listado admite filtro por fecha).
- **FR-003**: El sistema MUST exigir autenticación `ROLE_ADMIN` para `POST /eventos`, `PUT /eventos/{id}` y `DELETE /eventos/{id}`, con `@PreAuthorize("hasRole('ADMIN')")` explícito en el controller.
- **FR-004**: El sistema MUST persistir Evento con: establecimiento, dirección, fecha, descripción, cartel (bytes en MySQL: display 400px + HD tope 1600px) y ubicación (mapa) en MySQL. `cartelUrl` queda solo para URLs externas.
- **FR-005**: El sistema MUST permitir más de un evento por fecha (sin constraint UNIQUE sobre fecha).
- **FR-006**: El sistema MUST validar campos obligatorios y formato de fecha, devolviendo 400 ante datos inválidos.
- **FR-007**: El sistema MUST sembrar el primer admin al arrancar (env `ADMIN_SEED_PASSWORD`, BCrypt) y MUST NOT exponer ningún endpoint público que otorgue `ROLE_ADMIN`.
- **FR-008**: El sistema MUST autenticar con JWT (access + refresh rotado en cookie HttpOnly) reutilizando el modelo de padel-backend.
- **FR-009**: El sistema MUST documentar la API con springdoc-openapi.
- **FR-010**: El sistema MUST aceptar el cartel en `POST/PUT /eventos` como `multipart/form-data` (parte `evento` JSON + partes `file`/`fileHd` opcionales; solo JPEG/PNG/WebP, ≤ 2 MB por parte; los GIF se rechazan con 400). Sin ficheros, el PUT conserva los carteles.
- **FR-011**: El sistema MUST exponer `GET /eventos/{id}/cartel` y `GET /eventos/{id}/cartel-hd` públicos (bytes con Content-Type; 302 si solo hay URL externa; 404 si no hay cartel).

### Key Entities

- **Evento**: establecimiento (string obligatorio), dirección (string obligatoria), fecha (LocalDate obligatoria, no única), descripción/contenido (string obligatorio), cartel (bytes `LONGBLOB` display + HD con su Content-Type; `cartelUrl` solo URLs externas), ubicación/mapa (embed validado `google.com/maps/embed`).
- **Usuario**: email único, password BCrypt, roles (`ROLE_USER`/`ROLE_ADMIN`), como en padel-backend.
- **RefreshToken**: token rotado con reuse-detection, como en padel-backend.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Un invitado sin token obtiene por fecha la lista completa de eventos de ese día (todos, no solo uno).
- **SC-002**: Todo intento de crear/editar/eliminar sin token admin es rechazado (401/403) y no modifica datos.
- **SC-003**: Guardar cartel o ubicación nunca pierde ni corrompe los demás campos del evento.
- **SC-004**: `./mvnw test` en verde: lógica no trivial cubierta (Mockito) y autorización de endpoints cubierta (MockMvc + Security Test).
- **SC-005**: Todo evento con cartel sirve display (`/cartel`) y HD (`/cartel-hd`) públicos con su Content-Type.

## Assumptions

- Frontend Vue actual se adapta después para consumir `GET /eventos?fecha=` y JWT (fuera de este spec).
- MySQL 8 disponible en local; `ddl-auto=update` en desarrollo como en padel.
- Sin reservas, notificaciones ni búsqueda por texto/categoría en v1 (fuera de alcance explícito).
- Cartel: subida binaria multipart implementada (display + HD en BD); el frontend redimensiona a WebP en navegador, así que por tamaño nunca se rechaza, solo por tipo (sin GIF).
