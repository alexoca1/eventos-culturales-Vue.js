# Tasks: Buscador de eventos por texto

---

## Phase 1: Repositorio (P1)
- [x] T164 Añadir `buscarPorTextoYEstado(String q, EstadoEvento estado,
  Pageable p)` y `buscarPorTexto(String q, Pageable p)` en
  `EventoRepository` con los @Query de `plan.md`

## Phase 2: Controller (P1)
- [x] T165 Añadir `@RequestParam(required=false) String q` en `findAll()`;
  añadir helper `escaparLike(String q)` (privado, estático); añadir
  validaciones: q no vacío + (fecha!=null OR futuros) → 400; longitud < 2
  o > 100 → 400; añadir 2 ramas nuevas (admin con q / público con q)
  usando los métodos de T164

## Phase 3: Tests (P1)
- [x] T166 [P] Tests en `EventoControllerPublicTest`:
  - `?q=texto` parcial en nombre → devuelve el evento
  - `?q=texto` parcial en establecimiento → devuelve el evento
  - `?q=` vacío → comportamiento actual sin cambios
  - `?q=a` (1 char) → 400
  - `?q=texto&futuros=true` → 400
  - `?q=texto&fecha=X` → 400
  - `?q=%` → no explota, devuelve 200
  - admin autenticado + `?q=texto` → ve todos los estados

## Phase 4: Polish
- [x] T167 Actualizar `docs/api-contract.md`: documentar `?q=` (2-100
  chars, excluyente con fecha/futuros, paginado)
- [x] T168 `./mvnw test` completo en verde