# Contrato API — Eventos Culturales Puertollano (backend)

Base URL local: `http://localhost:8081` · Swagger UI: `/swagger-ui.html` · OpenAPI JSON: `/v3/api-docs`

Autenticación: access JWT (15 min) en `Authorization: Bearer <token>` + refresh rotado en cookie HttpOnly `refreshToken` (7 días).

## Públicos (sin token)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/eventos?fecha=YYYY-MM-DD` | Eventos VIGENTES ese día (rango `[fecha, fechaFin]`; vacío → `[]`) |
| GET | `/eventos?etiquetas=MUSICA,TEATRO` | Solo los que lleven ALGUNA (ANY, combinable con `fecha`; vacía/ausente = sin filtro; desconocida = `[]`) |
| GET | `/eventos?futuros=true` | Solo vigentes hoy o después (`fechaFin` ≥ hoy), de lo más próximo a lo último; combinable con `etiquetas`; excluyente con `fecha` (→ 400) |
| GET | `/eventos?q=texto` | Busca por `nombre` o `establecimiento` (LIKE parcial, sin distinguir mayúsculas), **paginado** igual que `/eventos`. Longitud útil 2-100 (fuera → 400); **excluyente con `fecha` y `futuros`** (→ 400); vacío/ausente = se ignora (comportamiento actual). Los comodines `%` y `_` se escapan (se buscan como texto literal). Admin autenticado ve todos los estados; el público solo `APROBADO` |
| GET | `/eventos` | Todos, ordenados por fecha. **Paginado**: `?page=N` (base 0, default 0; tamaño fijo 10 en servidor, `?size=` no se acepta). Responde `Page<>`: `{content[], number, size, totalElements, totalPages, first, last}` |
| GET | `/eventos/{id}` | Uno por id (inexistente → 404; no `APROBADO` → 404 salvo admin o el organizador dueño) |
| GET | `/eventos/{id}/cartel` | Imagen display (bytes); si solo hay URL externa → 302; sin cartel → 404 |
| GET | `/eventos/{id}/cartel-hd` | Imagen HD para el lightbox (misma lógica) |
| GET | `/eventos/{id}/galeria` | Índices **ocupados** de la galería, p. ej. `[0,1,2]` (no las imágenes); sin galería → `[]`; evento inexistente → 404 |
| GET | `/eventos/{id}/galeria/{orden}` | Bytes de la foto de esa posición, con su `Content-Type`; posición libre o evento inexistente → 404 |
| POST | `/auth/login` `{email, password}` → `{token, accessToken, user}` + cookie | Login |
| POST | `/auth/refresh` (cookie) | Rota el refresh y devuelve nuevo access |
| POST | `/auth/logout` (cookie) | Revoca el refresh y limpia la cookie |
| POST | `/auth/register` `{email, password, nombre, apellidos, telefono}` → 201 | Registro, **siempre `ROLE_USER`** (teléfono obligatorio) |

