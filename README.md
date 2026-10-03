# eventos-culturales-Vue.js

Web de eventos culturales en Puertollano (Ciudad Real). El admin hace CRUD completo de eventos, el invitado entra sin login y busca los eventos por fecha (una fecha puede tener varios eventos).

## Estado actual
Frontend Vue 3 + backend Spring Boot 4 / MySQL operativos. Contrato API en `docs/api-contract.md`; demo online: https://eventosculturales.netlify.app/.

## Estructura
```
frontend/          # app: index.html + views/ + js/ (módulos ESM: api/ composables/ pages/ ui/ store.js) + css/ + img/ + lib/vue.esm-browser.js (consume la API)
backend/           # API REST Spring Boot (ver backend/specs/001-eventos-crud/)
docs/api-contract.md  # contrato API vigente
```

## Stack frontend
HTML5 + CSS3 + JS con módulos ES nativos, Vue 3 ESM vendorizado en `frontend/lib/vue.esm-browser.js`. Sin build, sin npm. Un entrypoint por página en `frontend/js/pages/` (`login`, `invitado`, `admin`, `organizador`) que importa capas reutilizables: `api/` (fetch/auth), `composables/` (estado y lógica por dominio), `ui/` (helpers puros) y `store.js`. Datos vía `fetch` contra la API (`http://localhost:8081` en local; `RENDER_API` en producción).

## Roles
- **Invitado** (`frontend/views/usuarioEstandar.html`): entra directo desde `frontend/index.html`, elige fecha en `input[type=date]` y ve la lista de eventos del día (cartel clicable con lightbox HD, fecha, establecimiento, domicilio, evento principal y mapa embebido). Si no hay eventos, imagen de aviso.
- **Admin** (`frontend/views/administrador.html`): mismo buscador + crear (fecha, establecimiento, domicilio, contenido, cartel JPEG/PNG/WebP redimensionado en navegador a WebP 400px + HD 1600px —los GIF no se admiten—, mapa validado `google.com/maps/embed`) + gestionar (tarjeta compacta por evento con Editar y Eliminar).

## Modelo evento (API)
```json
{ "establecimiento": "...", "direccion": "...", "fecha": "YYYY-MM-DD", "descripcion": "...",
  "cartelUrl": null, "mapaEmbed": "<iframe...>" }
```
El cartel viaja aparte: `GET /eventos/{id}/cartel` (display) y `/cartel-hd` (lightbox). El frontend lo mapea a su vista (`establishment, address, date, content, poster, map`).

## Cómo ejecutar
1. Arranca el backend (`backend/specs/001-eventos-crud/quickstart.md`): `http://localhost:8081`.
2. Abre `frontend/index.html` en el navegador (demo online: https://eventosculturales.netlify.app/).

## Credenciales demo (cualquiera puede probar el proyecto)
- Admin: `admin@test.com` / `admin123` (password de desarrollo; en producción se fija con `ADMIN_SEED_PASSWORD`).
- El login es real contra `POST /auth/login`, con token JWT en `sessionStorage`.

## Limitaciones conocidas
`v-html` directo para cartel/mapa. Detalle del contrato de carteles en `docs/api-contract.md`.
