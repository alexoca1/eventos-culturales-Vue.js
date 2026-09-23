# Contrato API — Eventos Culturales Puertollano (backend)

Base URL local: `http://localhost:8081` · Swagger UI: `/swagger-ui.html` · OpenAPI JSON: `/v3/api-docs`

Autenticación: access JWT (15 min) en `Authorization: Bearer <token>` + refresh rotado en cookie HttpOnly `refreshToken` (7 días).

## Públicos (sin token)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/eventos?fecha=YYYY-MM-DD` | Eventos VIGENTES ese día (rango `[fecha, fechaFin]`; vacío → `[]`) |
| GET | `/eventos?categoria=MUSICA` | Solo los de esa categoría (combinable con `fecha`; valores: `MUSICA, TEATRO, EXPOSICION, CINE, LITERATURA, INFANTIL, OTROS`; inválida → 400, vacía = ausente) |
| GET | `/eventos` | Todos, ordenados por fecha |
| GET | `/eventos/{id}` | Uno por id (inexistente → 404; no `APROBADO` → 404 salvo admin o el organizador dueño) |
| GET | `/eventos/{id}/cartel` | Imagen display (bytes); si solo hay URL externa → 302; sin cartel → 404 |
| GET | `/eventos/{id}/cartel-hd` | Imagen HD para el lightbox (misma lógica) |
| POST | `/auth/login` `{email, password}` → `{token, accessToken, user}` + cookie | Login |
| POST | `/auth/refresh` (cookie) | Rota el refresh y devuelve nuevo access |
| POST | `/auth/logout` (cookie) | Revoca el refresh y limpia la cookie |
| POST | `/auth/register` `{email, password, nombre, apellidos, telefono?}` → 201 | Registro, **siempre `ROLE_USER`** |

## Solo `ROLE_ADMIN` (token + `@PreAuthorize("hasRole('ADMIN')")`)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/eventos` | Crea (201) como `APROBADO` directo, sin 48h. `multipart/form-data`: parte `evento` (JSON `EventoDTO`) + `file` (display 400px) y `fileHd` (HD tope 1600px) opcionales: JPEG/PNG/WebP ≤ 2 MB cada uno (los GIF no se admiten) |
| PUT | `/eventos/{id}` | Edita (200, inexistente → 404). Mismo multipart; sin ficheros conserva los carteles; queda `APROBADO` |
| DELETE | `/eventos/{id}` | Elimina directo (204, inexistente → 404) |
| DELETE | `/eventos?ids=1,2,3` | Borrado múltiple (204) |
| GET | `/eventos/pendientes` | Cola de moderación (`PENDIENTE_REVISION` + `PENDIENTE_ELIMINACION`) |
| POST | `/eventos/{id}/aprobar` | Revisión→`APROBADO` (200); eliminación pendiente→borrado definitivo (204); otro estado→409 |
| POST | `/eventos/{id}/rechazar` `{motivo?}` | Revisión→`RECHAZADO` (guarda motivo); eliminación pendiente→vuelve a `APROBADO`; otro estado→409. Dispara email al dueño (nunca revierte) |
| POST | `/auth/usuarios-admin` | Crea un admin (201, duplicado → 400) |
| GET | `/auth/usuarios` | Lista usuarios (`id, email, nombre, apellidos, roles[], telefono, enabled`) |
| PUT | `/auth/usuarios/{id}` `{roles?, enabled?}` | Edita roles/estado (200; inexistente → 404; `roles` vacío o inválido → 400; auto-bloqueo propio → 409) |
| GET | `/auth/perfil` | Perfil del token (cualquier autenticado) |

## `ROLE_ORGANIZADOR` (token + `@PreAuthorize`)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/eventos` | Crea con ≥48h de antelación (si no, 400); queda `PENDIENTE_REVISION` con `creadoPor` propio, invisible al público |
| PUT | `/eventos/{id}` | Solo propios (si no, 403); vuelve a `PENDIENTE_REVISION`; pendiente de eliminación → 409 |
| DELETE | `/eventos/{id}` | Solo propios (si no, 403); pasa a `PENDIENTE_ELIMINACION`, no borra |
| GET | `/eventos/mios` | Solo sus eventos, en cualquier estado |

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
// Evento (el cartel NO viene en el JSON: se pide a /cartel o /cartel-hd)
{"id": 1, "establecimiento": "El mesoncito", "direccion": "C. Aduana, 3, 13500 Puertollano, Ciudad Real",
 "fecha": "2026-10-01", "descripcion": "Fiesta Mexicana",
 "horaInicio": "20:00:00", "horaFin": "22:30:00", "fechaFin": "2026-10-01",
 "categoria": "MUSICA",
 "estado": "APROBADO", "creadoPor": {"id": 1, "email": "admin@test.com", ...}, "motivoRechazo": null,
 "cartelUrl": null, "mapaEmbed": "<iframe src=\"https://www.google.com/maps/embed?...\">...</iframe>",
 "fechaAlta": "2026-09-16T11:00:00", "fechaModificacion": "2026-09-16T11:00:00"}

// EventoDTO (POST/PUT: mismos campos menos id/fechas; establecimiento, direccion, fecha y descripcion obligatorios.
// horaInicio/horaFin opcionales pero siempre juntas (si no, 400); formato "HH:mm" o "HH:mm:ss".
// fechaFin opcional (YYYY-MM-DD): fin real del evento; ausente/null = un día (se guarda fecha).
// fechaFin < fecha → 400. El aviso "(día siguiente)" del frontend usa fechaFin tal cual, sin recalcular.)
```

// Auth: el campo `roles` es un ARRAY (p. ej. `["ROLE_ADMIN"]`), no una cadena
// (login: `user.roles`; refresh: `user.roles`; perfil: `roles`)
```

## Errores

| Código | Cuándo |
|---|---|
| 400 | Validación (`@NotBlank/@NotNull`), `?fecha=` malformada, horario a medias (solo una hora), `fechaFin` anterior a `fecha`, `mapaEmbed` que no sea un `<iframe>` de Google Maps con atributos permitidos (`src`, `width`, `height`, `style`, `allowfullscreen`, `loading`, `referrerpolicy`), email duplicado, cartel no-JPEG/PNG/WebP o > 2 MB |
| 401 | Sin token en ruta protegida, credenciales malas, refresh ausente/inválido/reutilizado/expirado |
| 403 | Token válido sin `ROLE_ADMIN` en operación de admin |
| 404 | Evento/usuario inexistente |

## Notas

- `fecha` **no** es única: varios eventos pueden compartir día y `GET ?fecha=` los devuelve todos.
- Carteles: bytes en MySQL (`LONGBLOB`, display 400px + HD tope 1600px) + `GET /eventos/{id}/cartel` y `/cartel-hd` públicos; el frontend sube WebP redimensionado en navegador (nunca se rechaza por tamaño, solo por tipo: JPEG/PNG/WebP, sin GIF); `cartelUrl` solo para URLs externas. Las semillas se sirven igual que los nuevos (imágenes en `backend/src/main/resources/imagenes/`).
- No existe ningún endpoint público que otorgue `ROLE_ADMIN` (difiere de padel-backend a propósito).
- Primer admin sembrado al arrancar: `admin@test.com` / `ADMIN_SEED_PASSWORD` (BCrypt).
