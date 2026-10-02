# Tasks: Dashboards por rol — Frontend

**Input**: `specs/015-dashboards-por-rol/spec.md` + `plan.md`

---

## Phase 1: Panel por rol (P1)
- [x] F162 `usePanel` (contadores por rol vía api + `abrirPanel`/`cerrarPanel` con ocultado mutuo); `irAPanel()` en `useAuth` (reemplaza `panelUrl`)
- [x] F163 Secciones dashboard en las 3 vistas (resumen + accesos; cartelera pública en organizador) + exponer en entrypoints
- [x] F164 Ocultar `showPanel` al abrir las otras 8 secciones (y viceversa)
- [x] F165 `node --check`; prueba manual (contadores, abrir/cerrar)
- [x] F166 Añadir enlace 'Ver cartelera pública' en el dashboard del admin (mismo patrón que el del organizador)
