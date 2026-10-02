# Feature Specification: Etiquetas de Eventos — Eventos Culturales Puertollano

**Feature Branch**: `005-categorias-eventos`
**Created**: 2026-09-18
**Status**: Implemented (2026-09-24, 141/141 tests en verde; ver tasks.md)
**Input**: Múltiples etiquetas por evento (tabla `etiquetas` gestionada por el admin) + filtro ANY combinable con fecha en GET /eventos. Reemplaza la decisión anterior de categoría única (punto 2 de la lista de mejoras).

## User Scenarios & Testing

### User Story 1 - Asignar etiquetas a un evento (Priority: P1)
El admin (u organizador), al crear o editar un evento, puede indicar varias etiquetas de las existentes.

**Independent Test**: Crear un evento con `etiquetas=["MUSICA","TEATRO"]` → se guarda y se recupera igual.

**Acceptance Scenarios**:
1. **Given** admin autenticado, **When** crea un evento sin etiquetas, **Then** se guarda con `["OTROS"]` por defecto.
2. **Given** admin autenticado, **When** crea un evento con `etiquetas=["MUSICA","TEATRO"]`, **Then** se guardan y se recuperan igual.
3. **Given** admin autenticado, **When** envía un nombre de etiqueta que no existe, **Then** 400.

---

### User Story 2 - Filtrar eventos por etiquetas (Priority: P1)
Cualquiera (invitado o admin) puede filtrar `GET /eventos` por etiquetas (coincidencia ANY), solas o combinadas con fecha.

**Independent Test**: `GET /eventos?etiquetas=MUSICA,TEATRO` devuelve los que llevan alguna; `GET /eventos?fecha=X&etiquetas=Y` devuelve solo los que cumplen ambas condiciones.

**Acceptance Scenarios**:
1. **Given** hay eventos con varias etiquetas, **When** `GET /eventos?etiquetas=TEATRO,MUSICA`, **Then** se devuelven los que llevan alguna (sin duplicados), en cualquier fecha.
2. **Given** hay eventos con etiquetas y fechas, **When** `GET /eventos?fecha=2026-10-10&etiquetas=CINE`, **Then** solo se devuelven los que cumplen ambos filtros.
3. **Given** no se informa ni fecha ni etiquetas, **When** `GET /eventos`, **Then** comportamiento actual sin cambios (todos los eventos).
4. **Given** se informa una etiqueta desconocida, **When** `GET /eventos?etiquetas=ROCK`, **Then** `[]` (el filtro no es validante, solo filtra).

---

### User Story 3 - Admin gestiona el catálogo de etiquetas (Priority: P1)
El admin puede listar, crear, renombrar y eliminar etiquetas.

**Independent Test**: `POST /etiquetas {"nombre":"JAZZ"}` → 201; `DELETE` de una etiqueta en uso → 409.

**Acceptance Scenarios**:
1. **Given** cualquiera (con o sin login), **When** `GET /etiquetas`, **Then** catálogo ordenado por nombre.
2. **Given** admin autenticado, **When** `POST /etiquetas` con nombre nuevo, **Then** 201; con nombre vacío → 400; duplicado → 409.
3. **Given** admin autenticado, **When** `PUT /etiquetas/{id}` con otro nombre existente, **Then** 409; id inexistente → 404.
4. **Given** admin autenticado, **When** `DELETE /etiquetas/{id}` de una etiqueta usada por eventos, **Then** 409 sin borrar; sin uso → 204.
5. **Given** un no-admin, **When** intenta mutar el catálogo, **Then** 401/403.

---

### Edge Cases
- `?etiquetas=` vacío MUST tratarse igual que no informarlo.
- La migración de la antigua `categoria` única es automática al arrancar (backfill): cada evento existente conserva su categoría como única etiqueta (null → `OTROS`).
- Semilla inicial del catálogo: `MUSICA, TEATRO, EXPOSICION, CINE, LITERATURA, INFANTIL, OTROS`.

## Requirements

### Functional Requirements
- **FR-001**: `Evento` MUST tener `etiquetas` (`@ManyToMany` a `Etiqueta`, EAGER) en vez del campo `categoria` (eliminado con ruptura limpia: sin enum, sin `?categoria=`).
- **FR-002**: `POST`/`PUT /eventos` MUST aceptar `etiquetas` (lista de nombres): ausente/vacía → `["OTROS"]`; nombre desconocido → 400.
- **FR-003**: `GET /eventos` MUST aceptar `etiquetas` (coma-separadas, ANY, DISTINCT), combinable con `fecha` y con el filtro de estado de 007, sin cambiar el comportamiento cuando no se informa.
- **FR-004**: `GET /etiquetas` MUST ser público; `POST`/`PUT`/`DELETE /etiquetas/{id}` MUST exigir `ROLE_ADMIN` (`@PreAuthorize` explícito + cadena 2; `GET` en la cadena 1).
- **FR-005**: `DELETE /etiquetas/{id}` MUST devolver 409 si algún evento la usa; duplicados en `POST`/`PUT` → 409; `PUT`/`DELETE` sobre id inexistente → 404.

### Key Entities
- **Etiqueta** (nueva): `id`, `nombre` (único). Sin referencia inversa (evita recursión JSON).
- **Evento**: + `etiquetas` (`Set<Etiqueta>`, join table `evento_etiquetas`); − `categoria`.

## Success Criteria

### Measurable Outcomes
- **SC-001**: Crear un evento con varias etiquetas y recuperarlo devuelve las mismas.
- **SC-002**: Sin etiquetas → `["OTROS"]`; nombre desconocido → 400.
- **SC-003**: Los escenarios fecha/etiquetas devuelven exactamente los eventos esperados (ANY, sin duplicados).
- **SC-004**: CRUD de etiquetas con sus 409/404/401/403.
- **SC-005**: `./mvnw test` en verde (suite completa).

## Assumptions
- Ruptura limpia decidida (punto 2): no hay compatibilidad con `categoria`/`?categoria=`; el frontend se actualiza a la vez.
- Sin borrado en cascada de etiquetas en uso (409 a propósito, coherente con no borrar usuarios con eventos).
