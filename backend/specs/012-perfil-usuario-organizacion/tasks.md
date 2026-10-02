# Tasks: Perfil editable + datos de organización — Backend

**Input**: `specs/012-perfil-usuario-organizacion/spec.md` + `plan.md`

---

## Phase 1: US1/US2/US3 (P1)
- [x] T143 Campos org en `Usuario` + `telefono` `@NotBlank` en `RegisterRequest` + 4 campos opcionales en `ActualizarUsuarioRequest` + `ActualizarPerfilRequest` nuevo
- [x] T144 `PUT /auth/perfil` (nombre/apellidos/teléfono no vacíos; org solo si organizador y completo; resto ignorado) + `mapaPerfil()` compartido con `GET`
- [x] T145 Regla organizador en `PUT /auth/usuarios/{id}` (400 si el estado final queda incompleto; DESPUÉS del 409) + aplicar campos + exponerlos en `GET /auth/usuarios`
- [x] T146 7 tests (`register_sinTelefono_400`, perfil x4, promoción x2) + adaptar `ascenderAOrganizador_200`
- [x] T147 Contrato (`register`, `PUT /usuarios`, `PUT /perfil`, listado) + `./mvnw test` en verde (148/148)
