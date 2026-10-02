# Feature Specification: Nombre de evento, descripción y galería de fotos

**Feature Branch**: `015-nombre-descripcion-galeria`
**Created**: 2026-09-25
**Status**: Implemented (2026-10-02; 194/194 tests en verde; ver tasks.md)
**Input**: El campo "descripcion" actual almacena el nombre del evento (deuda
histórica). Hay que separarlo en dos campos explícitos (nombre y descripción)
y añadir una galería de hasta 5 fotos por evento.

## User Scenarios & Testing

### User Story 1 - Separar nombre y descripción (Priority: P1)
El evento tiene ahora dos campos distintos: `nombre` (obligatorio, lo que
hoy se llama `descripcion`) y `descripcion` (opcional, texto largo).

**Acceptance Scenarios**:
1. **Given** admin/organizador crea un evento, **When** envía `nombre` y
   opcionalmente `descripcion`, **Then** ambos se guardan correctamente.
2. **Given** un evento existente creado antes de esta feature (con `descripcion`
   = nombre del evento), **When** se consulta, **Then** su valor aparece en
   el campo `nombre` de la respuesta — sin pérdida de datos.
3. **Given** `nombre` vacío, **When** se crea un evento, **Then** 400.
4. **Given** `descripcion` vacía (no informada), **When** se crea un evento,
   **Then** se acepta (es opcional).

---

### User Story 2 - Galería de fotos (Priority: P2)
Un evento puede tener hasta 5 fotos adicionales, gestionadas junto al
formulario de crear/editar.

**Acceptance Scenarios**:
1. **Given** admin/organizador sube 3 fotos en el formulario, **When** guarda,
   **Then** las 3 se persisten asociadas al evento.
2. **Given** se intenta subir una 6ª foto, **When** ocurre, **Then** 400
   ("Máximo 5 fotos por evento").
3. **Given** un evento con galería, **When** `GET /eventos/{id}/galeria`,
   **Then** devuelve la lista de índices de fotos disponibles.
4. **Given** `GET /eventos/{id}/galeria/{orden}` (orden: 0-4), **When** existe
   esa foto, **Then** devuelve la imagen con su `Content-Type`; si no existe,
   404.
5. **Given** un admin/organizador edita un evento, **When** elimina una foto
   de la galería, **Then** esa foto desaparece y el resto se conserva.
6. **Given** se borra un evento, **When** ocurre, **Then** sus fotos de galería
   se borran en cascada.
   
---
   
### User Story 3 - Redes sociales, teléfono y URL del evento (Priority: P2)
Cada evento puede tener opcionalmente una lista de redes sociales
(red + URL), un teléfono de contacto del evento y una URL externa del evento.

**Acceptance Scenarios**:
1. **Given** admin/organizador crea un evento con 2 redes sociales,
   teléfono y URL, **When** lo guarda, **Then** todos se persisten y se
   devuelven en la respuesta del evento.
2. **Given** ninguno de estos campos informado, **When** se crea el evento,
   **Then** se acepta (todos opcionales).
3. **Given** una red social con un valor de `red` fuera del enum permitido,
   **When** se envía, **Then** 400.
4. **Given** dos entradas con la misma `red` para el mismo evento,
   **When** se guarda, **Then** la segunda reemplaza a la primera
   (unique por `evento_id + red`).

---

### Edge Cases
- Los eventos existentes sin galería devuelven `[]` en
  `GET /eventos/{id}/galeria` — sin error.
- El orden de las fotos en galería es el de su `orden` (0-4), no el de
  inserción.
- La migración de datos de `descripcion` → `nombre` se hace en código
  (DataInitializer y una lógica de backfill al arrancar), no con un
  script SQL manual — para no añadir pasos de despliegue.

## Requirements

### Functional Requirements
- **FR-001**: `Evento` MUST tener un campo `nombre` (`@NotBlank`, columna
  `nombre`) y el campo `descripcion` actual MUST pasar a ser opcional
  (`@Column(nullable=true)`, tipo `TEXT`).
- **FR-002**: La migración MUST copiar el valor de `descripcion` a `nombre`
  en todos los eventos donde `nombre` sea null al arrancar la aplicación
  (backfill en `DataInitializer` o `@PostConstruct`).
- **FR-003**: `EventoDTO` MUST añadir `nombre` (`@NotBlank`) y hacer
  `descripcion` opcional (sin `@NotBlank`).
- **FR-004**: Entidad nueva `FotoGaleria`: `id`, `evento` (`@ManyToOne`,
  cascade ALL), `orden` (int 0-4), `datos` (`LONGBLOB`), `contentType`
  (String). Restricción única `(evento_id, orden)`.
- **FR-005**: `POST /eventos/{id}/galeria` (multipart, campo `foto`, campo
  `orden` 0-4) MUST añadir o reemplazar la foto en esa posición.
- **FR-006**: `DELETE /eventos/{id}/galeria/{orden}` MUST eliminar esa foto.
- **FR-007**: `GET /eventos/{id}/galeria` MUST devolver `[0, 2, 4]` (los
  índices ocupados), no las imágenes en sí.
- **FR-008**: `GET /eventos/{id}/galeria/{orden}` MUST devolver la imagen
  con su `Content-Type`, o 404.
- **FR-009**: El máximo de 5 fotos MUST validarse contando las existentes
  más las nuevas: si el `orden` ya existe se reemplaza (no suma), si no
  existe y ya hay 5 fotos distintas → 400.
- **FR-010**: Borrar un evento MUST borrar sus fotos de galería en cascada
  (cubierto por `cascade = CascadeType.ALL` + `orphanRemoval = true` en
  la relación).

## Success Criteria
- **SC-001**: Crear un evento con `nombre` y `descripcion` y recuperarlo
  devuelve ambos campos.
- **SC-002**: Los eventos existentes conservan su valor en `nombre`.
- **SC-003**: Subir 5 fotos funciona; la 6ª (orden nuevo, no reemplazo)
  devuelve 400.
- **SC-004**: `./mvnw test` en verde (suite completa).

## Assumptions
- `ddl-auto=update` de Hibernate añade la columna `nombre` y la columna
  `descripcion` pasa a ser nullable — sin perder datos (Hibernate no borra
  columnas existentes con `update`).
- No se implementa compresión de galería (a diferencia del cartel, que tiene
  WebP dual) — las fotos de galería se guardan tal cual, en su formato
  original. Si el tamaño se convierte en problema, es una feature futura.
- Los endpoints de galería son públicos para GET, autenticados para POST/DELETE
  (mismo patrón que `/cartel`).