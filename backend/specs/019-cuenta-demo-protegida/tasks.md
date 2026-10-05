# Tasks: Cuenta demo protegida y auditoría de moderación

---

## Phase 1: Usuario Demo y Filtro de Protección (P1)

- [x] T190 Sembrar usuario `demo@eventos-culturales.es` / `Demo1234!` con `ROLE_ADMIN` en `DataInitializer`.
- [x] T191 Crear `DemoAccountProtectionFilter` para interceptar peticiones `DELETE` realizadas por el usuario demo y devolver 403 Forbidden.
- [x] T192 Registrar `DemoAccountProtectionFilter` en `SecurityConfig`.

## Phase 2: Auditoría de Moderación (P2)

- [ ] T193 Añadir `moderadoPor` y `fechaModeracion` en `Evento` y `EventoResponseDTO`. *(pospuesto — es B5 de la clasificación, fuera del alcance de esta fase)*
- [ ] T194 Actualizar `aprobar` y `rechazar` en `EventoController` para guardar el nombre/email del moderador y la fecha. *(pospuesto, idem)*

---

## Notas de implementación

- **La contraseña `Demo1234!` va en claro a propósito**: es la credencial pública de la
  demo, no un secreto. Se documentará en el README.
- **El filtro NO puede ser un `@Bean`.** Spring Boot auto-registra cualquier bean de tipo
  `Filter` como filtro del servlet, que corre ANTES de `FilterChainProxy` y por tanto sin
  `SecurityContext`: `auth` llega `null` y el filtro no hace nada (además,
  `OncePerRequestFilter` se marca como ya ejecutado y hace que la copia de la cadena de
  seguridad se salte). Se crea dentro de `authenticatedChain(...)` con
  `addFilterBefore(..., AuthorizationFilter.class)`, que es donde ya está resuelto el token.
- **El email no sale de `Authentication.getName()`**: con JWT el principal es el objeto
  `Jwt`, y `getName()` devuelve su `toString()`. Se lee el claim `sub`, el mismo que usa
  `AuthController`.
- **Alcance del bloqueo**: cualquier `DELETE` autenticado de esa cuenta (eventos, usuarios,
  etiquetas, galería, favoritos y también `DELETE /auth/perfil`). Los POST/PUT funcionan
  normalmente: crear, editar y moderar están permitidos (US1 AC2).
- `DataInitializer` sigue sin constructor nuevo (sin `@Value`): la credencial demo es
  fija y `DataInitializerTest` no cambia.
