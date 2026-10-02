# Tasks: Etiquetas múltiples — Frontend

**Input**: `specs/003-categorias-eventos/spec.md` + `plan.md`

> Fases 1-3 (categoría única F021-F027) quedan superadas por el punto 2 de mejoras:
> las fases 4-6 las reemplazan con ruptura limpia.

---

## Phase 4: US1 — Formulario multi-etiqueta (P1, punto 2)
- [x] F139 [US1] Checkboxes del catálogo (`etiquetasDisponibles`) en formularios de admin/organizador, ligados a `inputEtiquetas[]`, con aviso "sin marcar = Otra"
- [x] F140 [US1] DTO con `etiquetas: [...]`; `editEvento()` preselecciona las actuales; `apiToView()` mapea `etiquetas` (nombres)
- [x] F141 [US1] `useEtiquetas` (catálogo + `cargarEtiquetas` en `onMounted` de las 3 páginas con formulario/filtro)

## Phase 5: US2/US3 — Filtro ANY + gestión del catálogo (P1, punto 2)
- [x] F142 [US2] Checkboxes de filtro en ambos buscadores (`searchEtiquetas[]` → `?etiquetas=a,b`); línea de etiquetas en tarjetas y previsualización
- [x] F143 [US3] Sección "Etiquetas" en `administrador.html` (crear/renombrar/eliminar + errores duplicada/en uso); flags excluyentes con las demás secciones
- [x] F144 `node --check` en ficheros tocados; prueba manual de punta a punta

## Dependencies & Execution Order
US1 antes de US2/US3. Requiere `backend/specs/005-categorias-eventos` reescrito (T129-T138).
