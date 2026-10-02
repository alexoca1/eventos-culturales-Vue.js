# Feature Specification: Sistema de variables CSS — Frontend

**Feature Branch**: `010-css-variables`
**Created**: 2026-09-23
**Status**: Implemented (2026-09-24, ver tasks.md)
**Input**: eventos.css e index.css repiten la misma paleta de colores hardcodeada de forma independiente, sin variables. Introducir un sistema de custom properties CSS como base para las mejoras de diseño que vienen después (dashboards, navegación, responsive).

## User Scenarios & Testing

### User Story 1 - Paleta centralizada en un único fichero (Priority: P1)
Un nuevo `frontend/css/variables.css` define todos los colores, espaciados, radios y sombras usados hoy en el proyecto como custom properties en `:root`, cargado antes que cualquier otra hoja de estilo en las 4 páginas.

**Independent Test**: cambiar `--color-primary` en `variables.css` cambia el color del header y de todos los botones principales en las 4 páginas, sin tocar `eventos.css` ni `index.css`.

**Acceptance Scenarios**:
1. **Given** las 4 páginas HTML, **When** se inspecciona el `<head>`, **Then** todas cargan `css/variables.css` antes que su hoja de estilo específica.
2. **Given** `eventos.css` e `index.css`, **When** se audita cada valor de color/radio/sombra, **Then** ninguno queda como literal hardcodeado — todos usan `var(--...)`.

---

### User Story 2 - El resultado visual es idéntico al actual (Priority: P1)
Este refactor es puramente estructural — ningún color, tamaño o espaciado cambia de valor, solo de origen.

**Independent Test**: comparar visualmente cada una de las 4 páginas antes/después — deben verse pixel-idénticas.

**Acceptance Scenarios**:
1. **Given** cualquiera de las 4 páginas, **When** se compara visualmente antes y después del refactor, **Then** no hay ninguna diferencia perceptible.
2. **Given** el bug conocido de `body` en `index.css` (`#f4f4f4` muerto, sobreescrito por `#1894ff`), **When** se refactoriza, **Then** se elimina la declaración muerta y solo queda `background-color: var(--color-bg)`.

## Requirements

### Functional Requirements
- **FR-001**: `variables.css` MUST definir, como mínimo: colores de marca (primario, hover, secundario, fondo, superficie), colores de texto, colores de estado (badges de moderación: pendiente/aprobado/rechazado/eliminación), colores de rol (admin/organizador/usuario), color de favorito, espaciados (xs/sm/md/lg), radio de borde, sombra estándar, familia tipográfica y duración de transición.
- **FR-002**: `eventos.css` e `index.css` MUST reemplazar cada valor hardcodeado correspondiente por `var(--nombre-variable)`, sin introducir ningún valor nuevo.
- **FR-003**: Las 4 páginas HTML (`index.html`, `views/administrador.html`, `views/organizador.html`, `views/usuarioEstandar.html`) MUST cargar `variables.css` mediante `<link>`, antes que su hoja de estilo específica.
- **FR-004**: La declaración muerta `background-color: #f4f4f4` en `body` de `index.css` MUST eliminarse.

## Success Criteria

### Measurable Outcomes
- **SC-001**: Cero valores de color hardcodeados en `eventos.css`/`index.css` fuera de `variables.css` (verificable por inspección — no hay linter de CSS en este proyecto).
- **SC-002**: Las 4 páginas se ven pixel-idénticas antes/después.

## Assumptions
- No se introduce un preprocesador (Sass/Less) — custom properties nativas son suficientes y no añaden dependencia (coherente con "sin build").
- No se añade dark mode en esta feature — las variables son la base para poder hacerlo después si se quiere, pero no es requisito aquí.