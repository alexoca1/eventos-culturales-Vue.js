# Tasks: Footer global — Frontend

**Input**: `specs/018-footer/spec.md` + `plan.md`

---

## Phase 1: CSS
- [x] F175 Añadir en `eventos.css` los estilos del footer usando variables:
  `.footer` (fondo `var(--color-primary)`, color `var(--color-surface)`,
  padding, margin-top auto), `.footer-cols` (display flex, gap, flex-wrap),
  `.footer-col` (flex: 1, min-width), `.footer-social` (flex con gap,
  iconos SVG con hover de opacidad usando `var(--transition-fast)`),
  `.footer-copy` (text-align center, border-top, padding-top, font-size
  pequeño); `@media (max-width: 480px)` con `.footer-cols { flex-direction:
  column }`

## Phase 2: HTML (las 4 páginas)
- [x] F176 Añadir el bloque `<footer>` dentro de `<div id="eventos">` justo
  antes del cierre `</div>`, en `index.html` (ruta de CSS: `css/`,
  imágenes: `img/`)
- [x] F177 Mismo bloque en `views/administrador.html` (ruta: `../css/`,
  `../img/`)
- [x] F178 Mismo bloque en `views/organizador.html`
- [x] F179 Mismo bloque en `views/usuarioEstandar.html`

## Phase 3: Verificación
- [x] F180 Confirmar visualmente que el footer aparece en las 4 páginas,
  los iconos se ven, y en 390px no hay scroll horizontal

## Notas
- Un solo prompt cubre toda la feature.