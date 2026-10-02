# Feature Specification: Página 404 — Frontend

**Feature Branch**: `010-pagina-404`
**Created**: 2026-09-24
**Status**: Implemented (2026-09-24, ver tasks.md)
**Input**: No existe ninguna página de error cuando se accede a una ruta que no existe.
El servidor devuelve un error genérico del sistema en vez de una página del proyecto.

## User Scenarios & Testing

### User Story 1 - Ruta inexistente muestra una página del proyecto (Priority: P1)
Cuando un usuario escribe manualmente una URL que no existe o sigue un enlace roto,
ve una página consistente con el resto del proyecto (mismas variables CSS,
mismo header) en vez de la pantalla de error por defecto del servidor.

**Independent Test**: acceder a `http://localhost/eventos-culturales/frontend/pagina-que-no-existe`
con Apache/XAMPP corriendo → muestra `404.html` del proyecto, no la pantalla de Apache.

**Acceptance Scenarios**:
1. **Given** Apache/XAMPP sirviendo el frontend, **When** se accede a una ruta inexistente,
   **Then** se muestra `404.html` con el diseño del proyecto.
2. **Given** Netlify sirviendo el frontend en producción, **When** se accede a una ruta
   inexistente, **Then** se muestra la misma `404.html`.
3. **Given** la página 404, **When** el usuario la ve, **Then** tiene un botón que lo
   lleva a `index.html` (inicio), sin perder la sesión si la tiene.
4. **Given** `vs code Live Server` (desarrollo local via file://), **When** se accede a
   una ruta inexistente, **Then** Live Server muestra su propio error 404 — este
   entorno no es configurable por el proyecto, se acepta como limitación (fuera de alcance).

---

### Edge Cases
- La página 404 es HTML estático puro — sin Vue, sin módulos ES, sin `import`.
  No tiene lógica de sesión, no hace fetch — es el fallback más simple posible.
- El header debe ser visualmente idéntico al del resto del proyecto (usa `variables.css`
  + una hoja inline mínima, sin depender de `eventos.css`/`index.css` que dan por
  sentado contextos de layout que la 404 no tiene).

## Requirements

### Functional Requirements
- **FR-001**: `frontend/404.html` MUST existir y usar `variables.css` para mantener
  consistencia visual con el resto del proyecto.
- **FR-002**: `frontend/.htaccess` MUST configurar Apache para servir `404.html`
  ante cualquier recurso no encontrado (`ErrorDocument 404`).
- **FR-003**: `frontend/netlify.toml` (o `frontend/_redirects`) MUST configurar
  Netlify para servir `404.html` en rutas no encontradas.
- **FR-004**: `404.html` MUST incluir un enlace/botón a `index.html` con texto claro
  ("Volver al inicio").
- **FR-005**: `404.html` MUST NOT requerir JavaScript para renderizarse (es el
  fallback de último recurso — si algo falla, no puede depender de scripts).

## Success Criteria

### Measurable Outcomes
- **SC-001**: Acceder a una URL inexistente en Apache/XAMPP muestra `404.html`
  del proyecto, no la pantalla por defecto de Apache.
- **SC-002**: `404.html` se ve visualmente consistente con el resto del proyecto.
- **SC-003**: El botón "Volver al inicio" lleva a `index.html` correctamente.

## Assumptions
- El `.htaccess` solo añade la directiva `ErrorDocument 404` — no toca ninguna otra
  configuración de Apache (reescritura de URLs, CORS, etc.) para no romper nada
  del entorno local existente.
- En desarrollo con Live Server de VS Code (file:// o 127.0.0.1:5500), la 404
  personalizada no se activa — solo funciona bajo Apache y Netlify. Se acepta.