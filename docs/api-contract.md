# Contrato API — Eventos Culturales Puertollano (backend)

Base URL local: `http://localhost:8081` · Swagger UI: `/swagger-ui.html` · OpenAPI JSON: `/v3/api-docs`

Autenticación: access JWT (15 min) en `Authorization: Bearer <token>` + refresh rotado en cookie HttpOnly `refreshToken` (7 días).

## Públicos (sin token)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/eventos?fecha=YYYY-MM-DD` | Todos los eventos de ese día (vacío → `[]`) |
| GET | `/eventos` | Todos, ordenados por fecha |
| GET | `/eventos/{id}` | Uno por id (inexistente → 404) |
| GET | `/eventos/{id}/cartel` | Imagen display (bytes); si solo hay URL externa → 302; sin cartel → 404 |
| GET | `/eventos/{id}/cartel-hd` | Imagen HD para el lightbox (misma lógica) |
| POST | `/auth/login` `{email, password}` → `{token, accessToken, user}` + cookie | Login |
| POST | `/auth/refresh` (cookie) | Rota el refresh y devuelve nuevo access |
| POST | `/auth/logout` (cookie) | Revoca el refresh y limpia la cookie |
| POST | `/auth/register` `{email, password, nombre, apellidos, telefono?}` → 201 | Registro, **siempre `ROLE_USER`** |

## Solo `ROLE_ADMIN` (token + `@PreAuthorize("hasRole('ADMIN')")`)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/eventos` | Crea (201). `multipart/form-data`: parte `evento` (JSON `EventoDTO`) + `file` (display 400px) y `fileHd` (HD tope 1600px) opcionales: JPEG/PNG/WebP ≤ 2 MB cada uno (los GIF no se admiten) |
| PUT | `/eventos/{id}` | Edita (200, inexistente → 404). Mismo multipart; sin ficheros conserva los carteles |
| DELETE | `/eventos/{id}` | Elimina (204, inexistente → 404) |
| DELETE | `/eventos?ids=1,2,3` | Borrado múltiple (204) |
| POST | `/auth/usuarios-admin` | Crea un admin (201, duplicado → 400) |
| GET | `/auth/usuarios` | Lista usuarios |
| GET | `/auth/perfil` | Perfil del token (cualquier autenticado) |

## Modelos

```json
// Evento (el cartel NO viene en el JSON: se pide a /cartel o /cartel-hd)
{"id": 1, "establecimiento": "El mesoncito", "direccion": "C. Aduana, 3, 13500 Puertollano, Ciudad Real",
 "fecha": "2026-10-01", "descripcion": "Fiesta Mexicana",
 "cartelUrl": null, "mapaEmbed": "<iframe src=\"https://www.google.com/maps/embed?...\">...</iframe>",
 "fechaAlta": "2026-09-16T11:00:00", "fechaModificacion": "2026-09-16T11:00:00"}

// EventoDTO (POST/PUT: mismos campos menos id/fechas; establecimiento, direccion, fecha y descripcion obligatorios)
```

## Errores

| Código | Cuándo |
|---|---|
| 400 | Validación (`@NotBlank/@NotNull`), `?fecha=` malformada, mapa sin `google.com/maps/embed`, email duplicado, cartel no-JPEG/PNG/WebP o > 2 MB |
| 401 | Sin token en ruta protegida, credenciales malas, refresh ausente/inválido/reutilizado/expirado |
| 403 | Token válido sin `ROLE_ADMIN` en operación de admin |
| 404 | Evento/usuario inexistente |

## Notas

- `fecha` **no** es única: varios eventos pueden compartir día y `GET ?fecha=` los devuelve todos.
- Carteles: bytes en MySQL (`LONGBLOB`, display 400px + HD tope 1600px) + `GET /eventos/{id}/cartel` y `/cartel-hd` públicos; el frontend sube WebP redimensionado en navegador (nunca se rechaza por tamaño, solo por tipo: JPEG/PNG/WebP, sin GIF); `cartelUrl` solo para URLs externas. Las semillas se sirven igual que los nuevos (imágenes en `backend/src/main/resources/imagenes/`).
- No existe ningún endpoint público que otorgue `ROLE_ADMIN` (difiere de padel-backend a propósito).
- Primer admin sembrado al arrancar: `admin@test.com` / `ADMIN_SEED_PASSWORD` (BCrypt).
