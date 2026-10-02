# Implementation Plan: Página 404 — Frontend

**Branch**: `010-pagina-404` | **Date**: 2026-09-24 | **Spec**: `specs/010-pagina-404/spec.md`

## Summary
Tres ficheros nuevos: `404.html` (HTML estático, sin Vue, con `variables.css`),
`.htaccess` (directiva `ErrorDocument 404` para Apache) y `netlify.toml`
(configuración de 404 para Netlify). Sin cambios en ningún fichero existente.

## Technical Context
**Dependencies**: ninguna nueva.
**Testing**: prueba manual en Apache/XAMPP (acceder a ruta inexistente).

## Constitution Check
- [x] Sin JavaScript en la 404 (FR-005): es HTML + CSS puro, el fallback más robusto.
- [x] Sin tocar `.htaccess` existente si ya hubiera uno — verificar primero.

## Project Structure
```text
frontend/404.html          # nuevo
frontend/.htaccess         # nuevo (solo ErrorDocument 404)
frontend/netlify.toml      # nuevo (ya existe netlify.com para el frontend)
```

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|--------------------------------------|
| Ninguna | — | — |