## Solo `ROLE_ADMIN` (token + `@PreAuthorize("hasRole('ADMIN')")`)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/eventos` | Crea (201) como `APROBADO` directo, sin 48h. `multipart/form-data`: parte `evento` (JSON `EventoDTO`) + `file` (display 400px, **obligatorio al crear**) y `fileHd` (HD tope 1600px, opcional): JPEG/PNG/WebP ≤ 2 MB cada uno (los GIF no se admiten). Sin `file` → 400 |
| PUT | `/eventos/{id}` | Edita (200, inexistente → 404). Mismo multipart; sin ficheros conserva los carteles; queda `APROBADO` |
| DELETE | `/eventos/{id}` | Elimina directo (204, inexistente → 404) |
| DELETE | `/eventos?ids=1,2,3` | Borrado múltiple (204) |
| GET | `/eventos/pendientes` | Cola de moderación (`PENDIENTE_REVISION` + `PENDIENTE_ELIMINACION`) |
| POST | `/eventos/{id}/aprobar` | Revisión→`APROBADO` (200); eliminación pendiente→borrado definitivo (204); otro estado→409 |
| POST | `/eventos/{id}/rechazar` `{motivo?}` | Revisión→`RECHAZADO` (guarda motivo); eliminación pendiente→vuelve a `APROBADO`; otro estado→409. Dispara email al dueño (nunca revierte) |
| POST | `/auth/usuarios-admin` | Crea un admin (201, duplicado → 400) |
| GET | `/auth/usuarios` | Lista usuarios (`id, email, nombre, apellidos, roles[], telefono, enabled, nombreOrganizacion, encargadoNombre, encargadoTelefono, encargadoEmail`) |
| PUT | `/auth/usuarios/{id}` `{roles?, enabled?, nombreOrganizacion?, encargadoNombre?, encargadoTelefono?, encargadoEmail?}` | Edita rol/estado (200; inexistente → 404; `roles` con ≠1 rol o inválido → 400; pasar a `ORGANIZADOR` sin los 4 datos de organización → 400; auto-bloqueo propio → 409) |
| GET | `/auth/perfil` | Perfil del token (cualquier autenticado; incluye `telefono` y datos de organización) |
| PUT | `/auth/perfil` `{nombre?, apellidos?, telefono?, nombreOrganizacion?, ...}` | Edita el propio perfil (nunca rol/estado; `telefono` no se puede vaciar → 400; datos org solo si es `ORGANIZADOR` y deben quedar completos) |
| GET | `/etiquetas` | Catálogo de etiquetas, ordenadas por nombre (público, sin login) |
| POST | `/etiquetas` `{nombre}` | Crea etiqueta (201; vacío → 400; duplicada → 409) |
| PUT | `/etiquetas/{id}` `{nombre}` | Renombra (200; inexistente → 404; vacío → 400; duplicada → 409) |
| DELETE | `/etiquetas/{id}` | Elimina (204; inexistente → 404; en uso por eventos → 409) |

## `ROLE_ORGANIZADOR` (token + `@PreAuthorize`)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/eventos` | Crea con ≥48h de antelación (si no, 400); queda `PENDIENTE_REVISION` con `creadoPor` propio, invisible al público |
| PUT | `/eventos/{id}` | Solo propios (si no, 403); vuelve a `PENDIENTE_REVISION`; pendiente de eliminación → 409 |
| DELETE | `/eventos/{id}` | Solo propios (si no, 403); pasa a `PENDIENTE_ELIMINACION`, no borra |
| GET | `/eventos/mios` | Solo sus eventos, en cualquier estado. **Paginado** igual que `/eventos` (`?page=N`, mismo formato `Page<>`) |

## Galería de fotos — escritura (`ROLE_ADMIN` o dueño del evento)

Máximo 5 fotos por evento, en posiciones fijas `0..4`. `orden` es la posición, no un id: no se renumeran al borrar. Los dos GET son públicos (tabla de arriba).

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/eventos/{id}/galeria` | `multipart/form-data` con partes `orden` (int) y `foto`. **Upsert por posición**: posición libre → 201, posición ya ocupada → 200 (sustituye, no cuenta como foto nueva). Responde `{"orden":N}` — la entidad lleva el LONGBLOB y no viaja en el JSON. `orden` fuera de `0..4` → 400; `foto` ausente/vacía → 400; JPEG/PNG/WebP ≤ 2 MB (los GIF no se admiten; el backend no redimensiona) → si no, 400. 6ª foto en posición nueva con las 5 ocupadas → 400 (reemplazar una ocupada sigue valiendo). Evento inexistente → 404; otro organizador que no es el dueño → 403 |
| DELETE | `/eventos/{id}/galeria/{orden}` | Libera la posición (204). Evento o posición inexistente → 404; otro organizador que no es el dueño → 403 |

## Favoritos (cualquier usuario autenticado, `@PreAuthorize("isAuthenticated()")`)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/eventos/{id}/favorito` | Marca favorito sobre evento `APROBADO` (201; si ya existía, 200 sin duplicar; no aprobado → 404) |
| DELETE | `/eventos/{id}/favorito` | Desmarca (204, exista o no) |
| GET | `/eventos/favoritos` | Solo sus favoritos que siguen `APROBADO` |

