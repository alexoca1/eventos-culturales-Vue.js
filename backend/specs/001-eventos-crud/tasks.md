# Tasks: Backend REST Eventos Culturales Puertollano

**Input**: `specs/001-eventos-crud/spec.md` + `plan.md` (Boot 4.0.0, Java 21, MySQL Aiven, auth patrón padel, 2 cadenas Security, Dockerfile Render, springdoc, JUnit5/Mockito/MockMvc)

**Tests**: Obligatorios por constitución IV (Mockito para servicios, MockMvc + Security Test para autorización).

## Formato: `[ID] [P?] [Story] Descripción`

---

## Phase 1: Setup — Esqueleto Maven + Boot

**Purpose**: Proyecto compilable y arrancable, espejo de padel-backend.

- [x] T001 Inicializar `backend/pom.xml` (parent Boot 4.0.0, Java 21, deps: data-jpa, webmvc, security, oauth2-resource-server, validation, mysql-connector-j, jjwt 0.12.6 x3, lombok, springdoc-openapi-starter-webmvc-ui, devtools, test + webmvc-test + security-test)
- [x] T002 [P] Crear `EventosCulturalesApplication.java` en `com.eventos.culturales` con `@EnableJpaAuditing`
- [x] T003 [P] Crear `src/main/resources/application.properties` con placeholders `${DB_URL:...}`, `${DB_USERNAME:root}`, `${DB_PASSWORD:}`, `${PORT:8081}`, `${JWT_SECRET:...}` (igual que padel)
- [x] T004 Crear `Dockerfile` multi-stage (build JDK 21 → runtime JRE 21, `-Xmx400m`, expone `${PORT:8081}`)
- [x] T005 Verificar `./mvnw compile` en verde

**Checkpoint**: `compile` OK, app arranca (aunque sin entidades aún).

---

## Phase 2: Foundational — Auth + base (BLOQUEANTE)

**Purpose**: Infra que todo lo demás necesita. Nada de historias hasta terminarla.

- [x] T006 [P] Entidad `Usuario` + `UsuarioRepository` (copia padel: email único, BCrypt, roles, enabled)
- [x] T007 [P] Entidad `RefreshToken` + `RefreshTokenRepository` (copia padel: token único, expiry, revoked)
- [x] T008 `JwtSecretKeyProvider` (fail-fast si `JWT_SECRET` falta/corta, igual que padel)
- [x] T009 `JwtService` (access 15 min, claim `roles`, issuer propio) + `CustomUserDetailsService`
- [x] T010 `RefreshTokenService` (create + rotate con reuse-detection + revoke + purge, constructor injection, sin `@Autowired` en campos)
- [x] T011 `SecurityConfig` con DOS cadenas: cadena pública (`/auth/login`, `/auth/refresh`, `/auth/logout`, `/auth/register`, `GET /eventos/**`, swagger) y cadena autenticada (resto), stateless, CORS por env, JWT converter claim `roles`
- [x] T012 `AuthController` (login/refresh/logout/perfil, cookie HttpOnly `SameSite`/`secure` por env) — SIN `/register-admin` público
- [x] T013 `DataInitializer` (siembra primer admin con `ADMIN_SEED_PASSWORD` + BCrypt si BD vacía)
- [x] T014 `GlobalExceptionHandler` (400 validación, 400 fecha/fichero malformado, 403 denegado, 500 genérico)
- [x] T015 Test `RefreshTokenServiceTest` (Mockito, estilo padel: rotación OK, reuse → invalida todo, expirado → 401)

**Checkpoint**: `./mvnw test` en verde; login → access + cookie refresh funciona.

---

## Phase 3: US1 — Invitado consulta eventos por fecha (P1) 🎯 MVP

**Goal**: `GET /eventos?fecha=` público devuelve TODOS los del día.

**Independent Test**: `GET /eventos?fecha=2026-10-01` sin token → 200 con lista completa; día vacío → 200 `[]`.

- [x] T016 [P] [US1] Test `EventoControllerPublicTest` (MockMvc sin token: GET por fecha OK, GET lista OK, GET id inexistente 404)
- [x] T017 [P] [US1] Entidad `Evento` (`id`, `establecimiento @NotBlank`, `direccion @NotBlank`, `fecha LocalDate @NotNull` indexada NO unique, `descripcion`, `cartelUrl`, `mapaEmbed`, `@CreationTimestamp/@UpdateTimestamp`)
- [x] T018 [US1] `EventoRepository` (`findByFechaOrderByIdAsc`, `findAllByOrderByFechaAscIdAsc`) (depende de T017)
- [x] T019 [US1] `EventoController` solo lectura: `GET /eventos?fecha=` + `GET /eventos/{id}` públicos (depende de T018)
- [x] T020 [US1] Validación formato fecha query inválida → 400 claro (handler `MethodArgumentTypeMismatch`, sin él caía en el 500 genérico)

**Checkpoint**: US1 funciona sola: consulta pública por fecha con varios eventos/día.

---

## Phase 4: US2 — Admin crea/edita/elimina (P1)

