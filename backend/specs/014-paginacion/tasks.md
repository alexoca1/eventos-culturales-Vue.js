# Tasks: Paginación en servidor — Eventos Culturales Puertollano

**Input**: `specs/014-paginacion/spec.md` + `plan.md`

---

## Phase 1: Repositorio (P1)
- [x] T138 Añadir `Pageable` como último parámetro a los 9 métodos de `EventoRepository` listados en `plan.md` (los 4 de búsqueda por fecha exacta se dejan sin cambios)

**Checkpoint**: el repositorio compila con los 9 métodos migrados.

---

## Phase 2: Controller (P1)
- [x] T139 Añadir `@Value("${app.paginacion.size:10}") int pageSize` en `EventoController`
- [x] T140 Añadir `@RequestParam(defaultValue = "0") int page` en `findAll()` y construir `PageRequest.of(page, pageSize)` para pasarlo a cada una de las 9 ramas de `GET /eventos`
- [x] T141 Igual que T140 para `GET /eventos/mios` (`findByCreadoPorOrderByIdAsc`)
- [x] T142 Confirmar que `GET /eventos/pendientes` sigue devolviendo `List<Evento>` sin ningún cambio
- [x] T143 Añadir `app.paginacion.size=10` en `application.properties`

**Checkpoint**: `GET /eventos?page=0` devuelve `Page<Evento>` con `content`, `totalElements`, `totalPages`, `last`.

---

## Phase 3: Tests (P1)
- [x] T144 [P] Test en `EventoControllerPublicTest`: con más de 10 eventos sembrados en la BD de test, `GET /eventos?futuros=true` devuelve `totalElements` correcto y `content.size() <= 10`; `?page=N` devuelve la página correcta; `?page=99` devuelve `content=[]` sin error; `GET /eventos/mios?page=0` pagina correctamente para el organizador; `GET /eventos/pendientes` sigue devolviendo array plano sin metadatos de página

---

## Phase 4: Polish
- [x] T145 Actualizar `docs/api-contract.md`: documentar `?page=` (base 0, default 0) y el formato de respuesta paginada (`content`, `number`, `size`, `totalElements`, `totalPages`, `first`, `last`) para `GET /eventos` y `GET /eventos/mios`
- [x] T146 `./mvnw test` completo en verde (suite `001` a `014`)

## Dependencies & Execution Order
T138 → T139-T143 en paralelo → T144 → T145-T146.