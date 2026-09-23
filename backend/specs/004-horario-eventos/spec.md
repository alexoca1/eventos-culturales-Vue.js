# Feature Specification: Horario de Eventos (inicio/fin) — Eventos Culturales Puertollano

**Feature Branch**: `004-horario-eventos`
**Created**: 2026-09-18
**Status**: Implemented (2026-09-19, 58/58 tests en verde; ver tasks.md)
**Input**: User description: "Añadir hora de inicio y hora de fin a los eventos. Si la hora de fin es anterior o igual a la de inicio, el evento termina el día siguiente (ej. empieza 22:00, dura 4h, termina 02:00 del día siguiente)."

## User Scenarios & Testing

### User Story 1 - Registrar horario de un evento (Priority: P1)
El admin, al crear o editar un evento, puede indicar opcionalmente una hora de inicio y una hora de fin.

**Why this priority**: Es el campo base del que dependen las demás historias; sin esto no hay nada que validar ni mostrar.

**Independent Test**: Crear un evento con `horaInicio=20:00` y `horaFin=22:30` → se guarda y se recupera igual.

**Acceptance Scenarios**:
1. **Given** admin autenticado, **When** crea un evento sin informar horario, **Then** se guarda con `horaInicio`/`horaFin` en `null`, igual que antes de esta feature.
2. **Given** admin autenticado, **When** crea un evento con `horaInicio=20:00` y `horaFin=22:30`, **Then** se guarda y `GET` lo devuelve igual.

---

### User Story 2 - Validación de horario coherente (Priority: P1)
El sistema rechaza un evento donde se informa una hora pero no la otra.

**Why this priority**: Evita datos a medias que romperían el cálculo de la Historia 3.

**Independent Test**: Enviar solo `horaInicio` sin `horaFin` → 400.

**Acceptance Scenarios**:
1. **Given** admin autenticado, **When** `POST/PUT /eventos` con `horaInicio` informado y `horaFin` vacío (o viceversa), **Then** 400 con mensaje claro ("horaInicio y horaFin deben informarse juntas").
2. **Given** admin autenticado, **When** ambas están vacías o ambas informadas, **Then** se acepta.

---

### User Story 3 - Cálculo de cruce de medianoche expuesto por la API (Priority: P2)
Cuando `horaFin` es anterior o igual a `horaInicio`, el sistema MUST calcular y exponer la fecha real de finalización (`fecha` + 1 día), sin que el cliente tenga que reimplementar esa lógica.

**Why this priority**: Sin esto, cada consumidor (frontend actual, y cualquier futuro) tendría que duplicar la regla de negocio del cruce de medianoche.

**Independent Test**: Evento con `fecha=2026-10-10`, `horaInicio=22:00`, `horaFin=02:00` → el campo derivado `fechaFin` de la respuesta es `2026-10-11`.

**Acceptance Scenarios**:
1. **Given** un evento con `horaFin > horaInicio` (mismo día, ej. 20:00→22:30), **When** se consulta, **Then** `fechaFin` == `fecha` (mismo día).
2. **Given** un evento con `horaFin <= horaInicio` (ej. 22:00→02:00), **When** se consulta, **Then** `fechaFin` == `fecha + 1 día`.
3. **Given** un evento sin horario informado, **When** se consulta, **Then** `fechaFin` == `fecha` (sin horario, no hay cruce posible).

---

### Edge Cases
- `horaInicio == horaFin` (ej. ambas 20:00): se considera cruce de medianoche (dura 24h) según la regla "fin anterior **o igual** a inicio" — MUST tratarse igual que Historia 3, escenario 2.
- Eventos ya existentes en BD (sembrados sin esta feature) siguen funcionando: `horaInicio`/`horaFin` `null`, `fechaFin` calculado == `fecha`.
- La búsqueda pública por fecha (`GET /eventos?fecha=`) sigue indexando por `fecha` (día de inicio), no por `fechaFin` — un evento que cruza medianoche sigue apareciendo en la búsqueda del día en que *empieza*, no en la del día en que termina.

## Requirements

### Functional Requirements
- **FR-001**: `Evento` MUST tener `horaInicio` y `horaFin` (`LocalTime`, ambos opcionales/nullable).
- **FR-002**: El sistema MUST rechazar con 400 cualquier creación/edición donde solo una de las dos horas esté informada.
- **FR-003**: El sistema MUST exponer un campo derivado `fechaFin` (`LocalDate`, calculado, no persistido) en cada evento devuelto por la API: `fecha` si no hay horario o si `horaFin > horaInicio`; `fecha + 1 día` si `horaFin <= horaInicio`.
- **FR-004**: La búsqueda por fecha (`GET /eventos?fecha=`) MUST seguir usando `fecha` (día de inicio) sin cambios.
- **FR-005**: Eventos sin horario informado MUST seguir funcionando exactamente igual que antes de esta feature (compatibilidad hacia atrás).

### Key Entities
- **Evento**: + `horaInicio` (`LocalTime`, nullable), `horaFin` (`LocalTime`, nullable), + getter derivado `fechaFin` (no persistido, `@Transient`).

## Success Criteria

### Measurable Outcomes
- **SC-001**: Crear un evento con horario válido y recuperarlo devuelve los mismos valores.
- **SC-002**: Informar solo una de las dos horas devuelve 400 en el 100% de los casos probados.
- **SC-003**: Un evento 22:00→02:00 devuelve `fechaFin` = `fecha + 1`; uno 20:00→22:30 devuelve `fechaFin` = `fecha`.
- **SC-004**: `./mvnw test` en verde (suite completa: `001`+`002`+`003`+`004`).

## Assumptions
- No se persiste `fechaFin` en BD — se calcula siempre al vuelo desde `fecha`+`horaInicio`+`horaFin`, para no tener dos fuentes de verdad.
- No se valida una duración máxima del evento (ej. no se impide que dure "varios días") — de momento el cruce solo contempla +1 día, coherente con lo pedido.