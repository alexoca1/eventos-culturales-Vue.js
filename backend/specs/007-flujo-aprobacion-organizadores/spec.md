# Feature Specification: Flujo de Aprobación de Organizadores — Eventos Culturales Puertollano

**Feature Branch**: `007-flujo-aprobacion-organizadores`
**Created**: 2026-09-18
**Status**: Implemented (2026-09-22, 97/97 tests en verde; ver tasks.md)
**Input**: Nuevo rol ROLE_ORGANIZADOR que crea/edita/elimina sus propios eventos, pero toda creación, edición y eliminación requiere aprobación de un admin antes de tener efecto público. Los eventos deben crearse con 48h de antelación mínima. El admin puede añadir un motivo al rechazar. El organizador se notifica por email y en su panel.

## User Scenarios & Testing

### User Story 1 - Organizador crea un evento (Priority: P1)
Un usuario con `ROLE_ORGANIZADOR` crea un evento que queda `PENDIENTE_REVISION`, invisible al público, hasta que un admin lo apruebe. El evento debe programarse con al menos 48h de antelación.

**Independent Test**: Organizador crea un evento para dentro de 72h → queda `PENDIENTE_REVISION`, no aparece en `GET /eventos` público. Crea uno para dentro de 10h → 400.

**Acceptance Scenarios**:
1. **Given** organizador autenticado, **When** crea un evento con `fecha`/`horaInicio` a más de 48h de ahora, **Then** se guarda con `estado=PENDIENTE_REVISION` y `creadoPor` = el propio organizador.
2. **Given** organizador autenticado, **When** crea un evento con `fecha`/`horaInicio` a menos de 48h de ahora, **Then** 400, rechazado antes de guardar.
3. **Given** el mismo evento pendiente, **When** un invitado consulta `GET /eventos` (sin login), **Then** el evento NO aparece.
4. **Given** admin autenticado, **When** crea un evento (con el flujo actual, sin cambios), **Then** se guarda directamente con `estado=APROBADO`, sin pasar por revisión ni por la regla de 48h.

---

### User Story 2 - Admin revisa y aprueba/rechaza creaciones (Priority: P1)
Un admin ve una cola de eventos pendientes y decide aprobar o rechazar cada uno, con motivo opcional en el rechazo.

**Independent Test**: `GET /eventos/pendientes` devuelve el evento de la Historia 1; `POST /eventos/{id}/aprobar` lo pasa a `APROBADO` (ya visible al público); `POST /eventos/{id}/rechazar` con motivo lo pasa a `RECHAZADO` con ese motivo guardado.

**Acceptance Scenarios**:
1. **Given** hay eventos `PENDIENTE_REVISION`, **When** admin autenticado consulta `GET /eventos/pendientes`, **Then** los ve todos, de cualquier organizador.
2. **Given** un evento `PENDIENTE_REVISION`, **When** admin hace `POST /eventos/{id}/aprobar`, **Then** pasa a `APROBADO` y ya aparece en `GET /eventos` público.
3. **Given** un evento `PENDIENTE_REVISION`, **When** admin hace `POST /eventos/{id}/rechazar` con `{"motivo": "Falta información de contacto"}`, **Then** pasa a `RECHAZADO` con ese motivo guardado.
4. **Given** un usuario `ROLE_ORGANIZADOR` (no admin), **When** intenta llamar a `/aprobar` o `/rechazar`, **Then** 403.

---

### User Story 3 - Organizador edita su propio evento (Priority: P1)
Editar un evento ya `APROBADO` lo devuelve a `PENDIENTE_REVISION` (oculto del público) hasta que el admin apruebe el cambio.

**Independent Test**: Organizador edita un evento propio `APROBADO` → pasa a `PENDIENTE_REVISION` y desaparece de `GET /eventos` público hasta nueva aprobación.

**Acceptance Scenarios**:
1. **Given** organizador dueño de un evento `APROBADO`, **When** lo edita, **Then** pasa a `PENDIENTE_REVISION`.
2. **Given** organizador dueño de un evento `RECHAZADO`, **When** lo edita (para corregirlo), **Then** pasa a `PENDIENTE_REVISION` (reenvío a revisión).
3. **Given** organizador NO dueño del evento (creado por otro organizador), **When** intenta editarlo, **Then** 403.
4. **Given** admin, **When** edita cualquier evento (propio o de un organizador), **Then** se aplica inmediatamente, queda `APROBADO`, sin pasar por revisión.

