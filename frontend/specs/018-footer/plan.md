# Implementation Plan: Footer global — Frontend

**Branch**: `018-footer` | **Date**: 2026-09-25

## Summary
Un bloque `<footer>` idéntico añadido dentro del `<div id="eventos">` de las
4 páginas, con estilos en `eventos.css` usando variables ya definidas.
SVGs inline para iconos (sin dependencias externas).

## Datos inventados (fijos, iguales en las 4 páginas)
- **Organización**: Asociación Cultural Puertollano
- **Dirección**: Calle Mayor, 12 — 13500 Puertollano, Ciudad Real
- **Teléfono**: +34 926 123 456
- **Email**: info@culturalpuertollano.es
- **Redes**: Facebook, Instagram, X (Twitter), YouTube → todos apuntan a `#`
- **Copyright**: © 2024–2025 Asociación Cultural Puertollano

## Estructura del footer (3 columnas → 1 en móvil)
┌─────────────────────────────────────────────┐
│ Asociación Cultural Puertollano │
│ ──────────────────────────────── │
│ [Col 1: nombre + tagline] │
│ [Col 2: contacto (dir/tel/email)] │
│ [Col 3: iconos redes sociales] │
│ ──────────────────────────────── │
│ © 2024–2025 Asociación Cultural Puertollano│
└─────────────────────────────────────────────┘


## Project Structure
```text
frontend/css/eventos.css              # estilos .footer, .footer-cols, .footer-col,
                                      # .footer-social, .footer-copy, @media 480px
frontend/index.html                    # + <footer>
frontend/views/administrador.html       # + <footer>
frontend/views/organizador.html          # + <footer>
frontend/views/usuarioEstandar.html       # + <footer>
```

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| SVG inline | Iconos sin dependencia externa | Emojis o texto plano no son reconocibles como iconos de redes sociales; librería externa viola la política del proyecto |