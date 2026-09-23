# Tasks: Panel de Organizador y Cola de Moderación — Frontend

**Input**: `specs/004-flujo-aprobacion-organizadores/spec.md` + `plan.md`

---

## Phase 1: US1 — Login redirige según rol (P1)
- [x] F028 [US1] En `index.html`/`eventos.js`: tras login exitoso, leer `roles` de la respuesta y redirigir a `administrador.html` si incluye `ROLE_ADMIN`, a `organizador.html` si incluye `ROLE_ORGANIZADOR` (y no admin), comportamiento actual sin cambios en cualquier otro caso

**Checkpoint**: el login ya no asume que todo el mundo es admin.

---

## Phase 2: US2 — Crear con previsualización y confirmación (P1)
- [x] F029 [US2] Crear `views/organizador.html`: formulario de evento (mismos campos que el de admin: establecimiento, dirección, fecha, horaInicio/horaFin, descripción, categoría, cartel, mapa), sin campo de estado
- [x] F030 [US2] `apiToView()`: mapear `estado`, `creadoPor`, `motivoRechazo`
- [x] F031 [US2] Validación de 48h en el frontend antes de permitir pasar a previsualización (mensaje claro si no se cumple)
- [x] F032 [US2] Pantalla/estado "previsualización": reutilizar el componente visual de tarjeta de evento de `usuarioEstandar.html`, con el aviso de aprobación pendiente y los botones "Volver a editar" / "Confirmar y enviar a revisión"
- [x] F033 [US2] Al confirmar: `POST /eventos` (mismo endpoint que ya usa el admin), con manejo de errores 400 (ej. si la validación de backend igualmente falla)

**Checkpoint**: crear un evento como organizador pasa siempre por previsualización + confirmación explícita antes de tocar el backend.

---

## Phase 3: US3 — Editar con el mismo patrón (P1)
- [x] F034 [US3] Reutilizar el formulario de F029 en modo edición (precarga de datos, igual que `editEvento()` en el admin)
- [x] F035 [US3] Reutilizar la previsualización de F032 con el texto de aviso adaptado a edición
- [x] F036 [US3] Al confirmar: `PUT /eventos/{id}`

**Checkpoint**: editar como organizador sigue el mismo flujo de previsualización que crear.

---

## Phase 4: US4 — Solicitar eliminación (P1)
- [x] F037 [US4] Diálogo de confirmación de eliminación con el texto de aviso del spec (reutilizando el patrón `confirm()` ya usado en el admin, o un modal simple si se prefiere más detalle visual)
- [x] F038 [US4] Al confirmar: `DELETE /eventos/{id}`

**Checkpoint**: eliminar como organizador deja claro que es una solicitud, no un borrado inmediato.

---

## Phase 5: US5 — "Mis eventos" con estado (P1)
- [x] F039 [US5] En `organizador.html`: listar `GET /eventos/mios`, con badge de estado (Pendiente de revisión / Aprobado / Rechazado + motivo / Pendiente de eliminación) y acciones Editar/Eliminar por tarjeta

**Checkpoint**: el organizador ve el estado real de todos sus eventos en un solo sitio.

---

## Phase 6: US6 — Cola de moderación del admin (P1)
- [x] F040 [US6] Nueva sección "Pendientes de revisión" en `administrador.html`: `GET /eventos/pendientes`, distinguiendo visualmente creación/edición pendiente vs solicitud de eliminación
- [x] F041 [US6] Botón "Aprobar" → `POST /eventos/{id}/aprobar`
- [x] F042 [US6] Botón "Rechazar" → campo de motivo opcional + `POST /eventos/{id}/rechazar`

**Checkpoint**: el admin puede resolver la cola completa sin salir del panel.

---

## Phase 7: Polish
- [x] F043 `node --check js/eventos.js` sin errores
- [x] F044 Prueba manual end-to-end: login organizador → crear con previsualización → aparece en "Mis eventos" como pendiente → login admin → aparece en cola → aprobar → reaparece como Aprobado en el panel del organizador

## Dependencies & Execution Order
US1 primero (hace falta para llegar a las demás vistas). US2 antes de US3 (se reutiliza el mismo componente). US6 puede hacerse en paralelo a US2-US5 una vez existe el backend. Requiere `backend/specs/007-flujo-aprobacion-organizadores` ya implementado, y una cuenta `ROLE_ORGANIZADOR` de prueba creada manualmente (vía Swagger/curl, hasta que `006-gestion-admins` dé una UI para ello).

## Notas
- Cada prompt ≈ 1-2 historias (esta feature es grande; ver agrupación abajo).