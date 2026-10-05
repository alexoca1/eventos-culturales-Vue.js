# Implementation Plan: Derechos ARCO+ (Supresión y Portabilidad RGPD)

**Branch**: `018-rgpd-supresion-portabilidad` | **Date**: 2026-10-03

## Summary

1. `DELETE /auth/perfil`: anonimiza al usuario en `UsuarioRepository` y revoca refresh tokens en `RefreshTokenRepository`.
2. `GET /auth/perfil/exportar`: recopila datos del usuario (perfil, eventos, favoritos) en un `UserDataExportDTO` y los sirve en JSON.

## Project Structure

```text
backend/src/main/java/com/eventos/culturales/
├── dto/
│   └── UserDataExportDTO.java (nuevo record)
├── controller/
│   └── AuthController.java (endpoints DELETE /auth/perfil y GET /auth/perfil/exportar)
└── src/test/java/...
```
