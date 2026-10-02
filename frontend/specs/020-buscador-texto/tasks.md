# Tasks: Buscador de texto — Frontend

---

## Phase 1: API (P1)
- [x] F193 En `api/eventos.js`, añadir función `buscarTexto({ q, page })`
  que llama a `GET /eventos?q=...&page=N` sin cabecera de auth (anónima,
  igual que `buscar()`)

## Phase 2: Composables (P1)
- [x] F194 En `useEventosInvitado.js`: añadir `searchText` (ref ""),
  `errBusqueda` (ref ""); función `buscarPorTexto()` que: valida
  `searchText.length >= 2` (si no → `errBusqueda = "Escribe al menos 2
  caracteres"` y return); limpia `searchDate`, `searchEtiquetas`, reinicia
  `paginaActual` a 0; llama a `buscarTexto()`; guarda resultados en
  `dayEvents` y `paginaMeta`; activa `showEvent`; limpia `errBusqueda`.
  Actualizar `filtrar()` para que limpie `searchText`; actualizar
  `cargarProximos()` para que limpie `searchText`. Añadir
  `paginaSiguiente()`/`paginaAnterior()` con rama para `searchText`
  activo. Exponer `searchText`, `errBusqueda`, `buscarPorTexto` en el
  return
- [x] F195 En `useGestionEventos.js`: añadir `filtroTexto` (ref ""); en el
  computed `eventosGestion`, añadir el filtro de texto sobre `nombre` y
  `establecimiento` (case-insensitive, usando `toLowerCase()`) como
  condición adicional al filtro de fecha ya existente. Exponer
  `filtroTexto` en el return

## Phase 3: Plantillas (P1)
- [x] F196 En `usuarioEstandar.html`, dentro del `div.control`,
  añadir debajo de los atajos de fecha un bloque de buscador:
```html
  <div class="buscador-texto">
    <label>🔍 Buscar por nombre o lugar</label>
    <div style="display:flex; gap:var(--space-sm)">
      <input type="text" v-model="searchText"
             placeholder="Ej: Teatro Municipal, Jazz..."
             minlength="2" maxlength="100"
             @keyup.enter="buscarPorTexto()">
      <button type="button" @click="buscarPorTexto()">Buscar</button>
    </div>
    <small v-if="errBusqueda" style="color:var(--color-estado-rechazado)">
      {{errBusqueda}}</small>
  </div>
```
  En el bloque de "no hay evento" (`v-show="showNoEvent"`), añadir
  variante del mensaje cuando `searchText`:
```html
  <p v-if="searchText">No se encontraron eventos para
    "<strong>{{searchText}}</strong>".</p>
```

- [x] F197 En `administrador.html`, dentro de la sección
  `v-show="showManage"` de "Gestionar eventos", añadir antes de la lista
  de tarjetas:
```html
  <div style="margin-bottom:var(--space-sm)">
    <input type="text" v-model="filtroTexto"
           placeholder="Filtrar por nombre o lugar..."
           style="width:100%; max-width:400px">
  </div>
```
  La lista ya usa `eventosGestion` (el computed), que ahora incluye el
  filtro de texto — sin cambios en el template del bucle.

## Phase 4: Polish
- [x] F198 `node --check` en `api/eventos.js`, `useEventosInvitado.js`,
  `useGestionEventos.js`
- [x] F199 Prueba manual: buscar "teatro" → resultados; buscar "a" →
  mensaje de error sin petición; en "Gestionar eventos", escribir en
  el filtro reduce la lista al instante