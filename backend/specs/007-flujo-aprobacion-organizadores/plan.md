# Implementation Plan: Flujo de Aprobación de Organizadores — Eventos Culturales Puertollano

**Branch**: `007-flujo-aprobacion-organizadores` | **Date**: 2026-09-18 | **Spec**: `specs/007-flujo-aprobacion-organizadores/spec.md`

## Summary
Nuevo rol `ROLE_ORGANIZADOR` + máquina de estados en `Evento` (`estado`, `creadoPor`, `motivoRechazo`) + 4 endpoints nuevos (`/eventos/mios`, `/eventos/pendientes`, `/eventos/{id}/aprobar`, `/eventos/{id}/rechazar`) + filtrado condicional de `GET /eventos` según rol + envío de email vía `spring-boot-starter-mail`/Mailtrap.

## Technical Context
**Language/Version**: Java 21.
**Primary Dependencies**: **nueva** `spring-boot-starter-mail` (única forma real de enviar email; excepción justificada a stdlib-first, igual que se justificó en su día para JWT/BCrypt).
**Storage**: + columna `estado` (String vía `@Enumerated(STRING)`, default `APROBADO`), + `creado_por_id` (FK nullable a `usuario`), + `motivo_rechazo` (String nullable).
**Testing**: JUnit 5 + MockMvc + Spring Security Test (`jwt()` post-processor con distintas authorities para simular admin/organizador).
**Constraints**: no romper la suite `001` a `006`; el email es un efecto secundario que nunca debe revertir una transacción de BD ya confirmada (FR-013).

## Constitution Check
- [x] I. Constructor injection: `EmailService`, `EventoController` reciben sus dependencias por constructor.
- [x] II. Stdlib-first: única excepción justificada es `spring-boot-starter-mail`, imprescindible para SC-de-email; el resto (máquina de estados, filtros) es lógica Java pura.
- [x] III. Sin over-engineering: la máquina de estados vive como enum simple en el propio `Evento`, no como entidad `Solicitud` aparte; el filtrado por rol se hace leyendo `SecurityContext`, no con un sistema de permisos genérico.
- [x] IV. Tests obligatorios: cubiertos en tasks.md, un test por escenario de aceptación.
- [x] V. Seguridad explícita: cada endpoint nuevo lleva `@PreAuthorize` explícito; la comprobación de propiedad (`creadoPor == usuario autenticado`) se hace en el cuerpo del método, no solo por rol.
- [x] VI/VII: sin cambios.

## Project Structure

### Documentation (this feature)
```text
specs/007-flujo-aprobacion-organizadores/
├── plan.md
└── tasks.md
```

### Source Code
```text
backend/src/main/java/.../entities/EstadoEvento.java              # enum nuevo
backend/src/main/java/.../entities/Evento.java                     # + estado, creadoPor, motivoRechazo
backend/src/main/java/.../dto/RechazoRequest.java                  # record nuevo: { motivo }
backend/src/main/java/.../services/EmailService.java               # nuevo, envuelve JavaMailSender
backend/src/main/java/.../controller/EventoController.java         # create/update/delete con lógica de estado+ownership; + /mios, /pendientes, /aprobar, /rechazar
backend/src/main/java/.../repositories/EventoRepository.java       # + findByCreadoPorOrderByIdAsc, findByEstadoInOrderByIdAsc
backend/src/main/java/.../config/SecurityConfig.java               # @PreAuthorize ya cubre los nuevos endpoints vía anotaciones en el controller; revisar cadena pública para /eventos/mios y /pendientes (NO deben ser públicos)
backend/src/main/java/.../config/DataInitializer.java              # eventos semilla -> estado=APROBADO, creadoPor=admin semilla
backend/src/main/resources/application.properties                  # spring.mail.* (Mailtrap, vía env vars)
backend/src/test/java/.../controller/EventoControllerOrganizadorTest.java  # nuevo
docs/api-contract.md                                                 # documentar estado, creadoPor, motivoRechazo, y los 4 endpoints nuevos
```

**Structure Decision**: 2 ficheros nuevos pequeños (`EstadoEvento`, `RechazoRequest`, `EmailService`), el resto son ediciones. No se crea capa de servicio nueva para eventos (se mantiene Controller→Repository directo, salvo la lógica de estado que vive en el propio controller, igual que el resto del CRUD).

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Dependencia nueva `spring-boot-starter-mail` | US6 requiere enviar email real | No existe alternativa stdlib para SMTP; es la misma excepción que ya se justificó para JWT (constitución II permite excepciones cuando no hay alternativa razonable) |