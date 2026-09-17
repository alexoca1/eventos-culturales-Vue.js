# Tasks: Panel admin + conexión API del frontend

**Input**: `specs/001-panel-admin-eventos/spec.md` + `plan.md` + `docs/api-contract.md`

**Tests**: `node --check js/eventos.js` + prueba manual contra backend local.

## Phase 1: Conexión API (US1 + US2)

- [x] F001 `API` autodetectada (local/`file://` → `localhost:8081`, si no `RENDER_API`) + `apiToView` (API → vista, cartel vía `/cartel`)
- [x] F002 `validateAdmin` contra `POST /auth/login` (email, JWT en `sessionStorage`, `credentials: 'include'`); login en `index.html` pide email
- [x] F003 Guardia admin: sin token redirige a `index.html`; `loadAllEvents` al montar
- [x] F004 `searchEvent` vía `GET /eventos?fecha=` (lista completa del día); vistas con `v-for over dayEvents`
- [x] F005 `submitForm` multipart (`evento` JSON + `file`, `fileHd` añadido en F007); 401/403 → aviso + vuelta al login
- [x] F006 Borrado inicial vía `DELETE /eventos?ids=` (después sustituido por F012)

## Phase 2: Carteles WebP + lightbox (US3)

- [x] F007 `handleFileUpload` + `webify`: canvas a WebP display 400px (`file`) + HD 1600px (`fileHd`); `accept` sin GIF; preview con proporción (`max-width/max-height`)
- [x] F008 Lightbox (`openLightbox` → `/cartel-hd`, `closeLightbox`, Esc) + overlay en ambas vistas + CSS `.lightbox`
- [x] F009 Miniatura 48px `object-fit: cover` en gestión (clic → HD)

## Phase 3: Gestionar eventos — CRUD completo (US2)

- [x] F010 Botón "Gestionar eventos" + tarjetas compactas (mini, fecha, establecimiento, contenido, Editar/Eliminar); fuera panel de checkboxes e imagen de borrado
- [x] F011 Editar reutilizando el formulario (`editingId`, título/botón dinámicos, `PUT` multipart, precarga todos los campos incluido el mapa, conserva carteles sin fotos nuevas, Cancelar)
- [x] F012 `deleteOne(id)` con `confirm()`; recarga lista + refresca búsqueda manteniendo la vista
- [x] F013 `AGENTS.md` y `README.md` alineados (auth JWT, gestión, WebP, credenciales demo)

**Checkpoint**: `node --check` verde; manual: login → crear con foto pesada → buscar → ampliar → editar → eliminar.
