# Tasks: Gestión de Usuarios (roles y estado) — Eventos Culturales Puertollano

**Input**: `specs/009-gestion-admins/spec.md` + `plan.md`

---

## Phase 1: US1 — `enabled` en el listado (P1)
- [x] T103 [US1] Test: `GET /auth/usuarios` incluye `enabled` en cada elemento
- [x] T104 [US1] Añadir `datos.put("enabled", u.getEnabled())` en `listarUsuarios()`

**Checkpoint**: el listado ya refleja quién está activo/inactivo.

---

## Phase 2: US2 — Editar roles y estado (P1)
- [x] T105 [US2] Crear `ActualizarUsuarioRequest` (record: `roles: Set<String>`, `enabled: boolean`)
- [x] T106 [US2] Test: ascender `ROLE_USER`→`ROLE_ORGANIZADOR`; desactivar una cuenta (login posterior falla); `roles` vacío o inválido → 400; llamada de un no-admin → 403
- [x] T107 [US2] `PUT /auth/usuarios/{id}` en `AuthController`, `@PreAuthorize("hasRole('ADMIN')")`: valida `roles` no vacío y dentro de `ROLE_USER/ROLE_ORGANIZADOR/ROLE_ADMIN`, actualiza y guarda

**Checkpoint**: un admin puede ascender usuarios y desactivar cuentas.

---

## Phase 3: US3 — Protección de auto-bloqueo (P1)
- [x] T108 [US3] Test: admin intenta desactivarse a sí mismo → 409; intenta quitarse su propio `ROLE_ADMIN` → 409; se edita a sí mismo sin comprometer su propio acceso → se acepta
- [x] T109 [US3] En el mismo endpoint: comparar el `id` del path con el `id` del usuario autenticado (extraído del JWT); si coincide y el resultado final no incluiría `ROLE_ADMIN` o `enabled=false`, devolver 409 antes de guardar

**Checkpoint**: ningún admin puede quedarse fuera del sistema por accidente.

---

## Phase 4: Polish
- [x] T110 Actualizar `docs/api-contract.md`: `PUT /auth/usuarios/{id}` y el campo `enabled` en `GET /auth/usuarios`
- [x] T111 `./mvnw test` completo en verde (suite `001` a `009`)

## Phase 5: Rol único (punto 1 de mejoras)
- [x] T112 [US2] Validación "exactamente un rol" en `actualizarUsuario`: `req.roles().size() != 1` → 400 (el contrato `roles` array-de-1 no cambia)
- [x] T113 [US2/US3] Tests: `actualizarUsuario_variosRoles_400` nuevo; `actualizarUsuario_propioSinRiesgo_200` adaptado (conservar el propio rol en vez de añadir otro)

## Dependencies & Execution Order
US1 → US2 → US3 (US3 depende de que el endpoint de US2 ya exista para poder protegerlo). T110-T111 al final.

## Notas
- Cada prompt ≈ 1 historia completa.