---

### User Story 4 - Organizador solicita eliminar su propio evento (Priority: P1)
Eliminar no borra directamente — pasa a `PENDIENTE_ELIMINACION`, requiere aprobación del admin.

**Independent Test**: Organizador pide eliminar su evento `APROBADO` → pasa a `PENDIENTE_ELIMINACION` (desaparece del público, pero sigue en BD); admin aprueba → se borra de verdad; admin rechaza → vuelve a `APROBADO`.

**Acceptance Scenarios**:
1. **Given** organizador dueño de un evento `APROBADO`, **When** hace `DELETE /eventos/{id}`, **Then** el evento pasa a `PENDIENTE_ELIMINACION` (no se borra todavía) y desaparece del público.
2. **Given** un evento `PENDIENTE_ELIMINACION`, **When** admin aprueba, **Then** el evento se borra definitivamente de la BD.
3. **Given** un evento `PENDIENTE_ELIMINACION`, **When** admin rechaza, **Then** vuelve a `APROBADO` y reaparece al público.
4. **Given** admin, **When** hace `DELETE /eventos/{id}` de cualquier evento, **Then** se borra directamente, sin pasar por este flujo (comportamiento actual, sin cambios).

---

### User Story 5 - El público solo ve eventos aprobados (Priority: P1)
`GET /eventos` sin autenticación (o con un rol distinto de admin) nunca devuelve eventos `PENDIENTE_REVISION`, `RECHAZADO` o `PENDIENTE_ELIMINACION`.

**Independent Test**: con eventos en los 4 estados en BD, `GET /eventos` sin login solo devuelve los `APROBADO`; con login admin, devuelve los 4.

**Acceptance Scenarios**:
1. **Given** eventos en los 4 estados, **When** `GET /eventos` sin autenticación, **Then** solo se devuelven los `APROBADO` (combinando también con los filtros de `fecha`/`categoria` ya existentes).
2. **Given** el mismo dataset, **When** `GET /eventos` con admin autenticado, **Then** se devuelven los 4 estados (comportamiento actual del panel de gestión, sin cambios).
3. **Given** eventos ya sembrados antes de esta feature, **When** se aplica esta feature, **Then** todos quedan `APROBADO` por defecto (no deben desaparecer del sitio).

---

### User Story 6 - Notificación al organizador (Priority: P2)
Al aprobar o rechazar (creación, edición o eliminación), el organizador recibe un email y puede verlo reflejado en su panel (`GET /eventos/mios`).

**Independent Test**: al aprobar/rechazar un evento, se dispara un email al organizador dueño vía Mailtrap; `GET /eventos/mios` siempre refleja el estado real sin necesidad de email (esto ya es gratis, viene de BD).

**Acceptance Scenarios**:
1. **Given** un evento pasa de `PENDIENTE_REVISION` a `APROBADO`, **When** ocurre, **Then** se envía un email al organizador con el resultado.
2. **Given** un evento se rechaza con motivo, **When** ocurre, **Then** el email incluye el motivo si se informó.
3. **Given** el envío de email falla (ej. Mailtrap caído), **When** ocurre, **Then** la aprobación/rechazo en BD MUST NOT revertirse — el email es un efecto secundario, no debe bloquear la operación principal.

---

### Edge Cases
- Un organizador no puede ver ni actuar sobre eventos de otro organizador, ni siquiera en modo lectura vía `/eventos/mios` (solo ve los suyos).
- `GET /eventos/pendientes` incluye tanto `PENDIENTE_REVISION` como `PENDIENTE_ELIMINACION`, distinguibles por el campo `estado` para que el admin sepa qué acción está aprobando.
- Si `horaInicio` no está informado en un evento nuevo, la regla de 48h se calcula contra `fecha` a las 00:00.
- El fallo de envío de email (US6, escenario 3) se registra en log pero no debe romper el flujo de aprobación/rechazo.

## Requirements

