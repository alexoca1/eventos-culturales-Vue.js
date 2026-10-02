# Feature Specification: Eventos futuros ordenados — Backend

**Feature Branch**: `013-eventos-futuros-ordenados`
**Created**: 2026-09-25
**Status**: Implemented (2026-09-25, ver tasks.md)
**Input**: Punto 6 de mejoras: `GET /eventos?futuros=true` devuelve solo eventos vigentes hoy o después (fechaFin ≥ hoy), ordenados de lo más próximo a lo último, combinable con etiquetas y con el filtro de estado de 007.

## User Scenarios & Testing

### User Story 1 - Próximos eventos ordenados (Priority: P1)
`?futuros=true` filtra por vigencia futura y ordena por fecha ascendente; respeta admin/todos vs público/APROBADO.

**Independent Test**: con eventos pasados, vigentes y futuros → `GET /eventos?futuros=true` devuelve vigentes+futuros ordenados por fecha.

**Acceptance Scenarios**:
1. **Given** eventos en los 3 grupos, **When** invitado pide `?futuros=true`, **Then** solo vigentes/futuros `APROBADO`, ordenados por fecha.
2. **Given** admin, **When** `?futuros=true`, **Then** los mismos sin filtrar por estado.
3. **Given** `?futuros=true&etiquetas=MUSICA`, **When** se pide, **Then** intersección ANY + futuro.
4. **Given** `?fecha=X&futuros=true`, **When** se pide, **Then** 400 (excluyentes, mensaje claro).

## Requirements

### Functional Requirements
- **FR-001**: `GET /eventos` MUST aceptar `futuros` (boolean): `fechaFin >= hoy` (con red null-safe), orden `fecha ASC, id ASC`.
- **FR-002**: `futuros` MUST combinarse con `etiquetas` (ANY) y con la regla admin/APROBADO de 007.
- **FR-003**: `fecha` + `futuros` juntos MUST devolver 400.

## Success Criteria

### Measurable Outcomes
- **SC-001**: 4 tests nuevos en verde (público, público+etiquetas, admin, 400 combo).
- **SC-002**: `./mvnw test` en verde (152/152).

## Assumptions
- "Hoy" es fecha del servidor (`LocalDate.now()`); la hora solo importa a nivel de día (los eventos no tienen hora de fin evaluable contra "ahora" de forma fiable para rangos).
