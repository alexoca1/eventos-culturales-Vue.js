# Tasks: Nombre, descripción y galería

---

## Phase 1: Separar nombre y descripción (US1)
- [x] T147 Añadir `nombre` a `Evento.java` (`@NotBlank`, `@Column(name="nombre",
  nullable=false)`); quitar `@NotBlank` de `descripcion` y hacer su `@Column`
  nullable y `columnDefinition="TEXT"`
- [x] T148 Añadir `nombre` (`@NotBlank`) a `EventoDTO`; hacer `descripcion`
  sin validación de `@NotBlank`
- [x] T149 En `EventoController`: `evento.setNombre(dto.nombre())` en
  create y update; añadir `nombre` a la respuesta JSON
- [x] T150 Backfill en `DataInitializer` (o `@PostConstruct` separado):
  buscar todos los eventos donde `nombre` es null y copiar el valor de
  `descripcion` a `nombre`
- [x] T151 [P] Test: crear con `nombre` y `descripcion` → ambos guardados;
  sin `nombre` → 400; sin `descripcion` → 201 (opcional); evento legacy
  con `nombre=null` → backfill lo rellena al arrancar

  Nota T150: el filtro cubre `null` **y** `''`, no solo `null`. Al añadir la
  columna como `NOT NULL`, MySQL rellena las filas existentes con cadena
  vacía, así que con `IS NULL` a secas el backfill no migraría ninguna fila
  (y `descripcion` es `TEXT` sin `@NotBlank`, por lo que la copia nunca
  excede 255 chars).

**Checkpoint**: `./mvnw test` en verde, eventos existentes conservan su
valor en `nombre`.

---

## Phase 2: Galería (US2)
- [x] T152 Crear `FotoGaleria.java`: `id`, `evento` (`@ManyToOne`),
  `orden` (int), `datos` (LONGBLOB), `contentType` (String); unique
  constraint `(evento_id, orden)`
- [x] T153 Crear `FotoGaleriaRepository`: `findByEventoIdAndOrden`,
  `findOrdenesByEventoId`, `countDistinctOrdenByEventoId`
- [x] T154 [P] Tests de galería: subir foto en orden 0 → 201; reemplazar
  orden 0 → 200; 5 fotos distintas + intento de 6ª → 400; GET índices →
  `[0,1,2]`; GET foto por orden → imagen; GET orden inexistente → 404;
  DELETE → 204; organización no dueña → 403; borrar evento → fotos
  eliminadas en cascada
- [x] T155 Implementar en `EventoController`:
  - `POST /eventos/{id}/galeria` (multipart: `foto`, `orden`)
  - `DELETE /eventos/{id}/galeria/{orden}`
  - `GET /eventos/{id}/galeria` → lista de órdenes ocupados
  - `GET /eventos/{id}/galeria/{orden}` → imagen con Content-Type

  Desviación deliberada de T152: `cascade=ALL` + `orphanRemoval=true` van
  en `Evento.fotos` (`@OneToMany`), NO en `FotoGaleria.evento`. En un
  `@ManyToOne`, `cascade=REMOVE` significa que borrar la FOTO borra el
  Evento padre, y `DELETE /galeria/{orden}` se llevaría por delante el
  evento entero. La cascada de FR-010 solo funciona desde la colección.
  Hay un test que fija ambas propiedades del mapping para que nadie lo
  revierta sin querer.

  Límite conocido: el proyecto no tiene ningún test que arranque un
  contexto JPA (los slices son `@WebMvcTest`; no hay H2 ni Testcontainers),
  así que la cascada real, el JPQL de `findOrdenesByEventoId` y el parseo
  del método derivado `countDistinctOrdenByEventoId` solo se validan al
  arrancar contra el MySQL real, no en la suite.

---

## Phase 3: Polish
- [x] T156 Actualizar `docs/api-contract.md`: campos `nombre`/`descripcion`
  en modelo Evento, 4 endpoints de galería
- [x] T157 `./mvnw test` completo en verde — 194/194, `BUILD SUCCESS`
  (2026-10-02)

---

## Phase 4: Redes sociales, teléfono y URL (US3)
- [x] T158 Crear enum `RedSocial { FACEBOOK, INSTAGRAM, X, YOUTUBE, TIKTOK,
  LINKEDIN, WHATSAPP, TELEGRAM }`
- [x] T159 Crear `@Embeddable RedSocialEvento { RedSocial red, String url }`
- [x] T160 En `Evento.java`: añadir `@ElementCollection(fetch=EAGER)`
  `@CollectionTable(name="evento_redes_sociales", uniqueConstraints=
  @UniqueConstraint(columnNames={"evento_id","red"}))` `List<RedSocialEvento>
  redesSociales`; añadir `String telefonoEvento` (nullable) y
  `String urlEvento` (nullable, length=500)
- [x] T161 En `EventoDTO`: añadir `List<RedSocialDTO> redesSociales`
  (nullable), `String telefonoEvento` (nullable), `String urlEvento` (nullable);
  crear record `RedSocialDTO(String red, String url)` en el mismo paquete dto
- [x] T162 [P] Test: crear evento con 2 redes sociales + teléfono + URL →
  se recuperan correctamente; red inválida → 400; sin ninguno de los 3 → 201;
  dos entradas con misma red → la segunda reemplaza a la primera
- [x] T163 Mapear en `EventoController.create()/update()`:
  `evento.setRedesSociales(...)`, `evento.setTelefonoEvento(...)`,
  `evento.setUrlEvento(...)`

  Nota: la unicidad real la impone el `@UniqueConstraint(evento_id, red)` de
  la `@CollectionTable`; el dedupe en `resolverRedesSociales()` es la defensa
  de API. Una red desconocida falla al deserializar el part `evento` antes de
  llegar al controller y el `GlobalExceptionHandler` responde 400.

  Cobertura (5 tests en `EventoControllerAuthTest`):
  `post_admin_conDosRedesTelefonoYUrl_201_yGuardaTodos`,
  `post_admin_conRedInvalida_400`, `post_admin_sinRedesNiContacto_201_conVacios`,
  `post_admin_conRedDuplicada_laReemplaza` y
  `put_admin_cambiaRedes_201_ReemplazaLasAnteriores`.

  Suite: `194/194` en verde, `BUILD SUCCESS` (2026-09-30). La nota anterior
  decía 195: ese +1 era el informe obsoleto de `DiagnosticoCadenaTest`, una
  clase de diagnóstico que ya no está en `src/test`. Compilar/ejecutar tests
  con `JWT_SECRET` en el entorno (Base64 válido, si no el
  `JwtSecretKeyProvider` aborta el arranque de todos los slices de Spring).