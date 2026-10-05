# Feature Specification: Páginas legales, footer, consentimientos y mapas

**Feature Branch**: `021-paginas-legales-footer`
**Created**: 2026-10-03
**Status**: Completed

## User Scenarios & Testing

### User Story 1 - Enlaces y Footer Legal Unificado (Priority: P1)

Todas las páginas de la aplicación deben contar con un pie de página actualizado con enlaces visibles a Aviso Legal, Política de Privacidad y Términos de Uso, así como un aviso permanente de que la web es un proyecto educativo con datos ficticios e imágenes de IA.

**Acceptance Scenarios**:

1. **Given** cualquier vista de la web, **When** el usuario baja al footer, **Then** ve enlaces funcionales a "Aviso Legal", "Política de Privacidad", "Términos de Uso", "Reportar contenido" (mailto) y el disclaimer del proyecto.

### User Story 2 - Vista de Documentos Legales (Priority: P1)

Crear las páginas estáticas/componentes para la lectura del Aviso Legal (LSSI art 10 adaptado a proyecto educativo), Política de Privacidad (RGPD) y Términos de Uso (prohibiciones de contenido y aviso de moderación).

**Acceptance Scenarios**:

1. **Given** clic en "Aviso Legal", **When** se abre la vista, **Then** explica la naturaleza educativa del proyecto, ausencia de actividad comercial y contacto.
2. **Given** el formulario de registro de usuario, **When** el usuario se va a registrar, **Then** ve una casilla obligatoria "Acepto la política de privacidad y confirmo que tengo 14 años o más".

### User Story 3 - Carga de Google Maps bajo clic (Cero Banner de Cookies - Priority: P1)

Para evitar requerir un banner de consentimiento de cookies de terceros, los mapas de Google Maps no deben cargarse automáticamente mediante iframe al visualizar la tarjeta del evento, sino mostrar un botón/contenedor "Cargar mapa interactivo" que al ser pulsado despliega el iframe.

**Acceptance Scenarios**:

1. **Given** una tarjeta de evento con mapa, **When** se renderiza la tarjeta, **Then** se muestra una vista previa estática o botón "Ver mapa", sin cargar iframe de Google de forma inmediata.
2. **Given** clic en "Ver mapa", **When** el usuario da su consentimiento explícito mediante la interacción, **Then** se sustituye la vista previa por el iframe de Google Maps.

---

## Requirements

### Functional Requirements

- **FR-001**: Crear `views/avisoLegal.html`, `views/privacidad.html`, `views/terminos.html` (o vistas dinámicas en Vue).
- **FR-002**: Actualizar el footer en todos los HTMLs (`index.html`, `usuarioEstandar.html`, `administrador.html`, `organizador.html`).
- **FR-003**: Añadir checkbox obligatorio en los formularios de registro de usuario para aceptar la política de privacidad y declarar >= 14 años.
- **FR-004**: Modificar el renderizado de mapas en `eventos.js` para usar el patrón "Cargar mapa bajo clic".
