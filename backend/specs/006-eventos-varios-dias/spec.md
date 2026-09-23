# Feature Specification: Eventos de varios días — Eventos Culturales Puertollano

**Feature Branch**: `006-eventos-varios-dias`

**Created**: 2026-09-19

**Status**: Implemented (2026-09-19, 68/68 tests backend en verde + `node --check` frontend; ver tasks.md)

**Input**: Un evento puede durar varios días (ej. circo del 11 al 17 de noviembre): la hora fin necesita fecha propia a futuro. Horario y fecha fin opcionales por usabilidad (ausentes = evento de un día).

## User Scenarios & Testing

### User Story 1 - Rango de fechas por evento (Priority: P1)

El admin indica fecha fin opcional al crear/editar; vacía = un día. Fin anterior a inicio → 400.

**Independent Test**: Crear circo 11→17 y recuperarlo con ambas fechas; crear sin fecha fin y que se guarde como un día.

**Acceptance Scenarios**:

1. **Given** admin autenticado, **When** crea sin fecha fin, **Then** se guarda con `fechaFin == fecha`.
2. **Given** admin autenticado, **When** crea con fecha fin posterior, **Then** se guarda y se recupera igual.
3. **Given** admin autenticado, **When** fecha fin anterior a inicio, **Then** 400.

---

### User Story 2 - Búsqueda por día vigente (Priority: P1)

`GET /eventos?fecha=` devuelve los eventos vigentes ese día (solape de rangos), no solo los que empiezan ese día.

**Independent Test**: Circo 11→17 aparece buscando el 14; no aparece buscando el 18.

**Acceptance Scenarios**:

1. **Given** circo del 11 al 17, **When** se busca el 14, **Then** aparece.
2. **Given** mismo circo, **When** se busca el 10 o el 18, **Then** no aparece.

---

### User Story 3 - Frontend muestra el rango (Priority: P2)

Formulario con fecha fin opcional (validación cliente fin ≥ inicio); invitado y gestión muestran `"11/11/26 → 17/11/26"` cuando dura varios días; editar precarga la fecha fin.

**Independent Test**: Crear circo desde el formulario y ver el rango en la vista de invitado.

## Requirements

### Functional Requirements

- **FR-001**: `Evento.fechaFin` MUST ser `LocalDate` real nullable; ausente en entrada MUST normalizarse a `fecha` al guardar.
- **FR-002**: `fechaFin` anterior a `fecha` MUST dar 400 (backend y validación cliente).
- **FR-003**: `GET /eventos?fecha=` MUST devolver eventos con `fecha <= día AND (fechaFin IS NULL OR fechaFin >= día)`, combinable con `categoria`.
- **FR-004**: Horario y fecha fin MUST seguir opcionales (sin fricción para el evento de un día); la pareja horaria se mantiene (ambas o ninguna).
- **FR-005**: El aviso "(día siguiente)" del frontend MUST usar `fechaFin` real tal cual, sin recalcular cruces.

### Key Entities

- **Evento**: + `fechaFin` (`LocalDate`, nullable, normalizado a `fecha` al guardar).

## Success Criteria

### Measurable Outcomes

- **SC-001**: Circo 11→17 se guarda, se recupera y aparece buscando días intermedios.
- **SC-002**: Fin anterior → 400 en backend y aviso en frontend antes de enviar.
- **SC-003**: `./mvnw test` (68/68) y `node --check` en verde.

## Assumptions

- El cálculo automático +1 de cruce nocturno desaparece: el admin indica la fecha fin explícita (más claro con selector de fecha).
- Filas antiguas sin `fechaFin` se migran a un día en el arranque (backfill idempotente).
