# Implementation Plan: Panel admin + conexión API del frontend

**Branch**: `001-panel-admin-eventos` | **Date**: 2026-09-17 | **Spec**: `specs/001-panel-admin-eventos/spec.md`

**Input**: `docs/api-contract.md` + app Vue existente (`js/eventos.js` compartida por 3 páginas).

## Summary

Conectar el frontend a la API sin reescribirlo: misma app Vue y mismas vistas, con `fetch` (login JWT, búsqueda/listas, multipart y borrado), tarjetas compactas de gestión con edición reutilizando el formulario, y subida WebP dual + lightbox HD con canvas + CSS plano.

## Technical Context

**Language/Version**: JS plano (sin build), Vue 3 global local.

**Primary Dependencies**: Ninguna nueva (Vue en `lib/`, Google Maps iframes).

**Storage**: N/A (todo en la API; solo `sessionStorage` para el JWT).

**Testing**: `node --check` como gate (sin framework de tests en frontend).

**Target Platform**: Estático en Netlify (`https://eventosculturales.netlify.app/`); local por `file://`.

**Project Type**: Frontend multi-página sin build.

**Performance Goals**: Listas descargan display (~30-50 KB/evento); HD solo bajo clic.

**Constraints**: `RENDER_API` debe fijarse al desplegar el backend; cookie refresh cross-site exige `credentials: 'include'` + `SameSite=None; Secure` en el backend; el PUT sin ficheros conserva carteles (comportamiento del backend del que depende la edición).

**Scale/Scope**: 1 JS + 3 vistas (index, invitado, admin) + 1 CSS tocados.

## Constitution Check

N/A (la constitución rige el backend). Principios aplicados: reutilizar antes que reescribir (mismo formulario para crear/editar, `apiToView` centraliza el mapeo), YAGNI (sin router, sin store, sin uploader externo).

## Project Structure

```text
frontend/
├── js/eventos.js                 # fetch API, auth, CRUD, WebP dual, lightbox
├── views/usuarioEstandar.html    # lista dayEvents + lightbox
├── views/administrador.html      # lista + formulario crear/editar + gestionar + lightbox
├── css/eventos.css               # .lightbox + .gestionar/.evento-row
└── index.html                    # login por email
```

**Structure Decision**: Sin cambios de estructura; toda la lógica en la app Vue compartida existente.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Sin tests frontend | Proyecto portfolio sin build ni framework de test; gate `node --check` + prueba manual | Montar Vitest/Playwright para 1 fichero JS es desproporcionado |
