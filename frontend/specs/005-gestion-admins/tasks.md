# Tasks: Pantalla de Gestión de Usuarios — Frontend

**Input**: `specs/005-gestion-admins/spec.md` + `plan.md`

---

## Phase 1: US1 — Listado de usuarios (P1)
- [x] F045 [US1] Nueva sección "Gestión de usuarios" en `administrador.html` (misma estructura visual que "Gestionar eventos")
- [x] F046 [US1] `loadUsuarios()` en `eventos.js`: `GET /auth/usuarios`, guarda en `usuarios` (data)
- [x] F047 [US1] Renderizar cada usuario: email, badges de roles, badge de estado (Activo/Desactivado)

**Checkpoint**: el admin ve la lista completa de usuarios con su rol y estado real.

---

## Phase 2: US2 — Ascender a Organizador (P1)
- [x] F048 [US2] Formulario de edición inline por usuario: checkboxes `ROLE_USER`/`ROLE_ORGANIZADOR`/`ROLE_ADMIN`
- [x] F049 [US2] `editarRolesUsuario(usuario)`: `PUT /auth/usuarios/{id}` con los roles marcados; recarga la lista al éxito; muestra el error del backend sin cerrar el formulario si falla

**Checkpoint**: ascender un usuario a organizador funciona de punta a punta.

---

## Phase 3: US3 — Activar/desactivar (P1)
- [x] F050 [US3] Botón activar/desactivar por usuario con `confirm()` previo
- [x] F051 [US3] `toggleActivo(usuario)`: `PUT /auth/usuarios/{id}` con `enabled` invertido; recarga lista; si el backend devuelve 409, muestra "No puedes desactivar tu propia cuenta"
- [x] F052 [US3] [P] Deshabilitar en la UI (sin llamar al backend) las acciones de desactivar/quitar-admin sobre la fila del propio usuario logueado (comparar email del JWT decodificado o del `/auth/perfil` ya cacheado con el email de cada fila)

**Checkpoint**: activar/desactivar funciona y el propio admin no puede bloquearse ni desde la UI.

---

## Phase 4: US4 — Crear admin (P2)
- [x] F053 [US4] Formulario simple "Crear admin" (email + password) en la misma sección
- [x] F054 [US4] `crearAdmin()`: `POST /auth/usuarios-admin`; recarga la lista al éxito

**Checkpoint**: crear un admin nuevo desde la UI funciona y aparece en la lista.

---

## Phase 5: Polish
- [x] F055 `node --check js/eventos.js` sin errores
- [x] F056 Prueba manual end-to-end: ascender un usuario de prueba a organizador → loguearse con esa cuenta → confirmar que entra a `organizador.html`

## Dependencies & Execution Order
US1 antes de US2/US3 (necesitas la lista antes de poder actuar sobre ella). US4 es independiente, puede ir en paralelo. Requiere `backend/specs/006-gestion-admins` ya implementado.

## Notas
- Cada prompt ≈ 1-2 historias.