Cada tarde (cron configurable `app.recordatorios.cron`, default 20:00) se envía un email por cada favorito con evento de mañana aún no avisado, y queda marcado para no reenviar.

## Público y visibilidad

`GET /eventos` (con o sin filtros) devuelve solo `APROBADO` salvo que el llamante sea `ROLE_ADMIN`, que ve todos los estados.

## Modelos

```json
// Evento (el cartel NO viene en el JSON: se pide a /cartel o /cartel-hd;
// las fotos de galería tampoco: se piden a /galeria/{orden}, y el JSON solo
// trae `nombre` y `descripcion`, no los índices)
{"id": 1, "establecimiento": "El mesoncito", "direccion": "C. Aduana, 3, 13500 Puertollano, Ciudad Real",
 "fecha": "2026-10-01", "horaInicio": "20:00:00", "horaFin": "22:30:00", "fechaFin": "2026-10-01",
 "nombre": "Fiesta Mexicana",
 "descripcion": "Verbena con música en directo y comida típica. Entrada libre hasta las 22:00.",
 "etiquetas": [{"id": 1, "nombre": "MUSICA"}],
 "estado": "APROBADO", "creadoPor": {"id": 1, "email": "admin@test.com", ...}, "motivoRechazo": null,
 "cartelUrl": null, "mapaEmbed": "<iframe src=\"https://www.google.com/maps/embed?...\">...</iframe>",
 "redesSociales": [{"red": "FACEBOOK", "url": "https://facebook.com/elmeseoncito"}],
 "telefonoEvento": "926420000", "urlEvento": "https://www.elmeseoncito.es",
 "fechaAlta": "2026-09-16T11:00:00", "fechaModificacion": "2026-09-16T11:00:00"}

// EventoDTO (POST/PUT: mismos campos menos id/fechas; establecimiento, direccion, fecha y nombre obligatorios.
// descripcion OPCIONAL (texto largo, TEXT): ausente o null = sin descripción; vacío = "".
// horaInicio/horaFin opcionales pero siempre juntas (si no, 400); formato "HH:mm" o "HH:mm:ss".
// fechaFin opcional (YYYY-MM-DD): fin real del evento; ausente/null = un día (se guarda fecha).
// etiquetas opcional (["MUSICA","TEATRO"]): ausente/vacío = ["OTROS"]; nombre desconocido → 400.
// redesSociales opcional ([{"red":"FACEBOOK","url":"https://..."}]): cada `red` de un enum cerrado (FACEBOOK, INSTAGRAM, X,
// YOUTUBE, TIKTOK, LINKEDIN, WHATSAPP, TELEGRAM); red desconocida → 400. Se guarda una fila por red;
// si una red se repite en el JSON gana la ÚLTIMA url (no se duplica).
// telefonoEvento y urlEvento opcionales (contacto del evento, no del organizador).
// fechaFin < fecha → 400. El aviso "(día siguiente)" del frontend usa fechaFin tal cual, sin recalcular.)
```

