# Feature Specification: Categorías de Eventos — Eventos Culturales Puertollano

**Feature Branch**: `005-categorias-eventos`
**Created**: 2026-09-18
**Status**: Implemented (2026-09-19, 66/66 tests en verde; ver tasks.md)
**Input**: Categoría única por evento (enum fijo) + filtro combinable con fecha en GET /eventos.

## User Scenarios & Testing

### User Story 1 - Asignar categoría a un evento (Priority: P1)
El admin, al crear o editar un evento, puede indicar una categoría de una lista fija.

**Independent Test**: Crear un evento con `categoria=MUSICA` → se guarda y se recupera igual.

**Acceptance Scenarios**:
1. **Given** admin autenticado, **When** crea un evento sin categoría, **Then** se guarda con `categoria=null` (compatible con eventos existentes).
2. **Given** admin autenticado, **When** crea un evento con `categoria=TEATRO`, **Then** se guarda y se recupera igual.
3. **Given** admin autenticado, **When** envía un valor de categoría que no existe en la lista fija, **Then** 400.

---

### User Story 2 - Filtrar eventos por categoría (Priority: P1)
Cualquiera (invitado o admin) puede filtrar `GET /eventos` por categoría, sola o combinada con fecha.

**Independent Test**: `GET /eventos?categoria=MUSICA` devuelve solo eventos de música; `GET /eventos?fecha=X&categoria=Y` devuelve solo los que cumplen ambas condiciones.

**Acceptance Scenarios**:
1. **Given** hay eventos de varias categorías, **When** `GET /eventos?categoria=TEATRO`, **Then** solo se devuelven los de teatro, en cualquier fecha.
2. **Given** hay eventos de varias categorías y fechas, **When** `GET /eventos?fecha=2026-10-10&categoria=CINE`, **Then** solo se devuelven los que cumplen ambos filtros.
3. **Given** no se informa ni fecha ni categoría, **When** `GET /eventos`, **Then** comportamiento actual sin cambios (todos los eventos).
4. **Given** se informa solo fecha (sin categoría), **When** `GET /eventos?fecha=X`, **Then** comportamiento actual sin cambios.

---

### Edge Cases
- Categoría vacía en el filtro (`categoria=` sin valor) MUST tratarse igual que no informarla.
- Lista fija de categorías: `MUSICA, TEATRO, EXPOSICION, CINE, LITERATURA, INFANTIL, OTROS`.

## Requirements

### Functional Requirements
- **FR-001**: `Evento` MUST tener un campo `categoria` (enum `CategoriaEvento`, nullable, valores: `MUSICA, TEATRO, EXPOSICION, CINE, LITERATURA, INFANTIL, OTROS`).
- **FR-002**: `POST`/`PUT /eventos` MUST rechazar con 400 cualquier valor de `categoria` fuera de la lista fija (comportamiento estándar de Spring al deserializar un enum inválido, sin código de validación manual adicional).
- **FR-003**: `GET /eventos` MUST aceptar un parámetro opcional `categoria`, combinable con el parámetro `fecha` ya existente, sin cambiar el comportamiento cuando ninguno de los dos se informa.
- **FR-004**: Eventos sin categoría informada MUST seguir funcionando exactamente igual que antes de esta feature.

### Key Entities
- **Evento**: + `categoria` (`CategoriaEvento`, nullable).
- **`CategoriaEvento`** (enum nuevo): `MUSICA, TEATRO, EXPOSICION, CINE, LITERATURA, INFANTIL, OTROS`.

## Success Criteria

### Measurable Outcomes
- **SC-001**: Crear un evento con cada uno de los 7 valores de categoría y recuperarlo devuelve el mismo valor.
- **SC-002**: Un valor de categoría inválido devuelve 400.
- **SC-003**: Los 4 escenarios de combinación fecha/categoría de la Historia 2 devuelven exactamente los eventos esperados.
- **SC-004**: `./mvnw test` en verde (suite `001` a `005`).

## Assumptions
- Categoría única por evento (no etiquetas múltiples), según lo decidido.
- Los eventos semilla de `DataInitializer` se actualizan con una categoría cada uno (mejora de la demo, no es requisito funcional estricto, pero se incluye como tarea de pulido).