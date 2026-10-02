# Feature Specification: Paginación en cliente — Frontend

**Feature Branch**: `017-paginacion`
**Created**: 2026-09-25
**Status**: Implemented (2026-09-25, ver tasks.md)
**Input**: El backend ahora devuelve páginas de 10 eventos (014-paginacion). El frontend debe mostrar los primeros 10, con botones "Anterior"/"Siguiente" y un indicador de página.

## User Scenarios & Testing

### User Story 1 - Próximos eventos paginados (invitado y usuario) (Priority: P1)
La sección "Próximos eventos" carga la primera página al entrar; los botones navegan entre páginas sin recargar la página HTML.

**Acceptance Scenarios**:
1. **Given** hay más de 10 próximos eventos, **When** el invitado entra, **Then** ve 10 tarjetas y el botón "Siguiente" activo; "Anterior" desactivado/oculto.
2. **Given** el invitado pulsa "Siguiente", **When** ocurre, **Then** ve los siguientes 10 (o menos si es la última página), el botón "Anterior" aparece, la vista sube al inicio del listado.
3. **Given** se está en la última página, **When** el invitado intenta avanzar, **Then** el botón "Siguiente" está desactivado/oculto.
4. **Given** hay 10 o menos eventos, **When** el invitado entra, **Then** no aparece ningún botón de navegación.

---

### User Story 2 - Búsqueda por fecha/etiquetas también pagina (Priority: P1)
Al buscar por fecha o etiquetas, los resultados también se paginan.

**Acceptance Scenarios**:
1. **Given** una búsqueda devuelve más de 10 resultados, **When** se muestra, **Then** aparece el control de paginación con los mismos botones.
2. **Given** el invitado cambia de filtro (nueva fecha o nueva etiqueta), **When** ocurre, **Then** la paginación se reinicia a la página 0.

---

### User Story 3 - "Mis eventos" del organizador también pagina (Priority: P1)
La sección de "Mis eventos" aplica la misma lógica de paginación.

**Acceptance Scenarios**:
1. **Given** el organizador tiene más de 10 eventos propios, **When** abre "Mis eventos", **Then** ve 10 con navegación de páginas.

---

### Edge Cases
- Al navegar de página, la vista hace scroll al inicio del listado de eventos (no al inicio de la página HTML completa), para que el usuario no pierda de vista el contenido nuevo.
- El indicador de página ("Página X de Y") usa indexación base 1 para el usuario (`number + 1` internamente), aunque la API usa base 0.
- Cambiar el filtro de etiquetas o fecha reinicia `paginaActual` a 0 — nunca se hace una petición a una página que ya no existe con el nuevo filtro.

## Requirements

### Functional Requirements
- **FR-001**: `useEventosInvitado` MUST mantener `paginaActual` (base 0) y `paginaMeta` (`{ totalPages, totalElements, first, last }`) como estado reactivo.
- **FR-002**: `searchEvent()` y `cargarProximos()` MUST incluir `?page=paginaActual` en la URL y guardar los metadatos de la respuesta.
- **FR-003**: MUST existir funciones `paginaSiguiente()` y `paginaAnterior()` que incrementen/decrementen `paginaActual` y relancen la búsqueda.
- **FR-004**: Cambiar cualquier filtro (fecha, etiqueta) MUST reiniciar `paginaActual` a 0 antes de llamar a `searchEvent()`.
- **FR-005**: La plantilla de invitado MUST mostrar el control de paginación (botones + indicador) solo cuando `paginaMeta.totalPages > 1`.
- **FR-006**: `useMisEventos` MUST aplicar la misma lógica de paginación para `GET /eventos/mios`.

## Success Criteria

### Measurable Outcomes
- **SC-001**: Con 15 próximos eventos, la primera carga muestra 10 y el botón "Siguiente" activo.
- **SC-002**: Pulsar "Siguiente" carga los 5 restantes y desactiva ese botón.
- **SC-003**: Cambiar la fecha o las etiquetas reinicia a la página 1.
- **SC-004**: `node --check` en los ficheros JS tocados sin errores.

## Assumptions
- No se muestra un número de página específico clicable (tipo "1 2 3 4 5") — solo "Anterior"/"Siguiente" + indicador de texto. Para 10 por página y el volumen esperado de eventos, es suficiente y más fácil de mantener.
- `GET /eventos/pendientes` no se toca — el admin ve toda la cola sin paginar (decisión tomada en el backend spec).