**Goal**: Mutaciones solo `ROLE_ADMIN` con `@PreAuthorize` explícito.

**Independent Test**: Sin token → 401/403; con `ROLE_USER` → 403; con `ROLE_ADMIN` → 201/200/204.

- [x] T021 [US2] Test `EventoControllerAuthTest` (MockMvc + Security Test: POST/PUT/DELETE anónimo 401, USER 403, ADMIN 201/200/204; validación 400)
- [x] T022 [P] [US2] DTO `EventoDTO` (record con `@NotBlank/@NotNull`, validación `mapaEmbed` contiene `google.com/maps/embed` si viene)
- [x] T023 [US2] `POST /eventos` + `PUT /eventos/{id}` + `DELETE /eventos/{id}` con `@PreAuthorize("hasRole('ADMIN')")` explícito (después pasados a multipart en T034; depende de T018, T022)
- [x] T024 [US2] `DELETE /eventos` múltiple por ids
- [x] T025 [US2] Seed de 5 eventos de ejemplo Puertollano

**Checkpoint**: US1 + US2 completas: invitado lee, admin gestiona, resto rechazado.

---

## Phase 5: US3 — Admins sin auto-registro público (P2)

**Goal**: Cerrar el agujero de padel: ningún endpoint público otorga `ROLE_ADMIN`.

**Independent Test**: `POST /auth/register` nunca crea admin; crear admin exige token ADMIN; arranque en BD vacía deja admin operativo.

- [x] T026 [US3] Test `AuthAdminTest` (MockMvc: register público con rol admin → ignorado/falla; crear admin sin token → 401; con token ADMIN → 201)
- [x] T027 [US3] `POST /auth/register` público solo crea `ROLE_USER`
- [x] T028 [US3] `POST /auth/usuarios-admin` (o similar) con `@PreAuthorize("hasRole('ADMIN')")` para crear admins adicionales

**Checkpoint**: Las 3 historias funcionan; seguridad validada por tests.

---

## Phase 6: Polish — Contrato, docs y despliegue

**Purpose**: Acabado profesional (constitución: claridad + springdoc).

- [x] T029 [P] springdoc-openapi configurado + `/swagger-ui` accesible; `docs/api-contract.md` generado/actualizado desde el OpenAPI
- [x] T030 [P] `quickstart.md` (env Aiven, `JWT_SECRET` con `openssl rand -base64 32`, run local, Docker Render, `./mvnw test`)
- [x] T031 CORS producción (origen frontend real) + cookie `secure=true` en prod vía propiedad
- [x] T032 `./mvnw test` final en verde (38 tests) + revisión constitución (I–VII) + limpieza

---

## Phase 7: Carteles WebP display + HD en BD (post-MVP, implementado 2026-09-17)

**Goal**: Fotos que sobreviven a despliegues con disco efímero (Render) sin cuentas externas; listas ligeras + lightbox HD.

- [x] T033 `Evento.cartel/cartelHd` (`LONGBLOB`) + content types; `cartelUrl` solo URLs externas
- [x] T034 `POST/PUT /eventos` multipart (`evento` JSON + `file`/`fileHd`; JPEG/PNG/WebP ≤ 2 MB, sin GIF); sin ficheros conserva carteles
- [x] T035 `GET /eventos/{id}/cartel` y `/cartel-hd` públicos (bytes / 302 a URL / 404)
- [x] T036 Seed con HD + display reescalado en Java (stdlib) + backfill idempotente; `Dockerfile` con `-Djava.awt.headless=true`
- [x] T037 Tests: multipart doble parte, GIF → 400, `/cartel-hd` 200/404
- [x] T038 `app.cookie.same-site` configurable (`Lax` local / `None` prod cross-site Netlify↔Render)

**Checkpoint**: 38/38 en verde; verificado en vivo (`/cartel` 22 KB, `/cartel-hd` 105 KB).

---

## Dependencies & Execution Order

- Phase 1 → Phase 2 (bloqueante) → US1 → US2 → US3 → Polish.
- US1 y US2 son P1: haz US1 antes (te da el MVP consultable), luego US2 encima.
- T010 prohíbe `@Autowired` en campos (constitución I); T023 exige `@PreAuthorize` explícito (constitución V).
- Paralelo real: T002/T003, T006/T007, T016/T017, T022 independiente de T021, T029/T030.

## Notas

- Cada prompt tuyo ≈ 1–2 tareas (ej. "haz T001–T005", "haz T006–T010"...).
- Commit tras cada tarea o grupo lógico.
- Si alguna tarea choca con padel-backend, manda padel (probado) salvo que spec/constitución diga lo contrario (ej. fecha NO unique, sin register-admin público).
- Lecciones de los tests slice `@WebMvcTest` (aplicar a futuros controller tests): el slice NO carga la `SecurityConfig` propia → `@Import` explícito (+ `JwtService`, `JwtSecretKeyProvider`); `@EnableJpaAuditing` rompe el slice → `@MockitoBean JpaMetamodelMappingContext`; `@WithMockUser` no se propaga con resource-server JWT → usar post-processor `jwt()` con authorities.
