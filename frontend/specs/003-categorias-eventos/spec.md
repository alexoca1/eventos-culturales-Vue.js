# Feature Specification: Etiquetas múltiples — Frontend

**Feature Branch**: `003-categorias-eventos`
**Created**: 2026-09-18
**Status**: Implemented (2026-09-24, ver tasks.md)
**Input**: Consumir `etiquetas` (array de nombres), el filtro `?etiquetas=` ANY y el CRUD `/etiquetas` de la API (backend `005-categorias-eventos` reescrito, punto 2). Reemplaza la decisión anterior de categoría única.

## User Scenarios & Testing

### User Story 1 - Admin/organizador asigna etiquetas al crear/editar (Priority: P1)
El formulario muestra checkboxes con el catálogo (`GET /etiquetas`); se pueden marcar varias. Sin marcar ninguna, el backend asigna `OTROS`.

**Acceptance Scenarios**:
1. **Given** el admin crea un evento, **When** marca varias etiquetas, **Then** viajan en el payload como `etiquetas: [...]`.
2. **Given** no marca ninguna, **When** guarda, **Then** el evento queda con `OTROS` (lo pone el backend; el formulario avisa "sin marcar = Otra").
3. **Given** edita un evento existente, **When** abre el formulario, **Then** vienen marcadas sus etiquetas actuales.

---

### User Story 2 - Invitado filtra por etiquetas (Priority: P1)
La vista de invitado (y el buscador del admin) muestra checkboxes del catálogo, combinables con la fecha (ANY).

**Acceptance Scenarios**:
1. **Given** el invitado marca una o varias etiquetas sin fecha, **When** busca, **Then** ve los eventos que llevan alguna (`?etiquetas=a,b`).
2. **Given** marca etiquetas + fecha, **When** busca, **Then** ve solo los que cumplen ambas.
3. **Given** no marca ninguna, **When** busca solo por fecha, **Then** comportamiento actual sin cambios.
4. **Given** un evento en tarjeta, **When** se muestra, **Then** incluye su línea de etiquetas.

---

### User Story 3 - Admin gestiona el catálogo (Priority: P1)
Sección "Etiquetas" en `administrador.html` con crear/renombrar/eliminar.

**Acceptance Scenarios**:
1. **Given** el admin abre "Etiquetas", **When** crea una con nombre nuevo, **Then** aparece en la lista (y en los checkboxes).
2. **Given** intenta crearla duplicada o vacía, **When** ocurre, **Then** se muestra el error sin cerrar el formulario.
3. **Given** renombra a un nombre existente, **When** ocurre, **Then** se muestra el error.
4. **Given** elimina una etiqueta en uso, **When** el backend devuelve 409, **Then** se avisa ("hay eventos con esta etiqueta") sin borrar.

---

### Edge Cases
- El catálogo se carga al montar cada página (`cargarEtiquetas` en `onMounted`); si falla, lista vacía (los formularios muestran el aviso de OTROS igualmente).
- Abrir una sección (gestionar/pendientes/usuarios/etiquetas/buscar/formulario) oculta las demás (mismo patrón de flags excluyentes).

## Requirements

### Functional Requirements
- **FR-001**: Los formularios de admin/organizador MUST mostrar las etiquetas del catálogo como checkboxes ligados a `inputEtiquetas` (array), con el aviso de que sin marcar queda `Otra`.
- **FR-002**: `apiToView()` MUST mapear `etiquetas` (array de nombres desde `e.etiquetas[].nombre`).
- **FR-003**: Los buscadores MUST enviar `?etiquetas=a,b` cuando haya alguna marcada, combinado con `?fecha=`; sin marcas, igual que antes.
- **FR-004**: Las tarjetas MUST mostrar la línea de etiquetas cuando el evento las tenga.
- **FR-005**: `administrador.html` MUST incluir la sección "Etiquetas" (crear/renombrar/eliminar) conectada a `useEtiquetas`.

## Success Criteria

### Measurable Outcomes
- **SC-001**: Crear un evento con varias etiquetas y verlo con ellas en el filtro de invitado.
- **SC-002**: Los escenarios de filtro devuelven los resultados esperados (ANY).
- **SC-003**: CRUD de etiquetas con sus errores visibles (duplicada, en uso).
- **SC-004**: `node --check` sin errores en los ficheros tocados.

## Assumptions
- Ruptura limpia (punto 2): el backend ya no expone `categoria`; este frontend es el único cliente.
