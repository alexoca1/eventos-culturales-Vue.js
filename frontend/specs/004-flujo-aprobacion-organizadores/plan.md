# Implementation Plan: Panel de Organizador y Cola de Moderación — Frontend

**Branch**: `004-flujo-aprobacion-organizadores` | **Date**: 2026-09-18 | **Spec**: `specs/004-flujo-aprobacion-organizadores/spec.md`

## Summary
Vista nueva `organizador.html` (formulario + previsualización + "mis eventos"), sección nueva de moderación dentro de `administrador.html`, y ajuste del login para redirigir según rol. Reutiliza el componente visual de tarjeta de evento que ya existe, sin crear un sistema de plantillas nuevo.

## Technical Context
**Language/Version**: JS plano, Vue 3 global, sin build.
**Dependencies**: ninguna nueva.
**Testing**: `node --check js/eventos.js` + prueba manual de los 6 flujos del spec.

## Constitution Check
- [x] Sin librerías nuevas, sin router (se sigue navegando entre `.html` como hasta ahora).
- [x] La previsualización reutiliza el HTML/CSS de tarjeta ya existente (`eventos.css`), no se duplica diseño.
- [x] La validación de 48h en frontend es una mejora de UX, no reemplaza la del backend (fuente de verdad sigue siendo el servidor).

## Project Structure

### Documentation (this feature)
```text
specs/004-flujo-aprobacion-organizadores/
├── plan.md
└── tasks.md
```

### Source Code
```text
frontend/views/organizador.html      # nueva: formulario + previsualización + "mis eventos"
frontend/views/administrador.html    # + sección "Pendientes de revisión"
frontend/index.html                  # login: redirección según rol
frontend/js/eventos.js               # apiToView(), lógica de previsualización, fetch a /mios, /pendientes, /aprobar, /rechazar
frontend/css/eventos.css             # estilos del aviso y del badge de estado (reutilizando clases existentes donde se pueda)
```

**Structure Decision**: 1 vista HTML nueva (`organizador.html`), el resto son ediciones sobre ficheros existentes.

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Ninguna | — | — |