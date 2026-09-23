# Feature Specification: Eventos de varios días (fechaFin) — Frontend

**Feature Branch**: `007-eventos-varios-dias`
**Created**: 2026-09-23
**Status**: Implemented (retrodocumentada 2026-09-23, ver tasks.md)
**Input**: Consumir el campo real `fechaFin` de la API (backend `006-eventos-varios-dias`) en los formularios de admin/organizador y en las vistas de invitado/gestión: selector de fecha fin opcional, validación en cliente y rango visible "DD/MM/AA → DD/MM/AA".

> Nota: esta spec se redactó a posteriori — describe lo que `js/eventos.js` y las vistas YA hacen, no un diseño previo.

## User Scenarios & Testing

### User Story 1 - Admin/organizador indica la fecha fin al crear/editar (Priority: P1)
Los formularios de crear/editar evento incluyen un campo de fecha fin opcional junto a la fecha de inicio.

**Independent Test**: Crear un evento con fecha fin posterior desde el formulario → viaja en el `multipart/form-data` y el evento aparece en búsquedas de días intermedios.

**Acceptance Scenarios**:
1. **Given** el admin/organizador abre "Nuevo evento", **When** deja vacía la fecha fin, **Then** el evento se crea como de un día (`fechaFin: null`, el backend la normaliza a `fecha`).
2. **Given** rellena una fecha fin posterior a la de inicio, **When** guarda, **Then** viaja en el payload como `fechaFin`.
3. **Given** rellena una fecha fin anterior a la de inicio, **When** intenta guardar o previsualizar, **Then** se muestra el aviso "La fecha de fin no puede ser anterior a la de inicio" y no se envía nada.
4. **Given** edita un evento de varios días, **When** se abre el formulario, **Then** la fecha fin viene precargada; si el evento es de un día, el campo aparece vacío.

---

### User Story 2 - El invitado ve el rango del evento (Priority: P1)
Las tarjetas de evento muestran una línea "Rango: DD/MM/AA → DD/MM/AA" solo cuando el evento dura varios días, usando el `fechaFin` que ya trae la API — sin recalcular nada en JS.

**Independent Test**: Un evento con `fecha=2026-11-11`, `fechaFin=2026-11-17` muestra "Rango: 11/11/26 → 17/11/26"; uno con `fechaFin == fecha` no muestra línea de rango.

**Acceptance Scenarios**:
1. **Given** un evento de un día (`fechaFin` ausente o igual a `fecha`), **When** se muestra, **Then** no aparece ninguna línea de rango (comportamiento anterior, sin cambios).
2. **Given** un evento de varios días, **When** se muestra en la vista de invitado, la de gestión de admin o "Mis favoritos", **Then** aparece "Rango: DD/MM/AA → DD/MM/AA".
3. **Given** el organizador previsualiza un evento de varios días antes de enviarlo, **When** se muestra la previsualización, **Then** también incluye la línea de rango.

---

### Edge Cases
- Filas antiguas sin `fechaFin` (null en BD): `apiToView()` lo sustituye por `fecha`, así que se tratan como eventos de un día sin línea de rango.
- La comparación "fin < inicio" en cliente es lexicográfica sobre strings `YYYY-MM-DD`, válida porque el formato ISO ordena cronológicamente.
- La búsqueda por fecha del invitado no cambió: sigue pasando `?fecha=` tal cual; el solapamiento de rango lo resuelve el backend.

## Requirements

### Functional Requirements
- **FR-001**: Los formularios de `administrador.html` y `organizador.html` MUST incluir un input `type="date"` ligado a `inputFechaFin`, etiquetado "Fecha fin (opcional, vacío = un día)".
- **FR-002**: `submitForm()` y `previsualizar()` MUST rechazar con aviso si `inputFechaFin < inputDate`, sin enviar nada.
- **FR-003**: El DTO enviado MUST llevar `fechaFin: inputFechaFin || null` (vacío = un día, el backend normaliza).
- **FR-004**: `apiToView()` MUST mapear `fechaFin: e.fechaFin || e.fecha` (null antiguo = un día).
- **FR-005**: `formatRango(ev)` MUST devolver `""` si no hay `fechaFin` o coincide con `date`, y `"{fecha} → {fechaFin}"` (formato `DD/MM/AA`) en caso contrario.
- **FR-006**: Las tarjetas de `usuarioEstandar.html` (resultado + "Mis favoritos"), `administrador.html` (gestión) y la previsualización de `organizador.html` MUST mostrar la línea de rango solo cuando `formatRango(ev)` no sea vacío.
- **FR-007**: `editEvento(ev)` MUST precargar `inputFechaFin` con `ev.fechaFin` cuando difiera de `ev.date`, y vacío en caso contrario; abrir el formulario en modo crear MUST vaciarlo.

## Success Criteria

### Measurable Outcomes
- **SC-001**: Crear un evento con fecha fin desde el formulario y verlo con su rango en la vista de invitado y en búsquedas de días intermedios.
- **SC-002**: Un evento de un día no muestra línea de rango; uno de varios días muestra "DD/MM/AA → DD/MM/AA".
- **SC-003**: `node --check js/eventos.js` sin errores de sintaxis (mismo gate que las features anteriores).

## Assumptions
- Se asume que `backend/specs/006-eventos-varios-dias` ya está implementado y desplegado en el entorno donde se prueba esto (dependencia entre features).
