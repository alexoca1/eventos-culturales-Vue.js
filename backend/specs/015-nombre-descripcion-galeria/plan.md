# Implementation Plan: Nombre, descripción y galería

**Branch**: `015-nombre-descripcion-galeria` | **Date**: 2026-09-25

## Summary
Añadir `nombre` a `Evento` + hacer `descripcion` opcional + backfill al
arrancar + entidad `FotoGaleria` + 4 endpoints de galería.

## Technical Context
**Dependencies**: ninguna nueva (mismo patrón LONGBLOB que el cartel).
**Storage**: columna nueva `nombre` (NOT NULL tras backfill), `descripcion`
pasa a nullable; tabla nueva `foto_galeria`.

## Mapa de impacto (verificado contra el código actual)

| Fichero | Cambio |
|---|---|
| `Evento.java` | + campo `nombre` (`@NotBlank`, `@Column(name="nombre", nullable=false)`); `descripcion` pasa a `@Column(nullable=true, columnDefinition="TEXT")`; quita `@NotBlank` de `descripcion` |
| `EventoDTO.java` | + `@NotBlank String nombre`; `descripcion` sin `@NotBlank` (sigue en el record) |
| `EventoController.java` | `evento.setNombre(dto.nombre())`; `evento.setDescripcion(dto.descripcion())`; endpoints de galería |
| `DataInitializer.java` | backfill: `UPDATE eventos SET nombre = descripcion WHERE nombre IS NULL` vía repositorio |
| `FotoGaleria.java` | nueva entidad |
| `FotoGaleriaRepository.java` | nueva: `findByEventoIdAndOrden`, `findOrdenesByEventoId`, `countByEventoId` |
| `EventoController.java` | + `POST/DELETE /eventos/{id}/galeria`, `GET /eventos/{id}/galeria`, `GET /eventos/{id}/galeria/{orden}` |
| Tests | `EventoControllerAuthTest` + nuevo `GaleriaControllerTest` |
| `docs/api-contract.md` | documentar `nombre`, `descripcion`, 4 endpoints de galería |
| `RedSocial.java` (enum) | nuevo: FACEBOOK, INSTAGRAM, X, YOUTUBE, TIKTOK, LINKEDIN, WHATSAPP, TELEGRAM |
| `RedSocialEvento.java` (@Embeddable) | nuevo: red (RedSocial), url (String) |
| `Evento.java` | + redesSociales (@ElementCollection EAGER, tabla evento_redes_sociales, unique evento_id+red), telefonoEvento (String nullable), urlEvento (String nullable) |
| `EventoDTO.java` | + redesSociales (List<RedSocialDTO> nullable), telefonoEvento (String nullable), urlEvento (String nullable) |
| `EventoController.java` | mapear los 3 campos nuevos en create/update |

## Constitution Check
- [x] III. Sin over-engineering: no se crea un `GaleriaService` aparte —
  la lógica de galería vive en `EventoController` (consistente con el resto
  del CRUD de eventos).
- [x] V. Seguridad: GET galería es público; POST/DELETE exigen JWT +
  ownership o ROLE_ADMIN (mismo patrón que update/delete de evento).