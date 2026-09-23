# Implementation Plan: Gestión de Usuarios (roles y estado) — Eventos Culturales Puertollano

**Branch**: `009-gestion-admins` | **Date**: 2026-09-18 | **Spec**: `specs/009-gestion-admins/spec.md`

## Summary
Un endpoint nuevo (`PUT /auth/usuarios/{id}`) que permite a un admin editar `roles` y `enabled` de cualquier usuario, con protección explícita contra auto-bloqueo. Se completa `GET /auth/usuarios` para incluir `enabled`.

## Technical Context
**Language/Version**: Java 21, sin dependencias nuevas.
**Storage**: sin cambios de esquema (`roles` y `enabled` ya existen en `Usuario`).
**Testing**: JUnit 5 + MockMvc + Spring Security Test.

## Constitution Check
- [x] III. Sin over-engineering: no se crea un endpoint de creación nueva para organizadores; se reutiliza `register` + este endpoint de edición.
- [x] V. Seguridad explícita: `@PreAuthorize("hasRole('ADMIN')")` + comprobación explícita de "no es mi propio id comprometiendo mi acceso" en el cuerpo del método.

## Project Structure

### Documentation (this feature)
```text
specs/009-gestion-admins/
├── plan.md
└── tasks.md
```

### Source Code
```text
backend/src/main/java/.../dto/ActualizarUsuarioRequest.java   # record nuevo: { roles: Set<String>, enabled: boolean }
backend/src/main/java/.../controller/AuthController.java       # + PUT /usuarios/{id}; listarUsuarios() + enabled
backend/src/test/java/.../controller/AuthAdminTest.java        # tests nuevos
docs/api-contract.md                                            # documentar el endpoint nuevo y el campo enabled en el listado
```

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Ninguna | — | — |