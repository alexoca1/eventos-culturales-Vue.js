# Tasks: Próximos por defecto + anteriores en gestión — Frontend

**Input**: `specs/014-eventos-futuros-ordenados/spec.md` + `plan.md`

---

## Phase 1: US1/US2 (P1)
- [x] F158 `proximos()` en api + `proximosEventos/showProximos/cargarProximos()` en `useEventosInvitado`; `onMounted` los carga (portada si vacío); ocultar al buscar/ver favoritos; `volverBuscar` async los recarga
- [x] F159 Sección "Próximos eventos" en `usuarioEstandar.html` (misma tarjeta + favoritos + pista de búsqueda)
- [x] F160 `mostrarAnteriores` + `eventosGestion` (computed con hoy local) en `useGestionEventos`; toggle + `v-for` en gestión admin
- [x] F161 `node --check`; prueba manual (defecto, toggle, volver)
