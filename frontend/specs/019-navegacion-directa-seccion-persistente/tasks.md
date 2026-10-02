# Tasks: Nombre, descripción y galería — Frontend

---

## Phase 1: apiToView y composable (US1)
- [x] F181 En `api/eventos.js`, actualizar `apiToView()`:
  - `nombre: e.nombre` (campo nuevo)
  - `content: e.nombre` (sigue siendo el alias visible en tarjetas)
  - `description: e.descripcion || ""` (campo nuevo, opcional)
  - `galeriaIndices: []` (se rellena aparte, ver F186)

## Phase 2: Formulario (US1 + US2)
- [x] F182 En `useEventoForm.js`: añadir `inputNombre` (ref ""),
  `inputDescripcion` (ref ""), `fotosGaleria` (ref, array de hasta 5
  `{orden, file}` para subida) y `galeriaExistente` (ref, array de
  índices ya en BD para edición); exponer en el return
- [x] F183 En `administrador.html` y `organizador.html`:
  - Renombrar el label "Ingresar Evento Principal" → "Nombre del evento *"
    y enlazarlo con `inputNombre`
  - Añadir debajo un `<textarea>` para `inputDescripcion` (label
    "Descripción del evento (opcional)", 4 filas)
  - Añadir sección de galería: 5 slots con `<input type="file">` + preview
    de miniatura + botón "×" para quitar; label "Galería de fotos
    (máx. 5, opcional)"
- [x] F184 En `useEventoForm.js`, actualizar `submitForm()`: tras crear/editar
  el evento y obtener su `id`, iterar `fotosGaleria` y llamar a
  `POST /eventos/{id}/galeria` con `multipart/form-data` para cada foto
  nueva; para las eliminadas (slot vacío que en edición tenía foto), llamar
  a `DELETE /eventos/{id}/galeria/{orden}`
- [x] F185 Actualizar `editEvento()` para precargar `inputNombre`,
  `inputDescripcion` y cargar `galeriaExistente` vía
  `GET /eventos/{id}/galeria`

## Phase 3: Vista pública (US3)
- [x] F186 En `apiToView()`, añadir una función auxiliar `galeriaUrl(id,
  orden)` que devuelve `${API}/eventos/${id}/galeria/${orden}`; en las
  plantillas (`usuarioEstandar.html`, `administrador.html`,
  `organizador.html`), debajo del cartel, mostrar miniaturas de galería
  cuando `ev.galeriaIndices.length > 0` con `@click="openLightbox(url)"`

## Phase 4: Polish
- [x] F187 `node --check` en `api/eventos.js` y `useEventoForm.js`
- [x] F188 Prueba manual: crear evento con nombre + descripción + 3 fotos
      de galería → verlas en la tarjeta pública con lightbox
      (hecha en Chrome el 2026-09-29; detectó el bug de `planGaleria` que vaciaba la
      galería al editar y guardar — corregido en `useEventoForm.js` con la regresión
      en `js/ui/galeria.check.js`)