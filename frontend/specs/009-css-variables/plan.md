# Implementation Plan: Sistema de variables CSS — Frontend

**Branch**: `010-css-variables` | **Date**: 2026-09-23 | **Spec**: `specs/010-css-variables/spec.md`

## Summary
Un fichero nuevo `variables.css` con custom properties en `:root`, y una pasada de búsqueda-y-reemplazo mecánica sobre `eventos.css`/`index.css` para que usen esas variables en vez de literales. Sin cambios de comportamiento ni de layout.

## Technical Context
**Dependencies**: ninguna — CSS custom properties nativas (soportadas por cualquier navegador moderno, sin polyfill).
**Testing**: comparación visual manual de las 4 páginas (no hay test automatizado de CSS en este proyecto).

## Valores identificados (auditados línea por línea en eventos.css + index.css)

| Variable | Valor | Uso actual |
|---|---|---|
| `--color-primary` | `#ff6347` | header, botones principales |
| `--color-primary-hover` | `#e53900` | hover de botones |
| `--color-secondary` | `#ffa07a` | fondo de `.control`, `.control_acceso`, `.mostrar_evento > div`, `.mostrar_ingresar`, `.eliminar`, `.gestionar` |
| `--color-bg` | `#1894ff` | fondo de `body` (las 2 hojas) |
| `--color-surface` | `#ffffff` | fondo de tarjetas internas (`.datos_eventos`, `.cartel`, `.mapa`, `.evento-row`) |
| `--color-border` | `#ccc` | bordes de inputs y tarjetas |
| `--color-text` | `#333` | texto general |
| `--color-text-muted` | `#999` | placeholders |
| `--color-estado-pendiente` | `#ff9800` | badge `PENDIENTE_REVISION` |
| `--color-estado-aprobado` | `#4caf50` | badge `APROBADO` |
| `--color-estado-rechazado` | `#f44336` | badge `RECHAZADO` |
| `--color-estado-eliminacion` | `#9c27b0` | badge `PENDIENTE_ELIMINACION` |
| `--color-rol-admin` | `#d32f2f` | badge rol admin |
| `--color-rol-organizador` | `#1976d2` | badge rol organizador |
| `--color-rol-user` | `#616161` | badge rol usuario |
| `--color-favorito` | `#e6a817` | `.fav-btn` |
| `--space-xs/sm/md/lg` | `0.25em/0.5em/1em/1.5em` | paddings/márgenes recurrentes |
| `--radius-sm` | `5px` | radio estándar |
| `--radius-pill` | `10px` | badges |
| `--shadow-sm` | `0 2px 4px rgba(0,0,0,0.1)` | sombra de tarjetas |
| `--font-family-base` | `Arial, sans-serif` | tipografía base |
| `--transition-fast` | `0.3s` | hover de botones |

## Project Structure
```text
frontend/css/variables.css           # nuevo
frontend/css/eventos.css              # refactor: literales -> var(--...)
frontend/css/index.css                 # refactor: literales -> var(--...); quita el bg muerto
frontend/index.html                     # + <link> a variables.css
frontend/views/administrador.html        # + <link> a variables.css
frontend/views/organizador.html           # + <link> a variables.css
frontend/views/usuarioEstandar.html        # + <link> a variables.css
```

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Ninguna | — | — |