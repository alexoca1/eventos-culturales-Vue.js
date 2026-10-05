# Feature Specification: Seguridad de datos, URLs y sanitización

**Feature Branch**: `017-seguridad-datos-urls`
**Created**: 2026-10-03
**Status**: Completed

## User Scenarios & Testing

### User Story 1 - Minimización de datos en respuestas de eventos (Priority: P1)

Cuando un usuario (invitado u organizador) consulta los eventos a través de `GET /eventos` o `GET /eventos/{id}`, la respuesta JSON no debe filtrar datos sensibles del organizador/usuario creador (como contraseñas, teléfonos personales, datos de encargados ni autoridades Spring Security).

**Acceptance Scenarios**:

1. **Given** un evento creado por un usuario organizador con teléfono y encargado, **When** se consulta `GET /eventos/{id}`, **Then** la propiedad `creadoPor` solo incluye `id`, `nombreOrganizacion` y `email`.

### User Story 2 - Validación estricta de URLs (Priority: P1)

Las URLs de eventos y redes sociales deben validarse en el backend para admitir únicamente esquemas `http://` o `https://` (o nulas/vacías), rechazando enlaces maliciosos como `javascript:`.

**Acceptance Scenarios**:

1. **Given** un DTO de evento con `urlEvento = "javascript:alert(1)"`, **When** se envía a `POST /eventos` o `PUT /eventos/{id}`, **Then** responde 400 Bad Request.
2. **Given** una red social con `url = "https://instagram.com/mi_evento"`, **When** se guarda, **Then** responde 200/201 OK.

### User Story 3 - Endurecimiento de registro y rate limiting (Priority: P2)

El registro de usuarios (`POST /auth/register`) debe requerir contraseñas de al menos 8 caracteres con combinación de mayúsculas, minúsculas y números, y contar con rate limiting por IP para evitar ataques de automatización.

**Acceptance Scenarios**:

1. **Given** un registro con contraseña de 6 caracteres o sin mayúscula/número, **When** `POST /auth/register`, **Then** responde 400 Bad Request.
2. **Given** más de 5 intentos de registro desde la misma IP en 1 minuto, **When** `POST /auth/register`, **Then** responde 429 Too Many Requests.

### User Story 4 - Validación de magic bytes en subida de imágenes (Priority: P2)

La subida de imágenes debe validar la firma real de los primeros bytes del archivo (magic bytes) además del Content-Type reportado por el navegador.

**Acceptance Scenarios**:

1. **Given** un archivo `.jpg` renombrado que en realidad es un ejecutable o script, **When** se sube como cartel, **Then** responde 400 Bad Request ("El archivo no es una imagen válida").

### User Story 5 - Datos semilla sin establecimientos comerciales reales (Priority: P2)

Los eventos generados por `DataInitializer` deben usar ubicaciones y establecimientos exclusivamente públicos o culturales de Puertollano (teatros, bibliotecas, plazas, museos).

**Acceptance Scenarios**:

1. **Given** la base de datos recién inicializada, **When** se consultan los eventos semilla, **Then** no aparece ningún restaurante ni comercio privado real.

---

## Requirements

### Functional Requirements

- **FR-001**: `GET /eventos` y `GET /eventos/{id}` MUST devolver una DTO de respuesta (`EventoResponseDTO` / `CreadoPorResponseDTO`) que oculte datos personales del `Usuario` creador.
- **FR-002**: `urlEvento`, `cartelUrl` y las URLs de `redesSociales` MUST validarse con regex `@Pattern` o validador personalizado para asegurar protocolo `http://` o `https://`.
- **FR-003**: `RegisterRequest` MUST exigir contraseña >= 8 caracteres con regex `^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$`.
- **FR-004**: `SecurityConfig` MUST registrar un filtro de rate limit para `POST /auth/register` (5 req / 60s por IP).
- **FR-005**: `validarArchivo()` en `EventoController` MUST comprobar magic bytes para JPEG (`FF D8`), PNG (`89 50 4E 47`) y WebP (`52 49 46 46`).
- **FR-006**: `DataInitializer` MUST reemplazar nombres de restaurantes reales por lugares públicos (Ej. Plaza de la Constitución, Teatro Municipal, Biblioteca Pública, Mercado Municipal).
