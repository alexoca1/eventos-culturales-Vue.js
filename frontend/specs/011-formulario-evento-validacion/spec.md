# Feature Specification: Validación visible del formulario + copy de previsualizar — Frontend

**Feature Branch**: `011-formulario-evento-validacion`
**Created**: 2026-09-24
**Status**: Implemented (2026-09-24, ver tasks.md)
**Input**: El formulario fallaba con `alert()` genéricos sin decir qué input faltaba. Punto 3 de mejoras: validación campo por campo con mensaje rojo bajo el input y foco al primero que falte; cartel obligatorio al crear (el backend ya lo exige: `011-cartel-obligatorio`); copy que explique el flujo en 2 pasos; la etiqueta del campo pasa a "Descripción del evento" (el campo ya existía como "Evento Principal").

## User Scenarios & Testing

### User Story 1 - El formulario dice qué falta y dónde (Priority: P1)
Al intentar previsualizar (organizador) o guardar (admin) con datos obligatorios vacíos, cada input que falte muestra su mensaje en rojo debajo y el foco va al primero.

**Independent Test**: abrir "Nuevo evento" vacío y pulsar el botón → aparecen 4-5 mensajes rojos y el foco queda en el establecimiento.

**Acceptance Scenarios**:
1. **Given** formulario con establecimiento vacío, **When** intenta enviar, **Then** mensaje rojo bajo ese input + foco en él, sin llamar al backend.
2. **Given** varios campos vacíos, **When** intenta enviar, **Then** todos muestran su mensaje pero el foco va solo al primero (orden: establecimiento, dirección, fecha, descripción, cartel).
3. **Given** crea sin cartel, **When** intenta enviar, **Then** mensaje de cartel obligatorio; en edición no se exige (se conserva el existente).
4. **Given** abre el formulario (crear o editar), **When** se muestra, **Then** sin mensajes de error residuales.

---

### User Story 2 - El flujo de previsualizar se entiende en 2 pasos (Priority: P1)
El botón y los textos dejan claro que previsualizar es el paso 1 y confirmar el paso 2.

**Acceptance Scenarios**:
1. **Given** el organizador abre el formulario, **When** lo lee, **Then** ve "Paso 1 de 2: previsualiza…" y el botón "Previsualizar antes de confirmar".
2. **Given** está en la previsualización, **When** la lee, **Then** el botón dice "Paso 2 de 2: Confirmar y enviar a revisión".

## Requirements

### Functional Requirements
- **FR-001**: `validarObligatorios()` MUST comprobar establecimiento, dirección, fecha, descripción y cartel (solo en crear), pintar un mensaje rojo bajo cada input vacío y llevar el foco al primero; MUST devolver `false` sin llamar al backend si falta algo.
- **FR-002**: `previsualizar()` y `submitForm()` MUST llamar a `validarObligatorios()` lo primero.
- **FR-003**: Abrir el formulario en cualquier modo MUST limpiar los mensajes de error.
- **FR-004**: La etiqueta del campo de descripción MUST decir "Descripción del evento"; el cartel MUST indicar "(obligatorio)" en crear y "(opcional: se conserva el actual)" en edición.
- **FR-005**: El botón de envío del organizador MUST decir "Previsualizar antes de confirmar" con el aviso "Paso 1 de 2"; el de la previsualización, "Paso 2 de 2: Confirmar y enviar a revisión".

## Success Criteria

### Measurable Outcomes
- **SC-001**: Formulario vacío → mensajes rojos + foco, cero llamadas al backend (verificable en Network).
- **SC-002**: Crear sin cartel → mensaje de cartel (además del 400 del backend como red de seguridad).
- **SC-003**: `node --check` sin errores.

## Assumptions
- Las validaciones de reglas (mapa, fechaFin, 48h, horario parejo) siguen con `alert()` como hoy — solo los *datos que faltan* pasan al formato rojo+foco.
- Sin librería de formularios (coherente con "sin build").
