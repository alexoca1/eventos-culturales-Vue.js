# Feature Specification: Favoritos y Recordatorio por Email — Eventos Culturales Puertollano

**Feature Branch**: `008-favoritos-recordatorios`
**Created**: 2026-09-18
**Status**: Implemented (2026-09-22, 122/122 tests en verde; ver tasks.md)
**Input**: Usuarios logueados (ROLE_USER) marcan eventos como favoritos; los invitados sin login siguen viendo eventos sin restricción. El día anterior por la tarde/noche, se envía un email recordatorio de cada evento favorito a los usuarios que lo marcaron, vía Mailtrap.

## User Scenarios & Testing

### User Story 1 - Marcar/desmarcar un evento como favorito (Priority: P1)
Cualquier usuario autenticado puede marcar o desmarcar un evento `APROBADO` como favorito.

**Independent Test**: `POST /eventos/{id}/favorito` seguido de `GET /eventos/favoritos` → el evento aparece; `DELETE /eventos/{id}/favorito` → deja de aparecer.

**Acceptance Scenarios**:
1. **Given** usuario autenticado, **When** `POST /eventos/{id}/favorito` sobre un evento `APROBADO`, **Then** se crea el favorito (o no hace nada si ya existía — idempotente).
2. **Given** usuario autenticado, **When** `DELETE /eventos/{id}/favorito`, **Then** se elimina el favorito si existía.
3. **Given** usuario autenticado, **When** `POST /eventos/{id}/favorito` sobre un evento que no está `APROBADO` (pendiente, rechazado), **Then** 404 (no se expone su existencia a quien no debería verlo).
4. **Given** ningún usuario autenticado (invitado), **When** intenta marcar favorito, **Then** 401 — pero sigue pudiendo ver eventos con `GET /eventos` sin restricción (esto no cambia).

---

### User Story 2 - Ver mis favoritos (Priority: P1)
El usuario ve la lista de sus eventos favoritos.

**Independent Test**: `GET /eventos/favoritos` devuelve solo los eventos marcados por ese usuario, y solo si siguen `APROBADO`.

**Acceptance Scenarios**:
1. **Given** un usuario con 3 favoritos, **When** `GET /eventos/favoritos`, **Then** devuelve exactamente esos 3.
2. **Given** uno de esos eventos deja de estar `APROBADO` (ej. el organizador lo edita y vuelve a revisión), **When** `GET /eventos/favoritos`, **Then** ese evento ya no aparece en la lista (aunque el favorito sigue guardado en BD, por si vuelve a aprobarse).

---

### User Story 3 - Integridad al borrar un evento (Priority: P1)
Borrar un evento definitivamente (por el admin, directo o vía aprobación de una solicitud de eliminación) MUST limpiar sus favoritos asociados, sin romper la operación.

**Independent Test**: un evento con favoritos se borra (por cualquiera de las 2 vías de borrado existentes) → no hay error de integridad referencial, y los favoritos de ese evento desaparecen.

**Acceptance Scenarios**:
1. **Given** un evento `APROBADO` con favoritos, **When** un admin lo borra directamente (`DELETE /eventos/{id}`), **Then** se borra sin error y sus favoritos también.
2. **Given** un evento `PENDIENTE_ELIMINACION` con favoritos, **When** el admin aprueba la eliminación, **Then** se borra sin error y sus favoritos también.

---

### User Story 4 - Recordatorio automático por email (Priority: P1)
Cada tarde/noche, el sistema envía un recordatorio a quienes marcaron como favorito un evento que ocurre al día siguiente.

**Independent Test**: con un evento `fecha=mañana` y un usuario que lo tiene en favoritos, tras ejecutar el job programado, se llama a `EmailService.enviar(...)` con ese usuario y ese evento, y el favorito queda marcado como "recordatorio enviado".

