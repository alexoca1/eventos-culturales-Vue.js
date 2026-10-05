# Implementation Plan: Editar email propio (frontend)

**Branch**: `023-editar-email-perfil` | **Date**: 2026-10-05

## Summary

1. Campo `perfil-email` en el formulario "Mi perfil" de las 3 vistas, precargado desde `GET /auth/perfil`.
2. Enviar `email` en `PUT /auth/perfil` y detectar si ha cambiado (comparación contra `emailGuardado`).
3. Si cambió, renovar la sesión en silencio con `useAuth().renovarSesion()` — obligatorio, porque el `sub` del JWT es el email.
4. Traducir los errores de validación con `mensajeValidacion` (el 400 de `@Email` llega como `{email: ...}`).
5. Check ejecutable `js/ui/perfil.check.js`.

## Project Structure

```text
frontend/js/
├── api/auth.js            (actualizarPerfil → mensajeValidacion)
├── composables/useAuth.js (renovarSesion expuesta en la instancia)
├── composables/usePerfil.js (perfilEmail + emailGuardado + renovación)
├── ui/perfil.check.js     (nuevo: check del flujo completo)
└── views/{administrador,organizador,usuarioEstandar}.html
                            (label + input type=email v-model.trim, tras apellidos)
```

## Por qué se renueva desde el cliente

`JwtService` emite `sub = usuario.getEmail()` y `AuthController` busca con
`findByEmail(jwt.getSubject())`. Guardado un email nuevo, el token en vigor deja de
apuntar a nadie → el siguiente request responde 500 (verificado por HTTP en vivo).

La cookie de refresh sí sirve: `RefreshToken` guarda `@ManyToOne Usuario`, no el email,
así que `POST /auth/refresh` devuelve ya un token con el `sub` correcto. Por eso basta con
llamar al `renovarSesion()` que ya existía en `useAuth` (refresh → `setSession` →
`GET /auth/perfil` → cabecera), que ahora se expone en la instancia.

Solo se renueva si el email realmente cambió: `rellenar()` guarda `emailGuardado` (lo que
hay en BD) y `guardarPerfil()` lo compara con lo que devuelve el PUT. Si no cambió, no hay
refresh y no se gasta la cookie.

## Sin ciclo peligroso

`useAuth` ya importaba a `usePerfil` (para `irAPerfil`); ahora `usePerfil` importa a
`useAuth` (para `renovarSesion`). Es seguro porque ambos son singletons perezosos y nada
se usa en la evaluación del módulo; `perfil.check.js` los importa juntos a propósito para
que un "Cannot access ... before initialization" quede cubierto por el gate.
