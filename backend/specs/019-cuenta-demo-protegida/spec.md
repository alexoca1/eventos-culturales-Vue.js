# Feature Specification: Cuenta demo protegida y auditoría de moderación

**Feature Branch**: `019-cuenta-demo-protegida`
**Created**: 2026-10-03
**Status**: Completed
**Nota**: T193/T194 pospuestos (B5 de la clasificación, fuera del alcance de esta fase).

## User Scenarios & Testing

### User Story 1 - Protección de la Cuenta Demo (Priority: P1)

Para permitir que reclutadores o usuarios de la demo exploren la aplicación con las credenciales públicas (`demo@eventos-culturales.es`), las operaciones destructivas (como `DELETE /eventos/{id}` o `DELETE /auth/usuarios/{id}`) intentadas por este usuario específico deben ser bloqueadas con un mensaje informativo (403 Forbidden: "Acción no permitida en modo demostración").

**Acceptance Scenarios**:

1. **Given** un usuario autenticado como `demo@eventos-culturales.es`, **When** intenta borrar un evento o un usuario, **Then** el servidor responde 403 Forbidden ("Acción no permitida en modo demostración").
2. **Given** el usuario demo, **When** crea o edita un evento, o aprueba/rechaza un evento, **Then** la operación se realiza correctamente.

### User Story 2 - Registro ligero de moderación (Priority: P2)

La entidad `Evento` debe almacenar quién aprobó o rechazó el evento y la fecha de moderación (`moderadoPor`, `fechaModeracion`), ofreciendo trazabilidad de las decisiones de moderación.

**Acceptance Scenarios**:

1. **Given** un admin que aprueba o rechaza un evento, **When** se ejecuta la acción, **Then** el evento guarda el usuario moderador y la fecha/hora actual.

---

## Requirements

### Functional Requirements

- **FR-001**: `DataInitializer` MUST sembrar al usuario `demo@eventos-culturales.es` con contraseña `Demo1234!` y rol `ROLE_ADMIN`.
- **FR-002**: Se MUST registrar un `DemoProtectionFilter` o interceptor de seguridad que capture peticiones mutativas destructivas (`DELETE`) realizadas por el usuario `demo@eventos-culturales.es` y devuelva 403 Forbidden.
- **FR-003**: `Evento` MUST contar con campos `String moderadoPor` y `LocalDateTime fechaModeracion`, que se actualizan al invocar `/aprobar` o `/rechazar`.
