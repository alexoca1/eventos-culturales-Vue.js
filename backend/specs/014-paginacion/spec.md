# Feature Specification: Paginación en servidor — Eventos Culturales Puertollano

**Feature Branch**: `014-paginacion`
**Created**: 2026-09-25
**Status**: Implemented (2026-09-25, ver tasks.md)
**Input**: GET /eventos devuelve todos los eventos de golpe. Para escalar y no cargar datos innecesarios, el listado público, el listado del admin y "Mis eventos" del organizador deben paginar en servidor (10 por página). El cliente pide la página que quiere con ?page=N (base 0) y recibe solo 10 registros + metadatos de paginación.

## User Scenarios & Testing

### User Story 1 - GET /eventos devuelve una página de 10 (Priority: P1)
El endpoint público respeta todos los filtros ya existentes (fecha, etiquetas, futuros, estado) y añade paginación.

**Independent Test**: con 25 eventos APROBADO en BD, `GET /eventos?futuros=true` devuelve `content` con ≤10 eventos, `totalElements=25`, `totalPages=3`, `last=false`; `GET /eventos?futuros=true&page=2` devuelve los últimos 5 y `last=true`.

**Acceptance Scenarios**:
1. **Given** hay más de 10 eventos, **When** `GET /eventos` sin `?page=`, **Then** devuelve página 0 con 10 eventos, `totalPages > 1`, `last=false`.
2. **Given** hay más de 10 eventos, **When** `GET /eventos?page=1`, **Then** devuelve la segunda página (eventos 11-20), `number=1`.
3. **Given** `GET /eventos?page=99` (más allá del total), **Then** devuelve `content=[]`, sin error 4xx.
4. **Given** cualquier combinación de filtros ya existentes (fecha, etiquetas, futuros), **When** se añade `?page=N`, **Then** la paginación se aplica sobre el resultado filtrado.
5. **Given** fecha + futuros juntos, **When** ocurre, **Then** sigue siendo 400 (no cambia).

---

### User Story 2 - GET /eventos/mios pagina (Priority: P1)
El organizador ve sus eventos de 10 en 10.

**Acceptance Scenarios**:
1. **Given** un organizador con 15 eventos propios, **When** `GET /eventos/mios`, **Then** devuelve 10 y `totalElements=15`.
2. **Given** `GET /eventos/mios?page=1`, **Then** devuelve los 5 restantes y `last=true`.

---

### User Story 3 - Respuesta consistente en todos los endpoints paginados (Priority: P1)
La forma del JSON de respuesta es idéntica en los 3 endpoints paginados — el cliente solo aprende un formato.

**Acceptance Scenarios**:
1. **Given** cualquiera de los 3 endpoints paginados, **When** se llama, **Then** la respuesta tiene siempre: `content` (array de eventos), `number` (página actual, base 0), `size` (tamaño de página, 10), `totalElements` (total de registros sin paginar), `totalPages`, `first` (boolean), `last` (boolean).

---

### Edge Cases
- `GET /eventos/pendientes` (cola de moderación del admin) **NO se pagina** en esta feature — la cola suele ser pequeña y añadir paginación complica la UX de moderación sin aportarle valor real.
- `?size=` no se expone al cliente — el tamaño de página es fijo en 10 (configurable vía `app.paginacion.size`, default 10) para evitar que un cliente pida `?size=10000` y tumbe el servidor.
- El orden de los resultados no cambia con la paginación — sigue siendo el mismo que ya tiene cada método (`fecha ASC, id ASC` para la mayoría, los futuros por proximidad).

## Requirements

### Functional Requirements
- **FR-001**: `GET /eventos` MUST devolver `Page<Evento>` (o un DTO equivalente) con `?page=N` (base 0, default 0) y tamaño fijo de `${app.paginacion.size:10}`.
- **FR-002**: Todos los filtros ya existentes en `GET /eventos` (fecha, etiquetas, futuros, estado) MUST seguir funcionando exactamente igual al añadir paginación.
- **FR-003**: `GET /eventos/mios` MUST devolver `Page<Evento>` con las mismas reglas.
- **FR-004**: `GET /eventos/pendientes` MUST seguir devolviendo `List<Evento>` sin cambios.
- **FR-005**: `?size=` NO MUST aceptarse como parámetro público — el tamaño lo fija el servidor.
- **FR-006**: La forma del JSON de respuesta MUST incluir siempre: `content`, `number`, `size`, `totalElements`, `totalPages`, `first`, `last`.

### Key Entities
- Sin entidades nuevas. Los métodos de `EventoRepository` que devuelven `List<Evento>` pasan a devolver `Page<Evento>` aceptando un parámetro `Pageable`.

## Success Criteria

### Measurable Outcomes
- **SC-001**: Con 25 eventos en BD, `GET /eventos?page=0` devuelve 10, `GET /eventos?page=2` devuelve 5 y `last=true`.
- **SC-002**: `GET /eventos/mios` pagina correctamente para un organizador con > 10 eventos.
- **SC-003**: `GET /eventos/pendientes` sigue devolviendo todos sin cambios.
- **SC-004**: `./mvnw test` en verde (suite completa).

## Assumptions
- Se usa `Page<Evento>` directamente de Spring Data — los metadatos (`totalElements`, `totalPages`, `last`, etc.) los serializa Jackson automáticamente sin un DTO envolvente explícito.
- El tamaño de página fijo (10) no es un límite de seguridad crítico aquí, pero sí una buena práctica documentada en las Assumptions: si en el futuro se necesita variable, se expone con un máximo validado.