# Implementation Plan: Páginas legales, footer, consentimientos y mapas

**Branch**: `021-paginas-legales-footer` | **Date**: 2026-10-03

## Summary

1. Crear páginas legales `views/avisoLegal.html`, `views/privacidad.html`, `views/terminos.html`.
2. Actualizar footers en todas las páginas HTML con los nuevos enlaces y disclaimer educativo.
3. Modificar `eventos.js` para renderizar el componente o plantilla de mapas con la interacción "Cargar mapa".
4. Añadir casilla de verificación RGPD (14+ años y aceptación de privacidad) en el formulario de registro.

## Project Structure

```text
frontend/
├── views/
│   ├── avisoLegal.html (nueva)
│   ├── privacidad.html (nueva)
│   └── terminos.html (nueva)
├── js/
│   └── eventos.js (soporte mapa bajo demanda)
└── index.html, views/*.html (footer actualizado)
```
