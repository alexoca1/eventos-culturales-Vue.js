# Implementation Plan: Accesibilidad WCAG 2.2 Nivel AA

**Branch**: `022-accesibilidad-wcag-aa` | **Date**: 2026-10-03

## Summary

1. Redefinir la paleta en `frontend/css/variables.css` para superar el contraste 4.5:1.
2. Aplicar `:focus-visible` explícito en `index.css` y `eventos.css`.
3. Revisar y asegurar `alt` en imágenes y etiquetas `<label>` asociadas en todos los HTMLs.

## Project Structure

```text
frontend/css/
├── variables.css (colores WCAG AA)
├── index.css (:focus-visible)
└── eventos.css (:focus-visible y contraste)
```
