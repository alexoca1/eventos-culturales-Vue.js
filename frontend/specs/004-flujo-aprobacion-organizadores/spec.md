# Feature Specification: Panel de Organizador y Cola de Moderación — Frontend

**Feature Branch**: `004-flujo-aprobacion-organizadores`
**Created**: 2026-09-18
**Status**: Implemented (2026-09-22, ver tasks.md)
**Input**: Consumir el flujo de aprobación del backend (007-flujo-aprobacion-organizadores). El organizador necesita: crear/editar/eliminar sus eventos con previsualización + aviso + botón de confirmar antes de enviarlos a revisión; ver el estado de sus eventos. El admin necesita una cola de moderación para aprobar/rechazar.

## User Scenarios & Testing

### User Story 1 - Login redirige según el rol (Priority: P1)
El login (`index.html`, ya existente) deja de asumir que todo login es de admin: tras autenticar, redirige según los roles devueltos por la API.

**Acceptance Scenarios**:
1. **Given** login exitoso con `roles` incluyendo `ROLE_ADMIN`, **When** se completa, **Then** redirige a `views/administrador.html` (comportamiento actual, sin cambios).
2. **Given** login exitoso con `roles` incluyendo `ROLE_ORGANIZADOR` (sin `ROLE_ADMIN`), **When** se completa, **Then** redirige a `views/organizador.html` (vista nueva).
3. **Given** login exitoso sin ninguno de esos dos roles, **When** se completa, **Then** se comporta como hoy con cualquier login no-admin (mensaje de error o vista de invitado — fuera de alcance de esta feature, se resuelve en `008-favoritos-recordatorios`).

> **Nota (2026-10-05)**: la UI de login tiene dos entradas — `index.html` (raíz) y `views/login.html`. Estas acceptance scenarios describen el **destino** (la página de cada rol), que no cambia; lo que cambia es cómo se construye la ruta, que ahora es relativa al documento con prefijo según el contexto (`views/` en la raíz, vacío dentro de `views/`), de modo que ambos puntos de entrada llevan a la misma vista sin aparecer `views/views/`.

---

### User Story 2 - Crear evento con previsualización y confirmación explícita (Priority: P1)
El organizador rellena el formulario de evento; antes de enviarlo, ve exactamente cómo quedará publicado, junto con un aviso de que requiere aprobación, y debe pulsar un botón de confirmación explícito para enviarlo a revisión.

**Why this priority**: Es el requisito que más explícitamente se pidió — evitar que el organizador publique "sin darse cuenta" de que queda pendiente.

**Acceptance Scenarios**:
1. **Given** el organizador rellena el formulario (establecimiento, dirección, fecha, horaInicio/horaFin, descripción, categoría, cartel, mapa), **When** pulsa "Previsualizar", **Then** ve una tarjeta con el aspecto exacto que tendría el evento en la vista pública (mismo componente visual que usa `usuarioEstandar.html`), sin haberlo enviado todavía al backend.
2. **Given** la previsualización está visible, **When** el organizador la revisa, **Then** ve un aviso destacado: *"Este evento quedará pendiente de revisión. Un administrador debe aprobarlo antes de que sea visible al público."*
3. **Given** la previsualización y el aviso están visibles, **When** el organizador pulsa el botón **"Confirmar y enviar a revisión"**, **Then** se envía la petición real de creación al backend.
4. **Given** el organizador quiere corregir algo, **When** pulsa "Volver a editar" en la pantalla de previsualización, **Then** vuelve al formulario con los datos ya rellenados, sin haber enviado nada.
5. **Given** la fecha elegida no cumple las 48h de antelación, **When** el organizador intenta previsualizar, **Then** se muestra el error de validación (400 del backend) antes de llegar a la previsualización, o —preferible— se valida ya en el frontend antes de permitir avanzar a la previsualización, para no hacerle perder tiempo rellenando algo que el backend va a rechazar.

---

### User Story 3 - Editar un evento propio, mismo patrón de previsualización (Priority: P1)
Editar reutiliza el mismo patrón: previsualización + aviso + confirmación, con los datos precargados.

**Acceptance Scenarios**:
1. **Given** el organizador edita uno de sus eventos, **When** llega a la previsualización, **Then** ve el mismo aviso que en creación, adaptado: *"Esta edición quedará pendiente de revisión. El evento dejará de ser visible al público hasta que se apruebe."*
2. **Given** confirma la edición, **When** se envía, **Then** usa `PUT /eventos/{id}` (comportamiento ya definido por el backend: el evento vuelve a `PENDIENTE_REVISION`).

---

### User Story 4 - Solicitar eliminación de un evento propio (Priority: P1)
Eliminar muestra una confirmación con aviso explícito de que también requiere aprobación.

