# Feature Specification: Robustez de Auth y CORS — Eventos Culturales Puertollano

**Feature Branch**: `003-robustez-auth-cors`
**Created**: 2026-09-18
**Status**: Implemented (2026-09-19, 50/50 tests en verde; ver tasks.md)
**Input**: User description: "3 mejoras de prioridad media detectadas en revisión de código: (1) sin protección de fuerza bruta en /auth/login; (2) el default de CORS_ALLOWED_ORIGINS incluye el origen 'null', innecesario ahora que el desarrollo local se sirve por Apache/XAMPP y no por file://; (3) Usuario.roles es un String parseado con split(\",\") en vez de una colección tipada."

## User Scenarios & Testing

### User Story 1 - Rate limiting básico en login (Priority: P2)
El sistema MUST limitar los intentos de `POST /auth/login` por origen (IP o email) en una ventana de tiempo, devolviendo 429 al superar el límite, sin depender de librerías externas.

**Why this priority**: Sin límite, `/auth/login` es vulnerable a fuerza bruta contra el admin sembrado. No es P1 porque hoy el proyecto no está expuesto con tráfico real, pero es imprescindible antes de dar el enlace a reclutadores.

**Independent Test**: Enviar más de N intentos fallidos de login para el mismo email/IP en la ventana configurada → a partir del intento N+1 se recibe 429, sin llegar a validar credenciales.

**Acceptance Scenarios**:
1. **Given** el límite es 5 intentos / 60s, **When** se envían 5 logins fallidos seguidos para el mismo email, **Then** los 5 devuelven 401 normal.
2. **Given** el mismo escenario, **When** se envía un 6º intento dentro de la ventana, **Then** se recibe 429 sin tocar `AuthenticationManager`.
3. **Given** pasa la ventana de tiempo, **When** se reintenta login, **Then** el contador se resetea y el intento se procesa normalmente.
4. **Given** un login válido, **When** se autentica con éxito, **Then** el contador de ese email/IP se limpia inmediatamente.

---

### User Story 2 - CORS local sin origen `null` (Priority: P2)
El default de desarrollo de `app.cors.allowed-origins` MUST NOT incluir el origen `null`, ya que el flujo de desarrollo local ya no depende de abrir el frontend como `file://` (se sirve por Apache/XAMPP en `http://localhost`).

**Why this priority**: `null` como origen permitido con `allowCredentials(true)` es una combinación permisiva que no debería llegar nunca a producción; quitarla del default reduce el riesgo de que alguien la copie sin pensar al configurar `CORS_ALLOWED_ORIGINS` en Render.

**Independent Test**: Una petición con `Origin: null` contra el backend en el perfil por defecto ya no recibe cabeceras CORS de respuesta permitiendo ese origen.

**Acceptance Scenarios**:
1. **Given** configuración por defecto (sin `CORS_ALLOWED_ORIGINS` en el entorno), **When** llega una petición con `Origin: null`, **Then** el backend no la trata como origen permitido.
2. **Given** configuración por defecto, **When** llega una petición con `Origin: http://localhost` o `http://127.0.0.1:<puerto>`, **Then** sigue permitida igual que antes.

---

### User Story 3 - Roles de Usuario como colección tipada (Priority: P3)
`Usuario.roles` MUST dejar de ser un `String` parseado manualmente con `split(",")` y pasar a una colección tipada (`Set<String>`), manteniendo el comportamiento actual (un usuario con `ROLE_USER` o `ROLE_ADMIN`).

**Why this priority**: Es deuda técnica de robustez, no un fallo de seguridad activo; se hace en último lugar y solo si no introduce riesgo sobre lo ya probado.

**Independent Test**: Los tests existentes de autorización (`AuthAdminTest`, `EventoControllerAuthTest`) siguen en verde sin cambios en sus aserciones de comportamiento HTTP; solo cambian las aserciones que inspeccionan `Usuario.getRoles()` directamente.

**Acceptance Scenarios**:
1. **Given** se siembra el admin al arrancar, **When** se consulta `usuario.getRoles()`, **Then** devuelve `Set.of("ROLE_ADMIN")` en vez de la cadena `"ROLE_ADMIN"`.
2. **Given** un registro público, **When** se crea el usuario, **Then** `getAuthorities()` sigue devolviendo `ROLE_USER` como única autoridad, igual que antes del cambio.
3. **Given** `GET /auth/perfil`, **When** se inspecciona la respuesta JSON, **Then** el campo `roles` ahora es un array (`["ROLE_ADMIN"]`) en vez de una cadena (`"ROLE_ADMIN"`) — documentado como cambio de contrato menor.

---

### Edge Cases
- Rate limit: dos usuarios distintos (emails o IPs distintas) no deben compartir contador entre sí.
- Rate limit: el contador vive en memoria (`ConcurrentHashMap`), por lo que se resetea al reiniciar la app — asumible para el tamaño actual del proyecto (ver Assumptions).
- CORS: si en el futuro se necesita volver a probar con `file://`, debe hacerse fijando `CORS_ALLOWED_ORIGINS` explícitamente para esa sesión, no reintroduciendo `null` en el default.
- Roles: ningún usuario del sistema tiene hoy más de un rol simultáneo; el cambio no necesita lógica de fusión de roles múltiples, solo de colección de tamaño 1.

## Requirements

### Functional Requirements
- **FR-001**: El sistema MUST rechazar con 429 los intentos de `POST /auth/login` que superen un máximo configurable (`app.security.login-rate-limit.max-attempts`, default 5) dentro de una ventana configurable (`app.security.login-rate-limit.window-seconds`, default 60), contados por combinación email+IP.
- **FR-002**: El contador de intentos MUST resetearse en un login exitoso para esa combinación email+IP.
- **FR-003**: El default de `app.cors.allowed-origins` en `application.properties` MUST NOT incluir el literal `null`.
- **FR-004**: `Usuario.roles` MUST ser `Set<String>` mapeado con `@ElementCollection`, sin introducir una entidad `Role` nueva (evitar over-engineering, constitución II/III).
- **FR-005**: Todos los puntos del código que hoy asignan roles con `String` (`DataInitializer`, `AuthController` en registro y en creación de admin) MUST actualizarse a la nueva colección sin cambiar la lógica de negocio (mismo rol asignado en cada caso).

### Key Entities
- **Usuario.roles**: pasa de `String` a `Set<String>` (tabla `usuario_roles` generada por Hibernate vía `@ElementCollection`).
- Ninguna entidad nueva.

## Success Criteria

### Measurable Outcomes
- **SC-001**: Un script/test que envíe 6 logins fallidos seguidos recibe 429 en el 6º intento.
- **SC-002**: Una petición con `Origin: null` no recibe cabecera `Access-Control-Allow-Origin` en la respuesta con la configuración por defecto.
- **SC-003**: `./mvnw test` en verde (tests previos + nuevos) tras el cambio de `roles` a `Set<String>`.
- **SC-004**: `docs/api-contract.md` refleja el nuevo formato de `roles` (array) en las respuestas de auth.

## Assumptions
- El rate limiting en memoria es suficiente para el alcance actual del proyecto (un solo pod/instancia en Render); si en el futuro se escala a varias instancias, habría que mover el contador a un store compartido (Redis) — fuera de alcance aquí.
- No hay usuarios en BD de producción con más de un rol hoy, así que el cambio de `roles` es una migración de forma, no de datos.
- Si existieran datos reales en producción con `roles` como columna `String`, habría que planificar una migración de datos aparte (fuera de alcance: se asume BD de desarrollo/demo, recreable).