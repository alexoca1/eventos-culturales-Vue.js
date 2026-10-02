# Tasks: Sistema de variables CSS — Frontend

**Input**: `specs/010-css-variables/spec.md` + `plan.md`

---

## Phase 1: Fichero de variables
- [x] F099 Crear `frontend/css/variables.css` con todas las custom properties de la tabla de `plan.md`, en `:root`

## Phase 2: Refactor de las 2 hojas de estilo existentes
- [x] F100 En `eventos.css`, reemplazar cada valor hardcodeado de la tabla por su `var(--...)` correspondiente, sin cambiar ningún valor
- [x] F101 En `index.css`, reemplazar cada valor hardcodeado por su `var(--...)`, y eliminar la declaración muerta `background-color: #f4f4f4` de `body`

## Phase 3: Enlazar en las 4 páginas
- [x] F102 Añadir `<link rel="stylesheet" href="css/variables.css">` (o `../css/variables.css` según la página) ANTES del `<link>` a `eventos.css`/`index.css` existente, en las 4 páginas

## Phase 4: Verificación
- [x] F103 Comparación visual manual de las 4 páginas antes/después (o descripción detallada de qué se verificó, si no se puede interactuar con el navegador)

## Notas
- Un solo prompt cubre toda la feature (es mecánica y de bajo riesgo).