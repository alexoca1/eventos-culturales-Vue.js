# Feature Specification: Accesibilidad WCAG 2.2 Nivel AA

**Feature Branch**: `022-accesibilidad-wcag-aa`
**Created**: 2026-10-03
**Status**: Completed

## User Scenarios & Testing

### User Story 1 - Corrección de contraste de color (Priority: P1)

Todos los elementos de texto e interactivos deben cumplir con un ratio de contraste mínimo de 4.5:1 (WCAG AA) sobre su fondo.

**Acceptance Scenarios**:

1. **Given** botones con `--color-primary: #ff6347` y texto blanco (ratio 2.95:1), **When** se aplica la paleta ajustada en `variables.css`, **Then** el ratio de contraste supera 4.5:1.
2. **Given** badges con textos de estado o rol, **When** se renderizan en pantalla, **Then** todos los textos son legibles con ratio >= 4.5:1.

### User Story 2 - Navegación por teclado y atributos ARIA (Priority: P1)

Todos los botones, inputs y elementos interactivos deben contar con etiquetas accesibles (`aria-label`, `<label for="...">`, `alt` en imágenes) e indicador claro de foco (`:focus-visible`).

**Acceptance Scenarios**:

1. **Given** un usuario navegando exclusivamente con la tecla Tab, **When** se desplaza por los elementos de la web, **Then** cada botón o campo activo muestra un borde/resaltado visible de enfoque.
2. **Given** una imagen de cartel o de aviso, **When** es procesada por un lector de pantalla, **Then** cuenta con un texto alternativo (`alt`) descriptivo.

---

## Requirements

### Functional Requirements

- **FR-001**: Ajustar variables en `frontend/css/variables.css` para garantizar ratios de contraste >= 4.5:1 en todos los estados.
- **FR-002**: Asegurar atributo `alt` en todas las etiquetas `<img>` del sitio.
- **FR-003**: Vincular todos los campos `<input>` y `<select>` a sus correspondientes `<label>` utilizando `for` e `id`.
- **FR-004**: Añadir reglas CSS para `:focus-visible` en botones, enlaces y campos de formulario.