**Acceptance Scenarios**:
1. **Given** un favorito de un evento con `fecha` = mañana y `recordatorioEnviado=false`, **When** se ejecuta el job (cron diario a las 20:00), **Then** se envía un email a ese usuario y el favorito pasa a `recordatorioEnviado=true`.
2. **Given** el mismo favorito ya con `recordatorioEnviado=true`, **When** el job se ejecuta de nuevo (ej. por un reinicio del servidor el mismo día), **Then** NO se reenvía el email (evita duplicados).
3. **Given** un favorito de un evento con `fecha` != mañana, **When** se ejecuta el job, **Then** no se procesa ese favorito.
4. **Given** el envío de email falla para un favorito concreto, **When** ocurre, **Then** el job MUST seguir procesando el resto de favoritos pendientes sin detenerse (un fallo aislado no debe bloquear los demás recordatorios del día).

---

### Edge Cases
- Un evento que cruza medianoche (`004-horario-eventos`) sigue usando `fecha` (día de inicio) para el recordatorio — se avisa el día antes de que *empiece*, no de que termine.
- Un usuario puede tener el mismo evento en favoritos, se le quite la aprobación, se vuelva a aprobar, y el recordatorio se sigue enviando correctamente si sigue siendo "mañana" cuando eso ocurre (no hay estado intermedio raro: `recordatorioEnviado` es independiente del estado del evento).
- Si el usuario desmarca el favorito antes de que se envíe el recordatorio, el favorito ya no existe y el job simplemente no lo encuentra — comportamiento correcto sin lógica adicional.

## Requirements

### Functional Requirements
- **FR-001**: El sistema MUST modelar `Favorito` como entidad propia (no `@ManyToMany` simple) porque necesita el atributo `recordatorioEnviado`.
- **FR-002**: `POST /eventos/{id}/favorito` y `DELETE /eventos/{id}/favorito` MUST requerir autenticación (cualquier rol), sin exigir un rol de negocio específico.
- **FR-003**: Marcar favorito sobre un evento que no está `APROBADO` MUST devolver 404.
- **FR-004**: `GET /eventos/favoritos` MUST devolver solo los eventos favoritos del usuario autenticado que sigan `APROBADO`.
- **FR-005**: Borrar un evento (por cualquiera de las 2 vías ya existentes de borrado definitivo) MUST borrar también sus `Favorito` asociados, sin error de integridad referencial.
- **FR-006**: Un job programado (cron diario, hora configurable vía `app.recordatorios.cron`, default 20:00) MUST buscar `Favorito` con `evento.fecha = hoy + 1 día`, `evento.estado = APROBADO` y `recordatorioEnviado = false`, enviar un email por cada uno, y marcar `recordatorioEnviado = true` tras el intento (haya tenido éxito o no).
- **FR-007**: Un fallo de envío de un recordatorio individual MUST NOT detener el procesamiento del resto de la cola de ese día.

### Key Entities
- **`Favorito`** (entidad nueva): `id`, `usuario` (`@ManyToOne`), `evento` (`@ManyToOne`), `fechaCreacion`, `recordatorioEnviado` (default `false`). Restricción única `(usuario_id, evento_id)` para que no se dupliquen favoritos del mismo usuario sobre el mismo evento.

## Success Criteria

### Measurable Outcomes
- **SC-001**: Marcar/desmarcar favorito y consultar "mis favoritos" funciona de punta a punta.
- **SC-002**: Borrar un evento con favoritos no produce ningún error.
- **SC-003**: Un favorito de un evento "de mañana" dispara exactamente un email, nunca cero ni dos, aunque el job se ejecute varias veces el mismo día.
- **SC-004**: `./mvnw test` en verde (suite `001` a `008`).

## Assumptions
- No hay reintento automático si el envío de un recordatorio falla ese día — al día siguiente `fecha` ya no será "mañana" para ese evento, así que ese recordatorio en concreto se pierde. Aceptable para el alcance de un portafolio; en un sistema real se añadiría una cola de reintentos.
- El job corre en la misma instancia del backend (sin necesidad de un scheduler externo) — coherente con el alcance actual (una sola instancia en Render).