**Acceptance Scenarios**:
1. **Given** el organizador pulsa "Eliminar" sobre uno de sus eventos, **When** aparece el diálogo de confirmación, **Then** el texto aclara: *"Esta acción no elimina el evento de inmediato: quedará pendiente hasta que un administrador la apruebe."*
2. **Given** confirma, **When** se envía `DELETE /eventos/{id}`, **Then** el evento pasa a mostrarse como "Pendiente de eliminación" en su lista de "Mis eventos" (no desaparece de su propia lista, solo de la vista pública).

---

### User Story 5 - "Mis eventos" con estado visible (Priority: P1)
El organizador ve todos sus eventos (`GET /eventos/mios`) con su estado actual, y el motivo si fue rechazado.

**Acceptance Scenarios**:
1. **Given** el organizador tiene eventos en varios estados, **When** abre su panel, **Then** ve cada uno etiquetado claramente: "Pendiente de revisión", "Aprobado", "Rechazado" (+ motivo si existe), "Pendiente de eliminación".
2. **Given** un evento `RECHAZADO`, **When** el organizador lo edita para corregirlo, **Then** reutiliza el mismo formulario/previsualización de US3, y al confirmar vuelve a `PENDIENTE_REVISION`.

---

### User Story 6 - Cola de moderación del admin (Priority: P1)
El admin ve, dentro de `administrador.html`, una sección nueva con los eventos pendientes de cualquier organizador, y puede aprobar o rechazar (con motivo opcional) cada uno.

**Acceptance Scenarios**:
1. **Given** admin autenticado, **When** abre la sección "Pendientes de revisión", **Then** ve todos los eventos `PENDIENTE_REVISION` y `PENDIENTE_ELIMINACION` (`GET /eventos/pendientes`), distinguibles por tipo de solicitud.
2. **Given** un evento pendiente, **When** el admin pulsa "Aprobar", **Then** se llama a `POST /eventos/{id}/aprobar` y desaparece de la cola.
3. **Given** un evento pendiente, **When** el admin pulsa "Rechazar", **Then** se le ofrece un campo de motivo opcional antes de confirmar, y se llama a `POST /eventos/{id}/rechazar`.

---

### Edge Cases
- La previsualización (US2/US3) MUST reutilizar el mismo layout/CSS de tarjeta de evento que ya existe en `usuarioEstandar.html`, no un diseño nuevo — coherencia visual y menos trabajo.
- Si el organizador cierra la pantalla de previsualización sin confirmar, no se envía ninguna petición al backend (todo queda solo en el estado local de Vue hasta el clic en "Confirmar").
- El aviso de 48h se calcula en el frontend con la hora local del navegador, coherente con la validación del backend.

## Requirements

### Functional Requirements
- **FR-001**: El login MUST redirigir a `organizador.html` cuando el usuario autenticado tenga `ROLE_ORGANIZADOR` y no `ROLE_ADMIN`.
- **FR-002**: El formulario de creación/edición de evento del organizador MUST tener un paso intermedio de previsualización antes de enviar, mostrando el evento con el mismo aspecto que tendría en la vista pública.
- **FR-003**: La previsualización MUST mostrar un aviso explícito de que el evento (o el cambio) queda pendiente de aprobación, y MUST requerir un clic explícito en un botón de confirmación para proceder.
- **FR-004**: El frontend MUST validar la antelación de 48h antes de permitir avanzar a la previsualización (validación adicional a la del backend, para mejor UX, no en sustitución de ella).
- **FR-005**: Eliminar un evento propio MUST mostrar un aviso de que la eliminación queda pendiente de aprobación antes de confirmar.
- **FR-006**: El panel del organizador MUST listar sus eventos (`GET /eventos/mios`) con estado visible y motivo de rechazo si aplica.
- **FR-007**: El panel del admin MUST incluir una sección de cola de moderación (`GET /eventos/pendientes`) con acciones de aprobar/rechazar (rechazar con motivo opcional).
- **FR-008**: `apiToView()` MUST mapear `estado`, `creadoPor` y `motivoRechazo`.

## Success Criteria

### Measurable Outcomes
- **SC-001**: Un organizador puede completar el flujo crear→previsualizar→confirmar de punta a punta y ver su evento como "Pendiente de revisión" en su panel.
- **SC-002**: Un admin puede aprobar o rechazar desde la cola de moderación y el estado se refleja correctamente en el panel del organizador tras recargar.
- **SC-003**: `node --check js/eventos.js` sin errores.

## Assumptions
- Esta feature asume que ya existe al menos una cuenta con `ROLE_ORGANIZADOR` creada por un admin — la pantalla para crear esa cuenta es responsabilidad de `006-gestion-admins` (pendiente, se hace a continuación de esta).
- No se implementa aquí ninguna notificación en tiempo real (websockets, polling) — el organizador ve el estado actualizado al recargar/volver a su panel, coherente con el resto del proyecto (sin librerías nuevas).