# Tasks: Editar email propio (frontend)

---

## Phase 1: Estado y envío (P1)

- [x] T230 `usePerfil`: refs `perfilEmail` (lo que se edita) y `emailGuardado` (lo que hay en la BD, rellenada en `rellenar`), ambas expuestas en la instancia.
- [x] T231 `guardarPerfil` envía `email` en el body del `PUT /auth/perfil`.
- [x] T232 `api/auth.js` → `actualizarPerfil` traduce el error con `mensajeValidacion` en vez de solo `r.data.error` (los 400 de `@Valid` vienen como `{email: ...}`).

## Phase 2: Renovación silenciosa de la sesión (P1)

- [x] T233 `useAuth` expone `renovarSesion` en su instancia (el flujo refresh → `setSession` → `GET /auth/perfil` ya existía).
- [x] T234 `guardarPerfil`, tras un 200, compara `r.perfil.email` con `emailGuardado`: si cambió, actualiza `emailGuardado` y `await useAuth().renovarSesion()`; si no cambió, no renueva.

## Phase 3: Formulario (P1)

- [x] T235 Input `perfil-email` (`type="email"`, `v-model.trim`) + label "Correo electrónico (obligatorio, con este inicias sesión)" en las 3 vistas, después de apellidos.

## Phase 4: Verificación (P1)

- [x] T236 `js/ui/perfil.check.js`: ciclo de imports `usePerfil` ↔ `useAuth`, renovación solo cuando el email cambia, token nuevo en `store.token`, cabecera actualizada y `email` presente en el body del `PUT`.
- [x] T237 Gates: `node --check` (30 ficheros) + los 5 checks en verde.

## Phase 5: Errores que no son "sesión caducada" (P1, hallado en la prueba de Chrome)

- [x] T238 `actualizarPerfil` pasa `authFail: "return"`: con el `"redirect"` por defecto,
  `http.js:51` trataba el **403 de la cuenta demo** como un 401 → `alert("Sesión caducada…")`
  + token borrado + salto a `index.html`. El perfil echaba al usuario en vez de enseñar el
  mensaje del backend (incumplía SC-003).
- [x] T239 `guardarPerfil` cierra sesión con `useAuth().logout()` cuando `r.status === 401`,
  para no perder el cierre de sesión que hacía `http.js` por defecto.
- [x] T240 `perfil.check.js` cubre ambos caminos con una respuesta de PUT forzada:
  403 → mensaje al campo, 0 alertas, token intacto; 401 → token borrado y `POST /auth/logout`.
  Verificado que el check **falla** si se quita el `authFail: "return"`.
- [x] T241 Prueba en Chrome real (Chromium headless vía `playwright-core` fuera del repo):
  login → campo precargado → 409 en rojo → 400 en rojo → cambio real con "Guardado." y
  token renovado (el token viejo da 500) → recarga con la sesión viva → 403 de la demo en
  rojo sin desloguear → correo original restaurado.

---

## Notas de implementación

- **Renovar es obligatorio, no cosmético**: con el token anterior a la petición,
  `GET /auth/perfil` responde 500 (verificado por HTTP en vivo). Sin esta llamada, el
  usuario vería errores en cualquier operación hasta que caducara el token (15 min).
- **El doble ref evita un refresh por guardado**: `perfilEmail` es lo que se está editando
  y `emailGuardado` lo que hay en BD; comparando contra `emailGuardado` (no contra
  `perfilEmail`, que ya vale lo nuevo) guardar sin tocar el correo no gasta la cookie.
- **Ciclo `usePerfil` ↔ `useAuth`**: seguro por ser singletons perezosos, y
  `perfil.check.js` lo importa juntos para que un "Cannot access ... before initialization"
  quede cubierto.
- **Si la renovación falla**, `renovarSesion()` ya limpia la sesión en ese caso (es el
  comportamiento preexistente para cualquier refresh rechazado): el usuario vuelve al
  login en lugar de quedarse con un token que responde 500.
