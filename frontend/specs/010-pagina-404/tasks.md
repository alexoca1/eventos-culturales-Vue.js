# Tasks: Página 404 — Frontend

**Input**: `specs/010-pagina-404/spec.md` + `plan.md`

---

## Phase 1: Página 404
- [x] F104 Crear `frontend/404.html`: HTML estático sin Vue ni scripts,
  con `<link>` a `css/variables.css`, header con el mismo `background-color: var(--color-primary)`
  que el resto del proyecto, mensaje claro ("Página no encontrada"),
  y un `<a href="index.html">` con estilo de botón usando las variables ya definidas

## Phase 2: Configuración de servidor
- [x] F105 Verificar si ya existe `frontend/.htaccess`; si no existe, crearlo con
  una única línea: `ErrorDocument 404 /404.html` (ruta absoluta desde la raíz
  del servidor, ajustar si el frontend no está en la raíz del VirtualHost de XAMPP)
- [x] F106 Crear o actualizar `frontend/netlify.toml` con:
```toml
  [[redirects]]
    from = "/*"
    to = "/404.html"
    status = 404
```

## Phase 3: Verificación
- [x] F107 Prueba manual en Apache/XAMPP: acceder a una URL inexistente bajo el
  directorio del frontend → confirmar que se muestra `404.html` del proyecto

## Dependencies & Execution Order
F104 → F105/F106 en paralelo → F107. Sin dependencias externas.

## Notas
- Un solo prompt cubre toda la feature.