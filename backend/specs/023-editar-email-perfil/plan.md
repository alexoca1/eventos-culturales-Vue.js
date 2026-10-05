# Implementation Plan: Editar email propio

**Branch**: `023-editar-email-perfil` | **Date**: 2026-10-05

## Summary

1. Añadir `email` con `@Email` a `ActualizarPerfilRequest` y `@Valid` al `PUT /auth/perfil`.
2. Validar en el controller: `trim()` defensivo, no-op si no cambia, 403 en la cuenta demo y 409 si ya está en uso, antes de `save` (el formato lo corta `@Email` antes de entrar en el método).
3. Exponer `DEMO_EMAIL` como constante pública para no duplicar el literal.
4. En el frontend: campo `email` en las 3 formularios de perfil y renovación silenciosa de la sesión cuando cambia.

## Project Structure

```text
backend/src/main/java/com/eventos/culturales/
├── config/
│   └── DemoAccountProtectionFilter.java (DEMO_EMAIL → public static final)
├── controller/
│   └── AuthController.java (@Valid + bloque de email en actualizarPerfil)
├── dto/
│   └── ActualizarPerfilRequest.java (campo email con @Email)
└── src/test/java/com/eventos/culturales/controller/AuthAdminTest.java (5 tests)

frontend/js/
├── api/auth.js            (mensajeValidacion en actualizarPerfil)
├── composables/useAuth.js (renovarSesion expuesta)
├── composables/usePerfil.js (perfilEmail + emailGuardado + renovación)
├── ui/perfil.check.js     (nuevo check)
└── views/{administrador,organizador,usuarioEstandar}.html (input de email)
```

## Por qué la renovación es del cliente

El `sub` del JWT es el email (`JwtService` → `subject(usuario.getEmail())`) y los controllers
buscan con `findByEmail(jwt.getSubject())`. Cambiado el email, el token en vigor ya no
identifica a nadie → 500 (verificado en vivo). La cookie de refresh sí sigue sirviendo
porque `RefreshToken` guarda `@ManyToOne Usuario`, no el email, así que
`POST /auth/refresh` emite un token nuevo con el `sub` correcto sin tocar el backend.
El frontend aprovecha `useAuth.renovarSesion()` ya existente: refresh → `setSession` →
`GET /auth/perfil` → cabecera actualizada.
