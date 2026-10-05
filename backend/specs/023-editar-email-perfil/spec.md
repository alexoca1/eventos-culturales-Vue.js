# Feature Specification: Editar email propio — Backend

**Feature Branch**: `023-editar-email-perfil`
**Created**: 2026-10-05
**Status**: Implemented (2026-10-05, ver tasks.md)
**Input**: Cada usuario puede cambiar su email desde `PUT /auth/perfil`. El email es el `sub` del JWT, así que cambiarlo invalida la identidad del token en vigor: el cliente lo renueva con `POST /auth/refresh` (la cookie de refresh apunta al `Usuario`, no al email, por lo que sigue viva).

## User Scenarios & Testing

### User Story 1 - Cambiar el propio email (Priority: P1)
Cualquier autenticado puede enviar `email` en `PUT /auth/perfil` y que quede guardado, con validación de formato y de unicidad.

**Independent Test**: `PUT /auth/perfil {"email":"nuevo@test.com"}` → 200 y el perfil responde con el email nuevo.

**Acceptance Scenarios**:
1. **Given** usuario autenticado, **When** `PUT /auth/perfil` con un email libre, **Then** 200, `email` guardado y `mapaPerfil` devuelve el nuevo.
2. **Given** envía un email con formato inválido, **When** ocurre, **Then** 400 `{"email": "El correo debe ser válido"}` y no se guarda nada.
3. **Given** envía un email que ya tiene otra cuenta, **When** ocurre, **Then** 409 `{"error": "Ese correo ya está en uso"}` y no se guarda nada (sin `DataIntegrityViolationException`).
4. **Given** envía el mismo email que ya tiene (en cualquier combinación de mayúsculas), **When** ocurre, **Then** 200 sin modificar nada (no-op).
5. **Given** no envía `email`, **When** ocurre, **Then** se ignora (null = "no lo toco"), como el resto de campos.

---

### User Story 2 - La cuenta demo conserva su email (Priority: P1)
Las credenciales publicadas en el README (`demo@eventos-culturales.es` / `Demo1234!`) no se pueden romper desde la propia sesión.

**Acceptance Scenarios**:
1. **Given** sesión demo, **When** `PUT /auth/perfil` con otro `email`, **Then** 403 con el mismo mensaje del `DemoAccountProtectionFilter`.
2. **Given** sesión demo, **When** edita nombre/teléfono, **Then** 200 (sigue permitido: el bloqueo es solo del email, como el DELETE ya lo era).

---

### User Story 3 - El token en vigor queda obsoleto (Priority: P1)
Comportamiento documentado del contrato: con el token anterior a la petición, cualquier endpoint que busque por `jwt.getSubject()` falla. El cliente **debe** renovar después de cambiar el email.

**Acceptance Scenarios**:
1. **Given** token emitido con el email A, **When** el email pasa a ser B, **Then** `GET /auth/perfil` con el token viejo responde 500 (no hay cuenta con ese `sub`) — verificado en vivo.
2. **Given** sesión con cookie de refresh válida, **When** `POST /auth/refresh`, **Then** 200 con un token cuyo `sub` es el email nuevo y `user.email` correcto.
3. **Given** login con el email nuevo, **When** ocurre, **Then** 200; con el antiguo, **Then** 401.

## Requirements

### Functional Requirements
- **FR-001**: `ActualizarPerfilRequest` MUST tener `email` con `@Email` (sin `@NotBlank`: null = no modificar; en blanco/inválido = 400).
- **FR-002**: `PUT /auth/perfil` MUST aplicar el cambio **después** de `findByEmail(jwt.getSubject())` y **antes** de guardar, en un `trim()` defensivo. En la práctica ese `trim()` nunca llega a ejecutarse: `@Valid`/`@Email` corta la petición antes de entrar en el método (verificado: `" prueba@x.com"` → 400), y el formulario ya lo evita con `v-model.trim`.
- **FR-003**: Si el email nuevo ya existe en otra cuenta, MUST responder 409 sin llamar a `save` (la columna es `UNIQUE`; sin esto el usuario vería un 500 sin explicación).
- **FR-004**: Si el usuario actual es la cuenta demo, MUST responder 403 y no modificar. El literal `DEMO_EMAIL` MUST ser único: la constante pasa a ser `public` en `DemoAccountProtectionFilter` y se reutiliza desde el controller.
- **FR-005**: `email` igual al actual (case-insensitive) MUST ser no-op: 200 sin cambiar nada.
- **FR-006**: La respuesta MUST seguir siendo `mapaPerfil(...)` (incluye `email`), que es lo que el cliente usa para detectar el cambio y decidir si renueva.
- **FR-007**: La sesión NO se renueva en el backend: `RefreshToken` guarda una relación `@ManyToOne Usuario`, no el email, por lo que `POST /auth/refresh` ya devuelve el email nuevo sin cambios adicionales.

### Key Entities
- **Usuario**: `email` sigue siendo `@Column(unique = true, nullable = false)`; el cambio de identidad es de columna, no de clave foránea (ninguna otra tabla referencia el email: `RefreshToken`, `Evento.creadoPor`, `Favorito` y `Recordatorio` apuntan por id).

## Success Criteria

### Measurable Outcomes
- **SC-001**: 5 tests nuevos en verde en `AuthAdminTest` (34 en total en ese fichero).
- **SC-002**: `./mvnw test` en verde (226/226).
- **SC-003**: Verificación en vivo contra backend corriendo: cambio de email, token viejo → 500, refresh → 200 con el email nuevo, login nuevo 200 / antiguo 401, demo 403.

## Assumptions
- **No se pide la contraseña actual.** En una demo lo que hace seguro un cambio de email es verificar el correo nuevo (enlace de confirmación), no reautenticar; pedir la contraseña añade fricción sin cubrir el caso de verdad. Queda anotado como futura mejora en el README.
- **No hay verificación del email nuevo.** Está fuera de alcance a propósito; en producción habría que enviar un enlace de confirmación antes de asumir la identidad nueva.
- El admin tampoco puede cambiar el email de terceros: `PUT /auth/usuarios/{id}` sigue sin aceptarlo (el rol/estado es lo único que toca).
