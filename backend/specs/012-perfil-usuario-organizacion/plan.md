# Implementation Plan: Perfil editable + datos de organización — Backend

**Branch**: `012-perfil-usuario-organizacion` | **Date**: 2026-09-24 | **Spec**: `specs/012-perfil-usuario-organizacion/spec.md`

## Summary
4 columnas nuevas en `Usuario` + `PUT /auth/perfil` + regla "organizador completo" en admin y perfil + `@NotBlank` en teléfono de registro.

## Technical Context
**Language/Version**: Java 21, sin dependencias nuevas.

## Constitution Check
- [x] IV. Tests obligatorios (7 nuevos).
- [x] V. Seguridad explícita: `PUT /perfil` autenticado (cadena 2), sin tocar rol/estado.

## Project Structure
```text
backend/.../entities/Usuario.java            # +4 campos
backend/.../dto/RegisterRequest.java          # telefono @NotBlank
backend/.../dto/ActualizarUsuarioRequest.java # +4 campos opcionales
backend/.../dto/ActualizarPerfilRequest.java  # nuevo
backend/.../controller/AuthController.java    # PUT /perfil + regla org + mapaPerfil
```

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Ninguna | — | — |
