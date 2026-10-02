# Feature Specification: Próximos por defecto + anteriores en gestión — Frontend

**Feature Branch**: `014-eventos-futuros-ordenados`
**Created**: 2026-09-25
**Status**: Implemented (2026-09-25, ver tasks.md)
**Input**: Punto 6 de mejoras: el invitado ve próximos eventos ordenados sin tener que buscar (consume `?futuros=true` del backend `013`); "Gestionar eventos" del admin muestra solo futuros con opción de ver anteriores.

## User Scenarios & Testing

### User Story 1 - Próximos eventos al entrar (Priority: P1)
`usuarioEstandar.html` carga y muestra los próximos (de lo más próximo a lo último) con pista de que también se puede buscar.

**Independent Test**: entrar como invitado sin buscar → sección "Próximos eventos" con tarjetas (incluida estrella de favorito); sin próximos → portada.

**Acceptance Scenarios**:
1. **Given** hay próximos, **When** se carga la página, **Then** se ven ordenados + pista de búsqueda por fecha/etiquetas.
2. **Given** no hay próximos, **When** se carga, **Then** portada (comportamiento anterior).
3. **Given** busca por fecha, **When** hay resultados, **Then** la sección de próximos se oculta.
4. **Given** pulsa "Buscar eventos" (volver), **When** ocurre, **Then** recarga los próximos frescos a la hora actual.

---

### User Story 2 - Gestión con futuros + ver anteriores (Priority: P1)
"Gestionar eventos" lista por defecto solo futuros a la fecha/hora de la consulta, con botón para incluir anteriores.

**Acceptance Scenarios**:
1. **Given** eventos pasados y futuros, **When** abre "Gestionar", **Then** solo futuros.
2. **Given** pulsa "Ver eventos anteriores", **When** ocurre, **Then** aparecen todos y el botón pasa a "Solo próximos".

## Requirements

### Functional Requirements
- **FR-001**: `useEventosInvitado` MUST tener `proximosEventos/showProximos/cargarProximos()`; `onMounted` de invitado MUST cargarlos; `searchEvent`/`verFavoritos` MUST ocultarlos; `volverBuscar` MUST recargarlos (pasa a async).
- **FR-002**: La plantilla MUST mostrar la sección "Próximos eventos" (misma tarjeta, con favoritos) + pista de búsqueda.
- **FR-003**: `useGestionEventos` MUST exponer `mostrarAnteriores` + `eventosGestion` (computed: filtra `fechaFin||date >= hoy` local salvo toggle); la plantilla MUST iterar `eventosGestion` + botón de toggle.

## Success Criteria

### Measurable Outcomes
- **SC-001**: Entrar sin buscar muestra próximos ordenados con favoritos operativos.
- **SC-002**: Toggle anteriores funciona en gestión.
- **SC-003**: `node --check` sin errores.

## Assumptions
- "Hoy" en cliente es fecha local del navegador (solo día, coherente con el backend).
- La búsqueda del admin no cambia (portada por defecto); el toggle es solo de gestión.
