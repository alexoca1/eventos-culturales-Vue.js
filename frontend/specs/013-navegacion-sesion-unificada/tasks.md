# Tasks: Navegación y sesión unificada — Frontend

**Input**: `specs/013-navegacion-sesion-unificada/spec.md` + `plan.md`

---

## Phase 1: Sesión del header + navegación (P1)
- [x] F154 `useAuth`: `sesionNombre/panelUrl/menuAbierto/alternarMenu/cargarSesion/irAPerfil`; `logout()` redirige a `../index.html`
- [x] F155 Headers idénticos en las 3 vistas (título a inicio + `.sesion`/dropdown/`Ingresar`) y en `index.html` (título enlazado); `panel-tag` fuera del header en organizador
- [x] F156 `.control` solo filtros + `.acciones` separada en admin/organizador/invitado; eliminados botones sueltos de perfil/salir; CSS mínimo con vars existentes
- [x] F157 Exponer `useAuth` + `cargarSesion()` en los 3 entrypoints; `node --check`; prueba manual
