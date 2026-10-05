# Tasks: Accesibilidad WCAG 2.2 Nivel AA

---

## Phase 1: Paleta de Colores y Contraste (P1)

- [x] T220 Ajustar variables CSS en `frontend/css/variables.css` (`--color-primary`, `--color-bg`, badges de estado y rol) para garantizar contraste >= 4.5:1 con texto blanco/oscuro.

## Phase 2: Foco Visible y Semántica (P1)

- [x] T221 Añadir estilos CSS `:focus-visible` en `index.css` y `eventos.css` para navegación fluida por teclado.
- [x] T222 Revisar plantillas HTML (`index.html`, `usuarioEstandar.html`, `administrador.html`, `organizador.html`) asegurando `alt` en imágenes y `<label for="...">` en todos los inputs.

---

## Notas de implementación

**T220** — No hacía falta tocar `--color-bg` (sigue en `#1894ff`): lo que fallaba era el
texto encima, así que `--color-text` bajó de `#333` a `#1f1f1f` (5.27:1 sobre el azul).
El resto de tokens se oscurecieron hasta pasar 4.5:1 contra el blanco con el que se usan:

| token | antes | ratio | ahora | ratio |
|---|---|---|---|---|
| `--color-primary` | `#ff6347` | 2.95 | `#d63600` | 4.79 |
| `--color-primary-hover` | `#e53900` | 4.27 | `#ad2c00` | 6.69 |
| `--color-text` | `#333` | 4.04 (sobre el azul) | `#1f1f1f` | 5.27 |
| `--color-text-muted` | `#999` | 2.85 | `#6e6e6e` | 5.10 |
| `--color-estado-pendiente` | `#ff9800` | 2.16 | `#a85600` | 5.26 |
| `--color-estado-aprobado` | `#4caf50` | 2.78 | `#1e7a2f` | 5.41 |
| `--color-estado-rechazado` | `#f44336` | 3.68 | `#c62828` | 5.62 |
| `--color-favorito` | `#e6a817` | 2.10 | `#8a6100` | 5.54 |

`--color-secondary`, `--color-estado-eliminacion` y los tres `--color-rol-*` ya pasaban
y no se tocaron. Aparte de los tokens hubo que arreglar cinco sitios que atenuaban el
texto con `opacity` (pie completo, subtítulos con `opacity: 0.85`): al 0.85 el texto
negro sobre el azul cae a 4.28:1 y el blanco sobre el tomate a 4.13:1.

El contraste se verifica de forma automática con `node js/ui/contraste.check.js`, que
lee la paleta real de `variables.css`, calcula los 19 pares que la app pinta de verdad y
además comprueba que la regla `:focus-visible` siga existiendo en las dos hojas. Se
comprobó que **falla** si se devuelve `--color-primary` a `#ff6347`.

**T221** — Una única regla global por hoja (`outline: 3px solid var(--color-text)` con
`outline-offset: 2px`): el negro da 3:1 o más sobre el tomate, el azul y el blanco, así
que sirve en todos los fondos sin romper los `:focus-visible` puntuales que ya existían
(`.mapa-boton`, `.galeria-slot-quitar`, `.red-evento-link`) porque no fijan `outline`.
Verificado en Chrome: tras pulsar Tab, `:focus-visible` casa y se pinta el contorno.

**T222** — Las imágenes ya tenían `alt` (incluido el cartel que inyecta `apiToView`).
Lo que faltaba era la asociación de los campos: había etiquetas sin `for`, inputs sin
`id` y dos etiquetas con `for=""` (inválido). Se añadieron `id`/`for` a los ~50 campos de
las 4 plantillas; los campos sin texto visible (miniaturas de galería, selector y enlace
de cada red social, renombrar etiqueta) llevan `aria-label`. Las etiquetas que titulan
un grupo de checkboxes dejaron de ser `<label>` y pasaron a `<p class="titulo-grupo">`,
que no debe llevar `for` porque no etiqueta a un campo. Auditoría de contrapartida:
`for` muerto, campo sin etiqueta e ids duplicados = 0 en las 4 plantillas.
