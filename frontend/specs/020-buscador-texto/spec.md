# Feature Specification: Buscador de texto — Frontend

**Feature Branch**: `020-buscador-texto`
**Created**: 2026-09-25
**Status**: Implemented (2026-10-02)

## User Scenarios & Testing

### User Story 1 - Buscador en la vista pública (Priority: P1)
Invitado, usuario logueado, organizador y admin (en "Ver cartelera pública")
usan el mismo buscador de texto.

**Acceptance Scenarios**:
1. El usuario escribe en el campo de texto y pulsa "Buscar" (o Enter) —
   se muestra la página 0 de resultados con paginación.
2. Al activar el buscador de texto se limpian `searchDate` y
   `searchEtiquetas`; al usar fecha o etiquetas se limpia `searchText`.
3. Con menos de 2 caracteres y se pulsa Buscar → mensaje de error inline
   "Escribe al menos 2 caracteres", sin llamar al backend.
4. Si no hay resultados → imagen `no_hay_eventos.jpg` con el mensaje
   "No se encontraron eventos para '{{searchText}}'."

### User Story 2 - Buscador en "Gestionar eventos" del admin (Priority: P1)
El admin filtra en cliente sobre los eventos ya cargados, sin petición extra.

**Acceptance Scenarios**:
1. El admin escribe en el campo de texto del panel "Gestionar eventos" →
   `eventosGestion` se filtra al instante (computed sobre `filtroTexto`).
2. Al limpiar el campo → vuelven todos los eventos.

## Requirements
- **FR-001**: Añadir `searchText` (ref "") a `useEventosInvitado`.
- **FR-002**: Función `buscarPorTexto()` que valida longitud ≥ 2, limpia
  fecha/etiquetas, reinicia a página 0 y llama a `GET /eventos?q=texto`.
- **FR-003**: En `usuarioEstandar.html`: campo de texto + botón "Buscar"
  + mensaje de error inline; al pulsar Buscar o Enter → `buscarPorTexto()`.
- **FR-004**: El buscador de texto es independiente: al activarlo, limpia
  `searchDate`/`searchEtiquetas`; al usar fecha/etiquetas, limpia `searchText`.
- **FR-005**: `useGestionEventos` añade `filtroTexto` (ref "") y el
  computed `eventosGestion` incluye el filtro de texto.
- **FR-006**: En `administrador.html`, en la sección "Gestionar eventos",
  añadir el campo de texto para filtrar.

## Success Criteria
- **SC-001**: Buscar "jazz" devuelve los eventos con esa palabra en nombre
  o establecimiento.
- **SC-002**: Buscar "a" muestra el mensaje de error inline, sin petición.
- **SC-003**: En "Gestionar eventos", escribir en el filtro reduce
  instantáneamente la lista visible.
- **SC-004**: `node --check` en los ficheros JS tocados.