# eventos-culturales-Vue.js

Web de eventos culturales en Puertollano (Ciudad Real). El admin hace CRUD completo de eventos, el invitado entra sin login y busca los eventos por fecha (una fecha puede tener varios eventos).

## Estado actual
Frontend Vue 3 + backend Spring Boot 4 / MySQL operativos. Contrato API en `docs/api-contract.md`; demo online: https://eventosculturales.netlify.app/.

## Estructura
```
frontend/          # app: index.html + views/ (incluye login.html) + js/ (módulos ESM: api/ composables/ pages/ ui/ store.js) + css/ + img/ + lib/vue.esm-browser.js (consume la API)
backend/           # API REST Spring Boot (ver backend/specs/001-eventos-crud/)
docs/api-contract.md  # contrato API vigente
```

## Stack frontend
HTML5 + CSS3 + JS con módulos ES nativos, Vue 3 ESM vendorizado en `frontend/lib/vue.esm-browser.js`. Sin build, sin npm. Un entrypoint por página en `frontend/js/pages/` (`login`, `invitado`, `admin`, `organizador`) que importa capas reutilizables: `api/` (fetch/auth), `composables/` (estado y lógica por dominio), `ui/` (helpers puros) y `store.js`. Datos vía `fetch` contra la API (`http://localhost:8081` en local; `RENDER_API` en producción).

La pantalla de login tiene dos entradas con el mismo entrypoint: `frontend/index.html` (raíz, punto de entrada de la demo) y `frontend/views/login.html` (a la que lleva el enlace "Ingresar" de las cabeceras de las vistas). Como las rutas son relativas al documento, la redirección tras autenticar se construye con un prefijo según el contexto (`views/` en la raíz, vacío dentro de `views/`): ambos puntos de entrada llegan a la vista de su rol sin caer en `views/views/`.

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
- Cuenta demo protegida: `demo@eventos-culturales.es` / `Demo1234!` (rol ADMIN creada por la semilla). Su email está en la lista de cuentas protegidas: no se puede modificar ni eliminar desde la API.
- El login es real contra `POST /auth/login`, con token JWT en `sessionStorage`.

## Cuentas protegidas y borrado de cuenta
`DELETE /auth/perfil` anonimiza la cuenta y revoca sus refresh tokens; `GET /auth/perfil/exportar` devuelve los datos personales en JSON (RGPD). Las cuentas de la semilla (la anterior y la demo) devuelven 403 en el borrado.

## Licencia
[MIT](LICENSE) — permiso libre de copiar, modificar, distribuir y usar con o sin fines comerciales,
incluidos la venta, siempre que se conserve el aviso de copyright.
Las imágenes son activos de demostración generados con IA; no están sujetas a esta licencia.
Ver `LICENSE` y `frontend/views/avisoLegal.html`.

## Limitaciones conocidas
`v-html` directo para cartel/mapa. Detalle del contrato de carteles en `docs/api-contract.md`.

## Futuras mejoras
Cosas que **no** se van a implementar en esta versión, decidido a propósito:

- **Cambiar la contraseña con seguridad.** Hoy no existe ningún endpoint para cambiarla
  (solo `PUT /auth/perfil`, que no toca la contraseña). El flujo correcto exigiría la
  **contraseña actual** antes de aceptar la nueva —no para hacerla más difícil, sino para
  que un token robado no pueda secuestrar la cuenta—, aplicarle las mismas reglas del
  registro (≥ 8 con mayúscula, minúscula y número), revocar los refresh tokens para cerrar
  el resto de dispositivos y mandar un aviso por correo. Se descarta para una demo: es un
  flujo que nadie recorre en una presentación y solo añade fricción y campos.
- **Verificar el correo nuevo.** `PUT /auth/perfil` deja cambiar el email sin comprobar
  que la nueva dirección es de quien dice ser (por eso tampoco se pide la contraseña
  actual: solo cubriría el caso del token robado, no el de identidad falsa). En producción
  haría falta un enlace de confirmación enviado al correo nuevo antes de asumir la
  identidad. El backend ya depende de `spring-boot-starter-mail` (se usa en los
  recordatorios), pero el flujo de token pendiente no existe.
- **Segundo factor y bloqueo por intentos fallidos.** Hay rate limit por IP y correo
  (5 intentos/min en login y registro), pero no TOTP/2FA ni bloqueo temporal de la cuenta.
