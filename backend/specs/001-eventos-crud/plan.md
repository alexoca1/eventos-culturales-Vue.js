# Implementation Plan: Backend REST Eventos Culturales Puertollano

**Branch**: `001-eventos-crud` | **Date**: 2026-09-16 | **Spec**: `specs/001-eventos-crud/spec.md`

**Input**: Feature specification from `/specs/001-eventos-crud/spec.md` + user plan prompt (Boot 4.0.0, Java 21, MySQL Aiven, JWT padel-pattern, 2 cadenas Security, Dockerfile Render, springdoc, JUnit5/Mockito/MockMvc).

## Summary

Backend REST Spring Boot 4.0.0 / Java 21 / Maven para CRUD de eventos culturales con consulta pública por fecha (múltiples eventos por día) y gestión solo `ROLE_ADMIN`. Reutiliza 1:1 el modelo de auth de padel-backend (access JWT corto en body + refresh rotado con reuse-detection en cookie HttpOnly) adaptando entidades (`Pista/Reserva` → `Evento`) y cerrando el agujero `/register-admin` público. Carteles en MySQL (`LONGBLOB` display + HD) con subida multipart y servicio público `/cartel`/`/cartel-hd`.

## Technical Context

**Language/Version**: Java 21 (LTS), Maven wrapper.

**Primary Dependencies**: spring-boot-starter-data-jpa, webmvc, security, oauth2-resource-server, validation, mysql-connector-j, jjwt-api/impl/jackson 0.12.6, lombok, springdoc-openapi-starter-webmvc-ui, spring-boot-devtools (runtime).

**Storage**: MySQL en Aiven (prod) / MySQL local (dev). Spring Data JPA + Hibernate, `ddl-auto=update`. Entidades: `Evento`, `Usuario`, `RefreshToken`.

**Testing**: JUnit 5 + Mockito (servicios, estilo `RefreshTokenServiceTest` padel) + MockMvc + Spring Security Test (autorización endpoints). `./mvnw test` en verde como gate.

**Target Platform**: Docker multi-stage (build JDK 21 → runtime JRE 21, heap ~400MB `-Xmx400m`) para Render; puerto `${PORT:8081}`.

**Project Type**: REST API backend (`com.eventos.culturales`).

**Performance Goals**: CRUD local <200ms p95; consulta por fecha con índice sobre `fecha`.

**Constraints**: Heap contenedor ≤400MB; access JWT 15min / refresh 7 días; cookie `secure` + `SameSite` configurables por env (`Lax` local / `None`+`Secure` en prod cross-site); JWT_SECRET fail-fast desde env; carteles JPEG/PNG/WebP ≤ 2 MB por parte (sin GIF).

**Scale/Scope**: ~10 entidades/endpoints; 1 tabla nueva (`eventos`) + reutilización `usuario`/`refresh_token`.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] I. Constructor injection: `@RequiredArgsConstructor` en controllers/services/config; prohibido `@Autowired` en campos (padel `ReservasController` se corrige aquí).
- [x] II. Stdlib-first: `Objects.requireNonNull`, `String.isBlank`, records para DTOs.
- [x] III. Sin ServiceImpl en CRUD: `EventoController` → `EventoRepository` directo; service solo para `JwtService`/`RefreshTokenService`.
- [x] IV. Tests: `RefreshTokenServiceTest` (Mockito) + `EventoControllerAuthTest`, `EventoControllerPublicTest`, `AuthAdminTest` (MockMvc + Security Test) obligatorios.
- [x] V. Seguridad explícita: 2 cadenas (pública: `/auth/login`, `/auth/refresh`, `/auth/logout`, `GET /eventos/**`; resto autenticado) + `@PreAuthorize("hasRole('ADMIN')")` en POST/PUT/DELETE.
- [x] VI. Fail-fast JWT_SECRET (provider como `JwtSecretKeyProvider` padel) + `${VAR:default}` solo dev local.
- [x] VII. BCrypt siempre; seed admin por env `ADMIN_SEED_PASSWORD`.

## Project Structure

### Documentation (this feature)

```text
specs/001-eventos-crud/
├── plan.md              # This file
├── research.md          # Phase 0: padel-pattern, springdoc, Aiven/Render notas
├── data-model.md        # Phase 1: Evento/Usuario/RefreshToken
├── quickstart.md        # Phase 1: env, run local, Docker, test
├── contracts/           # Phase 1: openapi.yaml (eventos + auth)
└── tasks.md             # Phase 2 (/speckit.tasks, NOT here)
```

### Source Code (repository root = `backend/`)

```text
backend/
├── pom.xml
├── Dockerfile                           # multi-stage + -Djava.awt.headless=true (reescalado seed)
├── src/main/java/com/eventos/culturales/
│   ├── EventosCulturalesApplication.java
│   ├── config/
│   │   ├── DataInitializer.java       # seed admin (env) + seed/backfill de carteles en BD
│   │   ├── JwtSecretKeyProvider.java  # fail-fast JWT_SECRET
│   │   └── SecurityConfig.java        # 2 cadenas + CORS/SameSite por env + JWT converter
│   ├── controller/
│   │   ├── AuthController.java        # login/refresh/logout/perfil (sin register-admin público)
│   │   └── EventoController.java      # GET público (+ /cartel y /cartel-hd) + POST/PUT multipart y DELETE @PreAuthorize ADMIN
│   ├── dto/
│   │   ├── LoginRequest.java
│   │   └── EventoDTO.java             # validación @NotBlank/@NotNull
│   ├── entities/
│   │   ├── Evento.java                # fecha indexada, NO unique; cartel/cartelHd LONGBLOB + content types
│   │   ├── Usuario.java               # = padel
│   │   └── RefreshToken.java          # = padel
│   ├── repositories/
│   │   ├── EventoRepository.java      # findByFecha(LocalDate)
│   │   ├── UsuarioRepository.java
│   │   └── RefreshTokenRepository.java
│   ├── services/
│   │   ├── CustomUserDetailsService.java
│   │   ├── JwtService.java
│   │   └── RefreshTokenService.java   # rotación + reuse-detection
│   └── exception/
│       └── GlobalExceptionHandler.java
├── src/main/resources/
│   ├── application.properties           # ${DB_URL...}, ${PORT:8081}, ${JWT_SECRET:...}, multipart 2MB, cookie, CORS
│   └── imagenes/imagen1-5.jpg           # semillas (se guardan en BD al arrancar)
└── src/test/java/... (RefreshTokenServiceTest, EventoControllerAuthTest)
```

**Structure Decision**: Maven single-module bajo `backend/`, paquete `com.eventos.culturales`, espejo directo de padel-backend para máxima reutilización.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Dos cadenas de filtros Security | Endpoints públicos explícitos (GET eventos, login) + resto autenticado, como pide el prompt | Una sola cadena con matchers basta en simple, pero el prompt exige dos y da aislamiento claro público/privado |
| Refresh rotado con reuse-detection | Requisito del prompt, ya probado en padel | JWT stateless puro es más simple pero sin revocación ante robo |
