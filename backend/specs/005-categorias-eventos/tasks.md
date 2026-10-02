# Tasks: Etiquetas de Eventos — Eventos Culturales Puertollano

**Input**: `specs/005-categorias-eventos/spec.md` + `plan.md`

> Fases 1-3 (categoría única T069-T077) quedan superadas por el punto 2 de mejoras:
> las fases 4-6 las reemplazan con ruptura limpia.

---

## Phase 4: US1 — Etiquetas múltiples (P1, punto 2)
- [x] T129 [US1] Crear entidad `Etiqueta` + `EtiquetaRepository`; `Evento.etiquetas` `@ManyToMany` EAGER (join `evento_etiquetas`); eliminar `categoria` y el enum `CategoriaEvento`
- [x] T130 [US1] `EventoDTO`: `categoria` → `etiquetas: List<String>`; `validarEtiquetas` (desconocida → 400) + `resolverEtiquetas` (vacío → `["OTROS"]`) en `aplicar()`
- [x] T131 [US1] Test: crear con `["MUSICA","TEATRO"]` se guarda igual; sin etiquetas → `["OTROS"]`; nombre desconocido → 400

## Phase 5: US2 — Filtro ANY + US3 CRUD catálogo (P1, punto 2)
- [x] T132 [US2] `EventoRepository`: queries ANY con `DISTINCT` (`findVigentesEnPorEtiquetas`, `findDistinct…`, `findVigentesEnPorEstadoYEtiquetas`, `existsByEtiquetasId`); `GET /eventos?etiquetas=a,b` combinable con fecha/estado
- [x] T133 [US2] Test: filtro ANY, fecha+etiquetas, vacía=ausente, desconocida=`[]`
- [x] T134 [US3] `EtiquetaController`: `GET /etiquetas` público + `POST`/`PUT`/`DELETE` admin (409 duplicada/en uso, 404 inexistente, 401/403); `GET /etiquetas` en la cadena 1 de `SecurityConfig`
- [x] T135 [US3] `EtiquetaControllerTest` (13 tests) + mocks de `EtiquetaRepository` en los 3 slices de `EventoController`

## Phase 6: Migración + docs (punto 2)
- [x] T136 `DataInitializer`: seed de las 7 etiquetas + migración de `categoria` legacy (nativa) en el backfill + seeds con etiquetas
- [x] T137 Actualizar `docs/api-contract.md` (modelo `etiquetas`, `?etiquetas=`, CRUD `/etiquetas`)
- [x] T138 `./mvnw test` completo en verde (141/141)