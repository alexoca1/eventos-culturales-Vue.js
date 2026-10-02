# Feature Specification: Footer global — Frontend

**Feature Branch**: `018-footer`
**Created**: 2026-09-25
**Status**: Implemented (2026-09-25, ver tasks.md)
**Input**: El frontend no tiene footer. Añadir uno consistente en las 4 páginas
con datos de la organización, enlaces a redes sociales y copyright.

## User Scenarios & Testing

### User Story 1 - Footer visible en todas las páginas (Priority: P1)
El footer aparece al final de las 4 páginas del proyecto con el mismo
contenido y diseño.

**Acceptance Scenarios**:
1. **Given** cualquiera de las 4 páginas, **When** el usuario llega al final,
   **Then** ve el footer con el logo/nombre de la organización, dirección,
   teléfono, email y enlaces a redes sociales.
2. **Given** el footer, **When** el usuario hace clic en un icono de red social,
   **Then** se abre en una pestaña nueva (`target="_blank" rel="noopener"`).
3. **Given** una pantalla móvil (< 480px), **When** se ve el footer,
   **Then** las columnas se apilan verticalmente sin desbordarse.

---

### Edge Cases
- El footer usa `var(--color-primary)` como fondo (igual que el header)
  para consistencia visual entre cabecera y pie.
- Los iconos de redes sociales son SVGs inline — sin librería externa,
  coherente con la política del proyecto.
- Los enlaces de redes sociales apuntan a `#` (datos inventados) — en
  producción real se sustituirían por las URLs reales.

## Requirements

### Functional Requirements
- **FR-001**: El footer MUST aparecer en `index.html`, `views/administrador.html`,
  `views/organizador.html` y `views/usuarioEstandar.html`.
- **FR-002**: El footer MUST incluir: nombre de la organización, dirección,
  teléfono, email, iconos SVG de Facebook/Instagram/X(Twitter)/YouTube
  enlazados con `target="_blank"`, y copyright con año.
- **FR-003**: El footer MUST usar exclusivamente variables de `variables.css`
  para colores — ningún literal hardcodeado.
- **FR-004**: El footer MUST ser responsive: en móvil (< 480px) las columnas
  se apilan.
- **FR-005**: El HTML del footer MUST ser idéntico en las 4 páginas (misma
  estructura, mismos datos).

## Success Criteria

### Measurable Outcomes
- **SC-001**: El footer se ve en las 4 páginas con el mismo diseño.
- **SC-002**: Los 4 iconos de redes sociales son visibles y abren `#` en
  pestaña nueva.
- **SC-003**: En viewport de 390px no hay desbordamiento horizontal.