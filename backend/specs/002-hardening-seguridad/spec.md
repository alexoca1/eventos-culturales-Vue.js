# Feature Specification: Hardening de Seguridad — Eventos Culturales Puertollano

**Feature Branch**: `002-hardening-seguridad`
**Created**: 2026-09-18
**Status**: Implemented (2026-09-18, 42/42 tests en verde; ver tasks.md)
**Input**: User description: "Corregir 3 hallazgos de seguridad detectados en revisión de código: (1) JWT_SECRET con fallback inseguro en application.properties que permite arrancar en producción sin variable de entorno real; (2) GlobalExceptionHandler expone ex.toString() al cliente en errores 500; (3) validación de mapaEmbed insuficiente permite HTML/JS arbitrario persistido y renderizado con v-html en la vista pública de invitados."

## User Scenarios & Testing

### User Story 1 - JWT_SECRET obligatorio sin fallback inseguro (Priority: P1)
La aplicación MUST NOT arrancar con una clave de firma JWT conocida/pública. Si `JWT_SECRET` no está definido, la app falla al arrancar con un mensaje claro, tanto en local como en producción.

**Why this priority**: Un JWT_SECRET filtrado en el repo permite falsificar tokens de administrador; es el hallazgo de mayor impacto.

**Independent Test**: Arrancar la app sin `JWT_SECRET` en el entorno → falla al arrancar (`IllegalStateException`) en vez de arrancar con la clave de repo.

**Acceptance Scenarios**:
1. **Given** no hay variable `JWT_SECRET` en el entorno, **When** la app arranca, **Then** falla con `IllegalStateException` explicando cómo generar una clave (`openssl rand -base64 32`).
2. **Given** `JWT_SECRET` está definido y es válido (≥32 bytes Base64), **When** la app arranca, **Then** arranca normalmente y firma/valida tokens con esa clave.

---

### User Story 2 - Errores 500 no filtran detalles internos (Priority: P1)
Ante una excepción no controlada, el cliente MUST recibir un mensaje genérico; el detalle completo (stack trace / `ex.toString()`) MUST quedar solo en el log del servidor.

**Why this priority**: Evita exponer nombres de clase, tabla, columna o mensajes internos a quien inspeccione la respuesta HTTP.

**Independent Test**: Forzar una excepción no controlada → la respuesta 500 no contiene el mensaje ni el tipo de la excepción original; el log del servidor sí lo registra.

**Acceptance Scenarios**:
1. **Given** ocurre una excepción no controlada, **When** el cliente recibe la respuesta, **Then** el body es un mensaje genérico fijo (`{"error": "Error interno del servidor"}`), no `ex.toString()`.
2. **Given** la misma excepción, **When** se revisa el log del servidor, **Then** el stack trace completo está disponible para depuración.

---

### User Story 3 - `mapaEmbed` validado estrictamente (Priority: P1)
El sistema MUST rechazar cualquier valor de `mapaEmbed` que no sea un iframe de Google Maps bien formado, para evitar XSS persistente en la vista pública.

**Why this priority**: Es un XSS almacenado que afecta a cualquier invitado que visite la web, no solo al admin que lo introduce.

**Independent Test**: Enviar un `mapaEmbed` con HTML/JS adicional alrededor de la substring `google.com/maps/embed` → 400. Enviar un iframe válido de Google Maps → 200/201.

**Acceptance Scenarios**:
1. **Given** admin autenticado, **When** `POST/PUT /eventos` con un iframe válido de Google Maps, **Then** se guarda y se sirve igual.
2. **Given** admin autenticado, **When** `mapaEmbed` contiene cualquier tag distinto de `<iframe>` o atributos fuera de la lista permitida (`src`, `width`, `height`, `style`, `allowfullscreen`, `loading`, `referrerpolicy`), **Then** 400 con mensaje claro.

---

### Edge Cases
- `mapaEmbed` vacío/nulo sigue siendo opcional, no rompe la creación del evento.
- `JWT_SECRET` presente pero corta o no-Base64 sigue fallando igual que hoy (ya cubierto).
- Los handlers ya existentes (validación 400, multipart 400, acceso denegado 403, tipo de parámetro 400) MUST seguir devolviendo su mensaje específico — el cambio de US2 afecta solo al handler genérico `Exception.class`.

## Requirements

### Functional Requirements
- **FR-001**: El sistema MUST NOT tener ningún valor de fallback para `jwt.secret` en `application.properties`; la propiedad MUST leerse solo de `${JWT_SECRET}` sin default.
- **FR-002**: El handler genérico de excepciones (`Exception.class`) MUST devolver siempre un mensaje fijo y genérico al cliente, y MUST seguir registrando el detalle completo en el log del servidor.
- **FR-003**: El sistema MUST validar `mapaEmbed` con un patrón que acepte únicamente un `<iframe>` de Google Maps con atributos de una lista permitida, rechazando cualquier otro contenido con 400.
- **FR-004**: Los handlers de excepción ya existentes (validación, multipart, acceso denegado, tipo de parámetro) MUST mantener su comportamiento actual sin cambios.

### Key Entities
- **Evento.mapaEmbed**: sin cambio de tipo (String), cambia solo la validación en `EventoDTO`/`EventoController`.

## Success Criteria

### Measurable Outcomes
- **SC-001**: La app no arranca sin `JWT_SECRET` en el entorno (ni en local ni en producción).
- **SC-002**: Ninguna respuesta 500 contiene el nombre de clase ni el mensaje de la excepción Java original.
- **SC-003**: `mapaEmbed` con contenido fuera del patrón permitido es rechazado con 400 en el 100% de los casos probados.
- **SC-004**: `./mvnw test` en verde incluyendo los tests nuevos de esta feature.

## Assumptions
- Los eventos ya guardados en BD con `mapaEmbed` antiguo (validación débil) no se migran automáticamente en este spec; si alguno tuviera contenido malicioso habría que revisarlo a mano una vez (alcance de datos, no de código).
- A partir de ahora `JWT_SECRET` es obligatoria también en local, no solo en producción — coherente con lo que ya recomendaba `quickstart.md`.