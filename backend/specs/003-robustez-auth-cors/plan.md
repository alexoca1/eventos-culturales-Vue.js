# Implementation Plan: Robustez de Auth y CORS — Eventos Culturales Puertollano

**Branch**: `003-robustez-auth-cors` | **Date**: 2026-09-18 | **Spec**: `specs/003-robustez-auth-cors/spec.md`

**Input**: Feature specification + revisión de código manual (rate limiting ausente en login, CORS con origen `null` innecesario, roles como String parseado).

## Summary
Tres mejoras de robustez independientes entre sí: (1) filtro/interceptor de rate limiting en memoria sobre `/auth/login`; (2) quitar `null` del default de `app.cors.allowed-origins`; (3) migrar `Usuario.roles` de `String` a `Set<String>` vía `@ElementCollection`.

## Technical Context
**Language/Version**: Java 21, sin nuevas dependencias externas (constitución II: stdlib-first).
**Primary Dependencies**: ninguna nueva — rate limiting con `java.util.concurrent.ConcurrentHashMap` + `java.time.Instant`, sin Bucket4j ni Redis.
**Storage**: nueva tabla `usuario_roles` (generada por Hibernate vía `@ElementCollection`, `ddl-auto=update`); resto sin cambios.
**Testing**: JUnit 5 + Mockito + MockMvc + Spring Security Test, mismo estilo que `001-eventos-crud`/`002-hardening-seguridad`.
**Target Platform**: mismo Docker/Render.
**Constraints**: no romper ningún test existente (`001` y `002` deben seguir en verde); rate limiter debe ser thread-safe y no introducir contención relevante en tráfico normal.

## Constitution Check
- [x] I. Constructor injection: el rate limiter se inyecta como bean (`@Component` con estado interno encapsulado), no como campo `@Autowired`.
- [x] II. Stdlib-first: `ConcurrentHashMap`/`Instant` en vez de Bucket4j; `@ElementCollection` es JPA estándar, no una librería nueva.
- [x] III. Sin over-engineering: no se crea entidad `Role`; rate limiter en memoria, no Redis (justificado en Complexity Tracking).
- [x] IV. Tests obligatorios: test del rate limiter (5 intentos OK, 6º 429, reset tras éxito) y test de CORS (origen `null` ya no permitido por defecto).
- [x] V. Seguridad explícita: refuerza, no debilita, la seguridad existente.
- [x] VI/VII: sin cambios.

## Project Structure

### Documentation (this feature)
```text
specs/003-robustez-auth-cors/
├── plan.md              # This file
└── tasks.md             # Phase 2
```

### Source Code (archivos afectados)
```text
backend/src/main/java/.../config/LoginRateLimitFilter.java     # nuevo: filtro servlet, se registra antes de la auth chain
backend/src/main/java/.../config/SecurityConfig.java           # registrar el filtro nuevo en la cadena pública
backend/src/main/resources/application.properties              # nuevas props app.security.login-rate-limit.*; quitar null de CORS default
backend/src/main/java/.../entities/Usuario.java                 # roles: String -> Set<String> (@ElementCollection); getAuthorities() adaptado
backend/src/main/java/.../config/DataInitializer.java           # admin.setRoles(Set.of("ROLE_ADMIN"))
backend/src/main/java/.../controller/AuthController.java        # setRoles(Set.of(...)) en registro y creación de admin
backend/src/test/java/.../config/LoginRateLimitFilterTest.java  # nuevo
backend/src/test/java/.../controller/AuthAdminTest.java         # actualizar aserciones sobre getRoles()
backend/src/test/java/.../services/RefreshTokenServiceTest.java # actualizar el builder de Usuario de test (roles(Set.of(...)))
docs/api-contract.md                                             # roles ahora es array en las respuestas de auth
```

**Structure Decision**: un fichero nuevo (`LoginRateLimitFilter`), el resto son ediciones sobre ficheros existentes. No se crean paquetes nuevos.

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Filtro servlet nuevo para rate limiting | Necesita interceptar antes de que `AuthenticationManager` valide credenciales, para no gastar ciclos de BCrypt en peticiones ya bloqueadas | Un `@Aspect` o lógica dentro de `AuthController` funcionaría, pero un filtro es el patrón estándar de Spring Security para esto y ya hay dos cadenas de filtros en el proyecto (consistencia con lo existente) |
| Rate limit en memoria, no Redis | Alcance actual: 1 instancia en Render | Redis añade una dependencia externa nueva y un servicio más que mantener, desproporcionado para el tráfico real de un portafolio (constitución II) |