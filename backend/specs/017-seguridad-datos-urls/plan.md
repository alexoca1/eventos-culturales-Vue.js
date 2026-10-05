# Implementation Plan: Seguridad de datos, URLs y sanitización

**Branch**: `017-seguridad-datos-urls` | **Date**: 2026-10-03

## Summary

1. Crear DTOs de respuesta `EventoResponseDTO` y `UsuarioPublicoDTO` para evitar la fuga de datos sensibles en `GET /eventos`.
2. Validar URLs con regex `^(https?://.+)?$` en `EventoDTO` y `RedSocialDTO`.
3. Fortalecer `RegisterRequest` (min 8 chars, mayúscula, minúscula, número) y añadir `RegistrationRateLimitFilter`.
4. Inspeccionar magic bytes en `EventoController.validarArchivo()`.
5. Actualizar `DataInitializer` para cambiar establecimientos de prueba por sitios públicos ficticios/culturales.

## Project Structure

```text
backend/src/main/java/com/eventos/culturales/
├── dto/
│   ├── CreadoPorResponseDTO.java (nuevo record)
│   ├── EventoResponseDTO.java (nuevo record + mapeador static)
│   ├── EventoDTO.java (añadidos @Pattern en URLs)
│   ├── RedSocialDTO.java (añadido @Pattern en url)
│   └── RegisterRequest.java (actualizada validación password)
├── config/
│   ├── RegistrationRateLimitFilter.java (nuevo filtro)
│   ├── SecurityConfig.java (registro del filtro)
│   └── DataInitializer.java (sustitución de semillas reales)
├── controller/
│   └── EventoController.java (retorno de EventoResponseDTO + magic bytes)
└── src/test/java/...
```