### Functional Requirements
- **FR-001**: El sistema MUST soportar un nuevo rol `ROLE_ORGANIZADOR`.
- **FR-002**: `Evento` MUST tener `estado` (`EstadoEvento`: `PENDIENTE_REVISION`, `APROBADO`, `RECHAZADO`, `PENDIENTE_ELIMINACION`) y `creadoPor` (referencia al `Usuario` que lo creó).
- **FR-003**: Crear un evento como `ROLE_ORGANIZADOR` MUST validar que `fecha`+`horaInicio` (o `fecha` a las 00:00 si no hay hora) sea ≥ ahora + 48h, devolviendo 400 si no se cumple.
- **FR-004**: Crear/editar un evento como `ROLE_ADMIN` MUST seguir aplicándose inmediatamente como `APROBADO`, sin la validación de 48h ni el flujo de revisión.
- **FR-005**: Editar un evento propio como `ROLE_ORGANIZADOR` MUST devolverlo a `PENDIENTE_REVISION`, cualquiera que fuera su estado anterior (excepto si ya estaba `PENDIENTE_ELIMINACION`, en cuyo caso MUST rechazarse con 409 — no se puede editar algo pendiente de borrar).
- **FR-006**: Eliminar un evento propio como `ROLE_ORGANIZADOR` MUST cambiarlo a `PENDIENTE_ELIMINACION` en vez de borrarlo.
- **FR-007**: `GET /eventos/pendientes` (solo `ROLE_ADMIN`) MUST devolver los eventos en `PENDIENTE_REVISION` o `PENDIENTE_ELIMINACION` de cualquier organizador.
- **FR-008**: `POST /eventos/{id}/aprobar` (solo `ROLE_ADMIN`) MUST: si estaba `PENDIENTE_REVISION` → `APROBADO`; si estaba `PENDIENTE_ELIMINACION` → borrado definitivo.
- **FR-009**: `POST /eventos/{id}/rechazar` (solo `ROLE_ADMIN`, body opcional `{motivo}`) MUST: si estaba `PENDIENTE_REVISION` → `RECHAZADO` (guardando el motivo); si estaba `PENDIENTE_ELIMINACION` → vuelve a `APROBADO` (se cancela la solicitud de borrado).
- **FR-010**: `GET /eventos/mios` (solo `ROLE_ORGANIZADOR`) MUST devolver únicamente los eventos donde `creadoPor` sea el usuario autenticado, en cualquier estado.
- **FR-011**: `GET /eventos` sin autenticación (o con rol distinto de `ROLE_ADMIN`) MUST filtrar siempre por `estado=APROBADO`, combinándose con los filtros de `fecha`/`categoria` ya existentes.
- **FR-012**: Un `ROLE_ORGANIZADOR` MUST recibir 403 al intentar editar/eliminar/ver-en-detalle-de-gestión un evento que no sea suyo.
- **FR-013**: El sistema MUST enviar un email al organizador dueño cuando su evento cambie a `APROBADO` o `RECHAZADO`. Un fallo de envío MUST NOT revertir el cambio de estado ya persistido.

### Key Entities
- **`EstadoEvento`** (enum nuevo): `PENDIENTE_REVISION, APROBADO, RECHAZADO, PENDIENTE_ELIMINACION`.
- **Evento**: + `estado` (`EstadoEvento`, default `APROBADO` para compatibilidad con eventos existentes), + `creadoPor` (`@ManyToOne Usuario`, nullable para eventos legacy), + `motivoRechazo` (`String`, nullable).
- **Usuario.roles**: se añade el valor posible `ROLE_ORGANIZADOR` (sin cambio de tipo, ya es `Set<String>` desde `003-robustez-auth-cors`).

## Success Criteria

### Measurable Outcomes
- **SC-001**: Los 4 escenarios de cada una de las 6 historias de usuario pasan en test automatizado.
- **SC-002**: Un evento sembrado antes de esta feature sigue apareciendo en `GET /eventos` público después de aplicarla (migración a `APROBADO` por defecto).
- **SC-003**: `./mvnw test` en verde (suite `001` a `007`).

## Assumptions
- El admin queda exento tanto de la regla de 48h como del flujo de aprobación — es quien modera, no quien es moderado.
- Un organizador no puede tener eventos con más de un `PENDIENTE_ELIMINACION`/`PENDIENTE_REVISION` "en cola" de forma especial — el estado es 1:1 con el evento, no hay historial de versiones.
- No se implementa en esta feature un límite al número de eventos pendientes que un organizador puede tener abiertos a la vez — se deja para una iteración futura si se detecta abuso.