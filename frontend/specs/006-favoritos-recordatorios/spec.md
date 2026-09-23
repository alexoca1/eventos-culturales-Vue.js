# Feature Specification: Registro, Login y Favoritos de Usuario Estándar — Frontend

**Feature Branch**: `006-favoritos-recordatorios`
**Created**: 2026-09-18
**Status**: Implemented (2026-09-22, ver tasks.md)
**Input**: Consumir el backend de 008-favoritos-recordatorios. Un usuario estándar puede registrarse, loguearse (sin perder el acceso de invitado sin login), marcar/desmarcar eventos favoritos, y ver su lista de favoritos.

## User Scenarios & Testing

### User Story 1 - Registro de usuario estándar (Priority: P1)
`index.html` incluye un formulario de registro (además del login ya existente).

**Acceptance Scenarios**:
1. **Given** un visitante en `index.html`, **When** pulsa "Crear cuenta" y rellena email/password, **Then** se llama a `POST /auth/register` (ya existente en el backend, sin cambios) y, tras éxito, se hace login automático con esas credenciales.
2. **Given** el backend rechaza el registro (email ya existente, password inválida), **When** ocurre, **Then** se muestra el error sin perder los datos ya escritos.

---

### User Story 2 - Login de usuario estándar redirige a la vista de invitado, ahora autenticado (Priority: P1)
Completa el punto que quedó pendiente en `004-flujo-aprobacion-organizadores`: un login sin `ROLE_ADMIN` ni `ROLE_ORGANIZADOR` redirige a `usuarioEstandar.html`, con el token guardado.

**Acceptance Scenarios**:
1. **Given** login exitoso con solo `ROLE_USER`, **When** se completa, **Then** redirige a `views/usuarioEstandar.html` con el token en `sessionStorage`.
2. **Given** el usuario ya está en `usuarioEstandar.html` autenticado, **When** recarga la página, **Then** sigue reconocido como logueado (no se le pide login de nuevo mientras el token siga válido).

---

### User Story 3 - Marcar/desmarcar favorito desde la tarjeta de evento (Priority: P1)
Cada tarjeta de evento en `usuarioEstandar.html` tiene un icono de favorito.

**Acceptance Scenarios**:
1. **Given** un usuario autenticado (`ROLE_USER`), **When** pulsa el icono de favorito sobre un evento, **Then** se llama a `POST /eventos/{id}/favorito` (o `DELETE` si ya era favorito) y el icono cambia de estado al instante (sin esperar a recargar toda la lista).
2. **Given** un invitado sin login, **When** ve una tarjeta de evento, **Then** el icono de favorito es visible pero al pulsarlo lo redirige a `index.html` a loguearse/registrarse, en vez de fallar silenciosamente o dar un error de red.

---

### User Story 4 - Sección "Mis favoritos" (Priority: P1)
El usuario autenticado tiene una pestaña/sección con sus eventos favoritos.

**Acceptance Scenarios**:
1. **Given** usuario autenticado con favoritos, **When** abre "Mis favoritos", **Then** ve `GET /eventos/favoritos` renderizado con el mismo componente visual de tarjeta que el resto del sitio.
2. **Given** el usuario no tiene favoritos, **When** abre esa sección, **Then** ve un mensaje/imagen equivalente al ya existente "no hay eventos" (reutilizar el patrón visual actual).

---

### User Story 5 - Cerrar sesión de usuario estándar (Priority: P2)
`usuarioEstandar.html` no tenía antes ningún estado de sesión; ahora necesita poder cerrarla.

**Acceptance Scenarios**:
1. **Given** usuario autenticado, **When** pulsa "Cerrar sesión", **Then** se llama a `POST /auth/logout` (endpoint ya existente), se limpia `sessionStorage`, y la vista vuelve al modo invitado sin recargar la página.

---

### Edge Cases
- El estado de favorito de cada evento se calcula cruzando los IDs de `GET /eventos/favoritos` (cargado una vez al loguearse o al entrar ya logueado) contra la lista de eventos que se esté mostrando en cada momento — no se pide ese endpoint por cada tarjeta individualmente.
- Si el usuario marca un favorito y luego navega a "Mis favoritos" sin recargar, la lista debe reflejar el cambio (actualización local del array de favoritos, sin depender de un nuevo `GET`).
- La navegación de invitado (buscar por fecha, ver cartel, lightbox) sigue funcionando exactamente igual sin login — esta feature es aditiva, no ata ninguna funcionalidad existente a estar autenticado.

## Requirements

### Functional Requirements
- **FR-001**: `index.html` MUST incluir un formulario de registro que llame a `POST /auth/register` y, tras éxito, autentique automáticamente al nuevo usuario.
- **FR-002**: El login MUST redirigir a `usuarioEstandar.html` cuando el usuario autenticado no tenga `ROLE_ADMIN` ni `ROLE_ORGANIZADOR`.
- **FR-003**: Cada tarjeta de evento en `usuarioEstandar.html` MUST mostrar un icono de favorito, cuyo estado se calcula cruzando el `id` del evento con la lista de favoritos ya cargada del usuario.
- **FR-004**: Pulsar el icono de favorito sin estar autenticado MUST redirigir a `index.html`, no intentar la llamada a la API.
- **FR-005**: MUST existir una sección "Mis favoritos" que consuma `GET /eventos/favoritos` y reutilice el componente de tarjeta ya existente.
- **FR-006**: `usuarioEstandar.html` MUST incluir un control de "Cerrar sesión" visible solo cuando hay un usuario autenticado.

## Success Criteria

### Measurable Outcomes
- **SC-001**: Un visitante puede registrarse, loguearse, marcar 2-3 eventos como favoritos, verlos en "Mis favoritos", y cerrar sesión — todo sin salir del frontend ni tocar Swagger.
- **SC-002**: La navegación como invitado sin login no cambia en nada observable respecto a antes de esta feature.
- **SC-003**: `node --check js/eventos.js` sin errores.

## Assumptions
- No se valida en el frontend la fortaleza de la contraseña más allá de lo que ya exige el backend (`@NotBlank`, etc. en `RegisterRequest`) — no se duplica esa validación.
- El recordatorio por email (parte de `008-favoritos-recordatorios` backend) no tiene UI propia — es un proceso de servidor invisible para el usuario, no requiere ninguna pantalla.