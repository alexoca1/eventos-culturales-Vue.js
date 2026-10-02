# Feature Specification: Navegación y sesión unificada — Frontend

**Feature Branch**: `013-navegacion-sesion-unificada`
**Created**: 2026-09-25
**Status**: Implemented (2026-09-25, ver tasks.md)
**Input**: Punto 5 de mejoras: cerrar sesión en todas las páginas (con vuelta al inicio), header persistente con indicador de sesión + dropdown (editar datos/panel/cerrar), "Panel organizador" fuera del header, filtros separados de acciones, y enlaces a inicio/otras páginas.

## User Scenarios & Testing

### User Story 1 - Header unificado con sesión visible (Priority: P1)
Las 3 páginas logueadas comparten el mismo header: título (a inicio, sin perder sesión) + indicador con el nombre visible y dropdown.

**Independent Test**: entrar como organizador → el header muestra el nombre de la organización; clic → menú con "Mi panel", "Editar mis datos", "Cerrar sesión".

**Acceptance Scenarios**:
1. **Given** cualquier página con sesión, **When** se mira el header, **Then** mismo título enlazado a `index.html` e indicador con el nombre (organización si la hay).
2. **Given** clic en el indicador, **When** se abre el menú, **Then** "Mi panel" (página del rol), "Editar mis datos" (abre el perfil) y "Cerrar sesión".
3. **Given** sin sesión, **When** se mira el header, **Then** enlace "Ingresar" a `index.html`.
4. **Given** "Panel del Organizador" antes en el `h1`, **When** se abre la página, **Then** el `h1` es el estándar y el texto vive como subtítulo en el `main`.

---

### User Story 2 - Cerrar sesión vuelve al inicio (Priority: P1)
Cerrar sesión desde cualquier página limpia la sesión y navega a `index.html` para poder reingresar.

**Acceptance Scenarios**:
1. **Given** usuario normal en `usuarioEstandar.html`, **When** cierra sesión desde el menú, **Then** acaba en `index.html` (antes se quedaba en la vista sin forma de reingresar).
2. **Given** admin/organizador (antes sin botón de salir), **When** cierra sesión, **Then** mismo destino.

---

### User Story 3 - Filtros y acciones separados (Priority: P1)
`.control` solo lleva filtros de búsqueda; los botones de acción viven en `.acciones`.

**Acceptance Scenarios**:
1. **Given** `administrador.html`, **When** se inspecciona, **Then** `.control` = fecha + etiquetas; `.acciones` = Agregar/Gestionar/Pendientes/Usuarios/Etiquetas.
2. **Given** `usuarioEstandar.html`, **When** se inspecciona, **Then** `.acciones` = Mis favoritos/Buscar eventos (perfil y salir ya están en el menú).

## Requirements

### Functional Requirements
- **FR-001**: Las 3 páginas MUST compartir la misma estructura de header (`header-bar` + título a inicio + `.sesion`/`.sesion-entrar`).
- **FR-002**: `useAuth` MUST exponer `sesionNombre` (organización o email), `panelUrl` (página del rol), `menuAbierto`/`alternarMenu()`, `cargarSesion()` (rellena el store vía `GET /auth/perfil` o limpia token muerto) e `irAPerfil()`.
- **FR-003**: `logout()` MUST navegar a `../index.html` tras limpiar.
- **FR-004**: Los entrypoints MUST llamar a `cargarSesion()` en `onMounted` (tras la guardia donde la haya).
- **FR-005**: Ninguna página MUST tener botones sueltos de perfil/salir fuera del menú (se eliminan los duplicados del punto 4).

## Success Criteria

### Measurable Outcomes
- **SC-001**: Menú funcional en las 3 páginas (panel/perfil/salir) con el nombre correcto.
- **SC-002**: Logout → `index.html` siempre.
- **SC-003**: `node --check` sin errores.

## Assumptions
- El item "Mi panel" abre la sección dashboard del punto 7 (evolución: empezó como enlace a la página del rol en F154, `irAPanel()` lo convirtió en sección sin recarga).
- `index.html` (landing con login) mantiene su header simple + título enlazado; el menú vive solo en las páginas de app.
- La búsqueda por fecha para el organizador se resuelve en el punto 7 (enlace a cartelera pública).
