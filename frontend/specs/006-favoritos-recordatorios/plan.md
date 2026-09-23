# Implementation Plan: Registro, Login y Favoritos de Usuario Estándar — Frontend

**Branch**: `006-favoritos-recordatorios` | **Date**: 2026-09-18 | **Spec**: `specs/006-favoritos-recordatorios/spec.md`

## Summary
Formulario de registro añadido a `index.html`; la lógica de redirección de login (ya extendida en `004` para admin/organizador) se completa con el caso `ROLE_USER`; `usuarioEstandar.html` gana un icono de favorito por tarjeta, una sección "Mis favoritos" y un control de sesión.

## Technical Context
**Language/Version**: JS plano, Vue 3 global, sin build.
**Dependencies**: ninguna nueva.
**Testing**: `node --check js/eventos.js` + prueba manual de los 5 flujos.

## Constitution Check
- [x] Reutiliza el componente visual de tarjeta de evento ya existente para "Mis favoritos" — no se crea uno nuevo.
- [x] Sin librería de iconos nueva — se puede usar un carácter/emoji (♡/♥) o un SVG inline simple, coherente con el resto del proyecto (sin dependencias de iconos hasta ahora).

## Project Structure

### Documentation (this feature)
```text
specs/006-favoritos-recordatorios/
├── plan.md
└── tasks.md
```

### Source Code
```text
frontend/index.html                  # + formulario de registro (toggle con el login ya existente)
frontend/views/usuarioEstandar.html  # + icono de favorito por tarjeta, sección "Mis favoritos", control de sesión
frontend/js/eventos.js               # register(), completar redirect de login, toggleFavorito(), loadFavoritos(), logout() para usuario estándar
frontend/css/eventos.css             # estilos del icono de favorito
```

**Structure Decision**: todo son ediciones sobre ficheros ya existentes; ninguna vista nueva.

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Ninguna | — | — |