// Auth: el campo `roles` es un ARRAY (p. ej. `["ROLE_ADMIN"]`), no una cadena
// (login: `user.roles`; refresh: `user.roles`; perfil: `roles`)
```

## Errores

| Código | Cuándo |
|---|---|
| 400 | Validación (`@NotBlank/@NotNull` —incluido `nombre` del evento, obligatorio), `?fecha=` malformada, horario a medias (solo una hora), `fechaFin` anterior a `fecha`, `mapaEmbed` que no sea un `<iframe>` de Google Maps con atributos permitidos (`src`, `width`, `height`, `style`, `allowfullscreen`, `loading`, `referrerpolicy`), `red` de `redesSociales` desconocida, email duplicado, cartel o foto de galería no-JPEG/PNG/WebP o > 2 MB, `orden` de galería fuera de `0..4`, foto de galería ausente, 6ª foto en posición nueva, o `q` de longitud fuera de 2-100 / combinado con `fecha` o `futuros` |
| 401 | Sin token en ruta protegida, credenciales malas, refresh ausente/inválido/reutilizado/expirado |
| 403 | Token válido sin `ROLE_ADMIN` en operación de admin; o `ROLE_ORGANIZADOR` que no es el dueño del evento en editar/borrar el evento o en escribir su galería |
| 404 | Evento/usuario inexistente |
| 413 | La imagen supera lo que admite `max_allowed_packet` de MySQL al insertar el `LONGBLOB` (no lo rechaza multipart sino la BD). Ocurre con fotos crudas de algo más de ~500 KB. El frontend ya sube WebP ≤ 480 KiB, así que por la UI normal no se llega; sí se puede ver con un POST directo de una imagen sin recomprimir. Subir `max_allowed_packet` a ≥ 8 MB lo elimina |

## Notas

- `fecha` **no** es única: varios eventos pueden compartir día y `GET ?fecha=` los devuelve todos.
- Carteles: bytes en MySQL (`LONGBLOB`, display 400px + HD tope 1600px) + `GET /eventos/{id}/cartel` y `/cartel-hd` públicos; el frontend sube WebP redimensionado en navegador (nunca se rechaza por tamaño, solo por tipo: JPEG/PNG/WebP, sin GIF); `cartelUrl` solo para URLs externas. Las semillas se sirven igual que los nuevos (imágenes en `backend/src/main/resources/imagenes/`).
- `nombre` es el título corto del evento y es **obligatorio** (`@NotBlank`, `NOT NULL`); `descripcion` es el texto largo opcional (`TEXT`, admite `null`). Antes de esto el título viajaba en `descripcion`, así que los datos sembrados se han copiado a `nombre` en el arranque (backfill idempotente) y las semillas nuevas ya rellenan ambos.
- Galería: los bytes se guardan tal cual en MySQL (`LONGBLOB`) **sin redimensionar ni recomprimir** (el peso del recorte es del frontend, como en los carteles), con unique `(evento_id, orden)`. La cascada va en `Evento.fotos` (`@OneToMany`, `cascade=ALL`, `orphanRemoval`): al borrar un evento desaparecen sus fotos. En `FotoGaleria.evento` (`@ManyToOne`) **no** hay cascada a propósito, porque `cascade=REMOVE` ahí significaría que borrar una foto borra el evento entero.
- Redes sociales y contacto: las redes viven en su propia tabla (`evento_redes_sociales`, `@ElementCollection` con unique `(evento_id, red)`): una fila por red, con su URL. `telefonoEvento` y `urlEvento` son columnas de `evento` — contacto del evento en sí, distinto del teléfono del organizador. Se autogeneran con `ddl-auto=update`.
- **Techo real de imagen ≈ 512 KB**, no los 2 MB de multipart: MySQL/XAMPP trae `max_allowed_packet=1 MB` y un `INSERT` de un `LONGBLOB` de ~500 KB ya lo supera. Por eso el frontend comprime a WebP ≤ 480 KiB antes de enviar, y por eso un POST directo de 600 KB responde 413 en vez de 500. El límite de 2 MB de `spring.servlet.multipart` sigue vigente para el *tráfico*, pero el *almacenamiento* es el que manda.
- No existe ningún endpoint público que otorgue `ROLE_ADMIN` (difiere de padel-backend a propósito).
- Primer admin sembrado al arrancar: `admin@test.com` / `ADMIN_SEED_PASSWORD` (BCrypt).
