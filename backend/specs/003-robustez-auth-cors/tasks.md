# Tasks: Robustez de Auth y CORS — Eventos Culturales Puertollano

**Input**: `specs/003-robustez-auth-cors/spec.md` + `plan.md`
**Tests**: Obligatorios por constitución IV.

## Formato: `[ID] [P?] [Story] Descripción`

---

## Phase 1: US1 — Rate limiting en login (P2)
- [x] T048 [P] [US1] Test `LoginRateLimitFilterTest`: 5 intentos fallidos → 401 cada uno; 6º intento en la ventana → 429; tras esperar la ventana, se resetea; tras un login válido, se resetea
- [x] T049 [US1] Crear `LoginRateLimitFilter` (`ConcurrentHashMap<String, List<Instant>>` por clave `email+IP`, props `app.security.login-rate-limit.max-attempts` y `.window-seconds`, default 5/60)
- [x] T050 [US1] Registrar el filtro en `SecurityConfig` antes de la cadena pública, solo sobre `POST /auth/login`
- [x] T051 [US1] Limpiar el contador de una clave en login exitoso (hook en `AuthController` o en el propio filtro tras respuesta 200)

**Checkpoint**: 6º intento de login en la ventana devuelve 429; login válido resetea el contador.

---

## Phase 2: US2 — CORS sin origen `null` en local (P2)
- [x] T052 [US2] Test que verifique que una petición con `Origin: null` no recibe `Access-Control-Allow-Origin` con la config por defecto
- [x] T053 [US2] Quitar `null` de `app.cors.allowed-origins` en `application.properties` (queda `http://localhost:*,http://127.0.0.1:*`)
- [x] T054 [US2] Verificar manualmente que el frontend servido por Apache/XAMPP (`http://localhost/...`) sigue funcionando sin cambios

**Checkpoint**: `null` ya no es un origen válido por defecto; desarrollo local vía XAMPP no se ve afectado.

---

## Phase 3: US3 — Roles como colección tipada (P3)
- [x] T055 [P] [US3] Actualizar tests que inspeccionan `getRoles()` directamente (`AuthAdminTest`, builder de `RefreshTokenServiceTest`) para esperar `Set<String>` en vez de `String`
- [x] T056 [US3] `Usuario.roles`: `String` → `Set<String>` con `@ElementCollection(fetch = FetchType.EAGER) @CollectionTable(name = "usuario_roles", joinColumns = @JoinColumn(name = "usuario_id"))`; adaptar `getAuthorities()` (ya no necesita `split(",")`)
- [x] T057 [US3] Actualizar `DataInitializer.setRoles(...)` y los dos `setRoles(...)` de `AuthController` (registro público y creación de admin) al nuevo tipo
- [x] T058 [US3] Actualizar `docs/api-contract.md`: `roles` pasa de string a array en las respuestas de `/auth/login`, `/auth/refresh` y `/auth/perfil`

**Checkpoint**: `./mvnw test` en verde con `roles` como `Set<String>`.

---

## Phase 4: Polish
- [x] T059 `./mvnw test` completo en verde (suite de `001` + `002` + nuevos de `003`)

## Dependencies & Execution Order
- US1, US2 y US3 son independientes entre sí.
- Dentro de US1: T048 (test) antes de T049-T051 (implementación), como en las features anteriores.
- Dentro de US3: T055 (tests actualizados) puede ir en paralelo con T056 mientras no se ejecuten hasta que ambos estén listos.
- T059 depende de que T048-T058 estén completas.

## Notas
- Cada prompt ≈ 1 historia completa (T048–T051, T052–T054, T055–T058).
- Commit tras cada historia.