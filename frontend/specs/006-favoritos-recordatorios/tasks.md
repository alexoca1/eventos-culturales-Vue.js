# Tasks: Registro, Login y Favoritos de Usuario Estándar — Frontend

**Input**: `specs/006-favoritos-recordatorios/spec.md` + `plan.md`

---

## Phase 1: US1 — Registro (P1)
- [x] F057 [US1] Formulario de registro en `index.html` (toggle con el login ya existente: "¿No tienes cuenta? Regístrate")
- [x] F058 [US1] `register()` en `eventos.js`: `POST /auth/register`, y tras éxito, login automático reutilizando `validateAdmin()`/la lógica de login ya existente

**Checkpoint**: crear cuenta nueva y quedar autenticado funciona de punta a punta.

---

## Phase 2: US2 — Login redirige a vista de invitado autenticada (P1)
- [x] F059 [US2] Completar la lógica de redirección de login (extendida en `004`): si no hay `ROLE_ADMIN` ni `ROLE_ORGANIZADOR` en `roles`, redirigir a `views/usuarioEstandar.html`
- [x] F060 [US2] En `usuarioEstandar.html`/`eventos.js`: al cargar, comprobar si hay token en `sessionStorage` y, si lo hay, cargar `GET /eventos/favoritos` para tener los IDs favoritos disponibles desde el principio

**Checkpoint**: loguearse como usuario estándar te deja en la vista de invitado, ahora reconocido como autenticado.

---

## Phase 3: US3 — Marcar/desmarcar favorito (P1)
- [x] F061 [US3] Icono de favorito en cada tarjeta de `usuarioEstandar.html`, con estado calculado cruzando `evento.id` contra el array de IDs favoritos ya cargado
- [x] F062 [US3] `toggleFavorito(evento)`: si no hay token, redirige a `index.html`; si lo hay, `POST` o `DELETE /eventos/{id}/favorito` según el estado actual, actualizando el array local al instante (sin esperar un nuevo `GET`)

**Checkpoint**: marcar/desmarcar favorito funciona y el invitado sin login es redirigido en vez de fallar.

---

## Phase 4: US4 — Mis favoritos (P1)
- [x] F063 [US4] Sección/pestaña "Mis favoritos" en `usuarioEstandar.html`, visible solo si hay sesión
- [x] F064 [US4] Renderiza el array de favoritos ya cargado (o lo recarga con `GET /eventos/favoritos` si se entra directamente a esa pestaña) reutilizando el componente de tarjeta existente; mensaje de "sin favoritos" si está vacío (mismo patrón visual que "no hay eventos")

**Checkpoint**: "Mis favoritos" muestra correctamente los eventos marcados, incluyendo cambios hechos en la misma sesión sin recargar.

---

## Phase 5: US5 — Cerrar sesión (P2)
- [x] F065 [US5] Control "Cerrar sesión" visible solo si hay token, en `usuarioEstandar.html`
- [x] F066 [US5] `logout()`: `POST /auth/logout`, limpia `sessionStorage`, vuelve al modo invitado sin recargar la página (oculta favoritos/Mis favoritos, quita el icono de favorito de las tarjetas o lo deja pero redirigiendo a login al pulsarlo)

**Checkpoint**: cerrar sesión deja la vista exactamente como la ve un invitado que nunca se logueó.

---

## Phase 6: Polish
- [x] F067 `node --check js/eventos.js` sin errores
- [x] F068 Prueba manual end-to-end: registrarse → marcar 2 eventos favoritos → verlos en "Mis favoritos" → cerrar sesión → confirmar que la navegación de invitado sigue intacta

## Dependencies & Execution Order
US1 → US2 → US3 → US4 → US5, en ese orden. Requiere `backend/specs/008-favoritos-recordatorios` ya implementado.

## Notas
- Cada prompt ≈ 1 historia completa.