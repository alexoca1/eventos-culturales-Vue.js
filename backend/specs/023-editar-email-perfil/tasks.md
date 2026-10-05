# Tasks: Editar email propio

---

## Phase 1: DTO y validación en el endpoint (P1)

- [x] T230 Añadir `email` con `@Email(message = "El correo debe ser válido")` a `ActualizarPerfilRequest` (sin `@NotBlank`: null = no tocar).
- [x] T231 `@Valid @RequestBody` en `AuthController.actualizarPerfil` + bloque de email: `trim()`, no-op si no cambia (case-insensitive), 403 en la cuenta demo y 409 si el correo ya está en uso, todo antes de `save`.
- [x] T232 `DemoAccountProtectionFilter.DEMO_EMAIL` pasa a `public static final` y se reutiliza desde el controller (un solo literal).

## Phase 2: Tests (P1)

- [x] T233 `AuthAdminTest`: `perfil_cambiaEmail_200`, `perfil_emailYaEnUso_409`, `perfil_emailInvalido_400`, `perfil_mismoEmailOtroCaso_sinCambiar_200`, `perfil_cuentaDemo_noCambiaEmail_403`.

## Phase 3: Frontend — campo y renovación de sesión (P1)

- [x] T234 `usePerfil`: refs `perfilEmail` y `emailGuardado`, `email` en el body del PUT y `useAuth().renovarSesion()` cuando la respuesta trae un email distinto al guardado.
- [x] T235 `useAuth`: exponer `renovarSesion` en la instancia (ya existía, solo faltaba exportarla).
- [x] T236 Input `perfil-email` (`type="email"`, `v-model.trim`) en las 3 vistas, después de apellidos.
- [x] T237 `api/auth.js`: `actualizarPerfil` usa `mensajeValidacion` (los 400 de `@Valid` vienen como `{email: ...}`, no `{error: ...}`).

## Phase 4: Verificación (P1)

- [x] T238 Gates: `./mvnw test` → 226/226; `node --check` (30 ficheros); `galeria/vistas/imagenes/contraste/perfil.check.js` en verde.
- [x] T239 Verificación en vivo por HTTP contra el backend: cambio de email, formato inválido 400, duplicado 409, no-op con mayúsculas 200, demo 403, token previo → 500, refresh → token nuevo con el email correcto, login nuevo 200 / antiguo 401.

---

## Notas de implementación

- **La renovación es del cliente y no es opcional**: con el token anterior a la petición,
  `findByEmail(jwt.getSubject())` no encuentra a nadie y `GlobalExceptionHandler` devuelve
  500. Comprobado en vivo: `GET /auth/perfil` con el token viejo tras cambiar el email → 500.
- **La cookie de refresh no se ve afectada**: `RefreshToken.usuario` es una relación JPA al
  `Usuario`, no una cadena de email, así que `POST /auth/refresh` ya emite el `sub` nuevo.
  Ninguna tabla guarda el email desnormalizado (comprobado: solo `Usuario.email` y
  `Usuario.encargadoEmail`), por lo que el cambio no rompe relaciones.
- **Se prefiere `@Valid` + `@Email` a un `isBlank` manual**: el resto del endpoint valida a
  mano y devuelve `{"error": ...}`, pero `register` ya usaba la vía de Hibernate Validator con
  `{campo: mensaje}`. Como `api/auth.js` ahora traduce ambos con `mensajeValidacion`, el
  formulario muestra el motivo real en los dos casos en lugar de "No se pudo guardar".
- **Ciclo de imports `usePerfil` ↔ `useAuth`**: seguro porque ambos son singletons perezosos
  y nada se usa en la evaluación del módulo; `perfil.check.js` lo ejercita importándolos
  juntos y fallaría con "Cannot access ... before initialization" si dejara de serlo.
- **La cuenta demo solo pierde el email**, no el resto: el `DemoAccountProtectionFilter`
  sigue bloqueando únicamente `DELETE`, y el nombre/teléfono de la demo se siguen pudiendo
  editar (US2 AC2). El bloqueo vive en el controller porque es específico de este campo.
