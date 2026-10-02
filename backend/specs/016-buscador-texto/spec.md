# Feature Specification: Buscador de eventos por texto

**Feature Branch**: `016-buscador-texto`
**Created**: 2026-09-25
**Status**: Implemented (2026-10-02)
**Input**: Buscar eventos por nombre del evento o por nombre del establecimiento.
Accesible para cualquiera (con o sin login). Independiente de los filtros de
fecha, etiquetas y futuros (se excluyen mutuamente).

## User Scenarios & Testing

### User Story 1 - Búsqueda pública por texto (Priority: P1)
`GET /eventos?q=texto` devuelve los eventos APROBADO cuyo `nombre` o
`establecimiento` contiene el texto (parcial, sin distinguir mayúsculas).
Paginado de 10 en 10.

**Acceptance Scenarios**:
1. **Given** eventos con nombre "Concierto de Jazz" y establecimiento
   "Teatro Municipal", **When** `GET /eventos?q=jazz`, **Then** devuelve
   el primero.
2. **Given** `GET /eventos?q=teatro`, **Then** devuelve el evento cuyo
   establecimiento es "Teatro Municipal".
3. **Given** `GET /eventos?q=` (vacío) o sin `?q=`, **Then** se ignora
   el parámetro y el comportamiento es el actual sin cambios.
4. **Given** `GET /eventos?q=texto&futuros=true` o `?q=texto&fecha=X`,
   **Then** 400 — q es excluyente con fecha y futuros.
5. **Given** `GET /eventos?q=texto` como admin autenticado, **Then**
   devuelve eventos en cualquier estado (no solo APROBADO), paginado.
6. **Given** `?q=%` o `?q=_`, **Then** no explota — los caracteres de
   LIKE se escapan.

---

### Edge Cases
- Mínimo 2 caracteres, máximo 100. Fuera de ese rango → 400.
- `q` es excluyente con `fecha` y `futuros`. Sí es combinable con
  `etiquetas` (ver Assumptions).

## Requirements

### Functional Requirements
- **FR-001**: `GET /eventos?q=texto` (2-100 chars) MUST buscar por
  `nombre` LIKE `%texto%` OR `establecimiento` LIKE `%texto%`, case-insensitive.
- **FR-002**: El resultado MUST estar paginado (mismo `pageSize` que el
  resto del endpoint).
- **FR-003**: `q` vacío o ausente MUST ignorarse (comportamiento actual
  sin cambios).
- **FR-004**: `q` + `fecha` o `q` + `futuros=true` MUST devolver 400.
- **FR-005**: Los caracteres `%` y `_` en `q` MUST escaparse antes de
  usarlos en el LIKE.
- **FR-006**: Longitud de `q` fuera de [2, 100] MUST devolver 400.
- **FR-007**: Admin autenticado ve todos los estados; público solo APROBADO.

## Success Criteria
- **SC-001**: `GET /eventos?q=jazz` devuelve eventos con "jazz" en nombre
  o establecimiento.
- **SC-002**: `GET /eventos?q=%` no lanza error de BD.
- **SC-003**: `./mvnw test` en verde.

## Assumptions
- `q` + `etiquetas` se deja para una iteración futura si se detecta
  demanda — hoy el volumen de eventos no lo justifica.
- La búsqueda usa LIKE (no full-text index) porque el volumen esperado
  (decenas/cientos de eventos) no justifica configurar MySQL FULLTEXT.