# Feature Specification: Editar email propio — Frontend

**Feature Branch**: `023-editar-email-perfil`
**Created**: 2026-10-05
**Status**: Implemented (2026-10-05, ver tasks.md)
**Input**: Consumir el `email` que añade `PUT /auth/perfil` (backend `023-editar-email-perfil`) en la sección "Mi perfil" de las 3 páginas, y renovar la sesión en silencio cuando cambia, porque el `sub` del JWT es el email.

## User Scenarios & Testing

### User Story 1 - Cambiar el correo desde "Mi perfil" (Priority: P1)
El formulario "Mi perfil" muestra el correo y lo envía en el guardado, igual que el resto de campos.

**Independent Test**: Abrir "Mi perfil", cambiar el correo, guardar → aviso "Guardado.", la cabecera enseña el correo nuevo y la sesión sigue viva.

**Acceptance Scenarios**:
1. **Given** cualquier página con sesión, **When** abre "Mi perfil", **Then** el campo `Correo electrónico` está precargado con su email.
2. **Given** guarda con el correo cambiado, **When** el backend responde 200, **Then** aviso "Guardado." y la sesión se renueva sola sin salir de la página.
3. **Given** guarda con un correo ya usado por otra cuenta, **When** el backend responde 409, **Then** se muestra "Ese correo ya está en uso" en rojo, sin cerrar el perfil.
4. **Given** guarda con formato inválido, **When** el backend responde 400, **Then** se muestra "El correo debe ser válido" (el mensaje llega del campo `email`, no del genérico).
5. **Given** guarda sin tocar el correo, **When** ocurre, **Then** 200 y **no** se gasta la cookie de refresh (sin petición a `/auth/refresh`).
6. **Given** hay espacios alrededor al escribir, **When** el input se sale, **Then** el valor se envía recortado (`v-model.trim`), porque `@Email` rechazaría " correo@x.com".
7. **Given** la cuenta demo intenta cambiar el correo, **When** el backend responde 403, **Then** se muestra el mensaje del backend en rojo dentro del formulario: sin alerta de "Sesión caducada", sin borrar el token y sin salir de la página.

---

### User Story 2 - La sesión sobrevive al cambio (Priority: P1)
Tras cambiar el correo, el usuario sigue trabajando: el token en vigor es sustituido por uno nuevo cuyo `sub` es el correo nuevo.

**Acceptance Scenarios**:
1. **Given** el correo ha cambiado, **When** termina el guardado, **Then** se llama a `POST /auth/refresh` con la cookie y el token nuevo sustituye al viejo en `sessionStorage`.
2. **Given** el token nuevo está guardado, **When** se consulta `GET /auth/perfil`, **Then** la cabecera (`sesionNombre`) muestra el correo nuevo.
3. **Given** la renovación falla (cookie caducada o backend caído), **When** ocurre, **Then** `renovarSesion()` limpia la sesión como ya hacía en cualquier otro caso y el usuario vuelve al login en lugar de quedarse con un token que da 500.
4. **Given** la sesión se recarga en otra pestaña, **When** ocurre, **Then** no hay residuos del correo antiguo (el `store.miEmail` se actualiza en la renovación).

---

### User Story 3 - Aviso en el propio formulario (Priority: P2)
El label explica por qué ese campo está ahí: es con lo que se entra.

**Acceptance Scenarios**:
1. **Given** el formulario abierto, **When** se lee el label, **Then** pone "Correo electrónico (obligatorio, con este inicias sesión)".

## Requirements

### Functional Requirements
- **FR-001**: `usePerfil` MUST gestionar `perfilEmail` (lo que se edita) y `emailGuardado` (lo que hay en la BD, rellenado en `rellenar`), y exponer ambos.
- **FR-002**: `guardarPerfil` MUST enviar `email` en el body del `PUT` **y** comparar `r.perfil.email` con `emailGuardado`: si cambió, actualizar `emailGuardado` y `await useAuth().renovarSesion()`; si no cambió, no renovar.
- **FR-003**: `useAuth` MUST exponer `renovarSesion` en su instancia (el flujo interno refresh → `setSession` → `GET /auth/perfil` ya existía).
- **FR-004**: Las 3 páginas MUST tener el input `perfil-email` (`type="email"`, `v-model.trim`, label asociado por `for`/`id`), después de apellidos y antes del bloque de organización.
- **FR-005**: `api/auth.js` → `actualizarPerfil` MUST traducir el error con `mensajeValidacion` (acepta `{error}` y `{email}`), no solo `r.data.error`.
- **FR-006**: `actualizarPerfil` MUST llamarse con `authFail: "return"`. Con el `"redirect"` por defecto, `http.js` trataba el 403 de la cuenta demo como sesión caducada (alerta + borra el token + salto al login) y el perfil echaba al usuario sin explicar nada. Como así se pierde la redirección automática del 401, `guardarPerfil` MUST cerrar sesión con `useAuth().logout()` cuando `r.status === 401`.

### Key Entities
- **`perfil.check.js`**: check ejecutable con `node` que cubre el ciclo de imports `usePerfil` ↔ `useAuth`, que sin cambio de email no hay refresh, que con cambio sí lo hay con el token nuevo en `store.token` y la cabecera actualizada, que el `email` viaja en el body del `PUT`, y los dos caminos de error: el 403 de la demo (mensaje al campo,0 alertas, token intacto) y el 401 (cierre de sesión).

## Success Criteria

### Measurable Outcomes
- **SC-001**: `node js/ui/perfil.check.js` en verde (y los 4 checks anteriores siguen en verde).
- **SC-002**: `node --check` sin errores sobre los 30 ficheros de `js/`.
- **SC-003**: Guardar un correo nuevo en cualquier página no saca al usuario de la pantalla ni dispara "Sesión caducada".
- **SC-004**: En Chrome (2026-10-05) el recorrido completo pasa: precarga del campo, 409 y 400 en rojo, cambio real con "Guardado." + token renovado + recarga con la sesión viva, 403 de la demo en rojo sin desloguear, y restauración del correo original.

## Assumptions
- No se pide la contraseña actual ni se verifica el correo nuevo (limitación documentada como futura mejora en el README).
- El `v-model.trim` evita el 400 por espacios; el backend hace `trim()` igualmente por si algo lo llama sin ese trim.
- La llamada a `renovarSesion` se espera (`await`) dentro de `guardarPerfil`: si el refresh falla, la sesión se limpia como ya hacía en cualquier otro camino.
