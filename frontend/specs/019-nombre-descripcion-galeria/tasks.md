# Tasks: Nombre, descripción y galería + contacto y redes — Frontend

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
  cuando `ev.galeriaIndices.length > 0` con `@click="openLightbox(url)"`.
  La carga bajo demanda de los índices de un evento ya la cubre
  `cargarGaleria(ev)` en `useEventosInvitado.js` (no se duplicó).

## Phase 4: Polish (histórico, 2026-09-29/30)
- [x] F187 `node --check` en `api/eventos.js` y `useEventoForm.js`
- [x] F188 Prueba manual en Chrome 2026-09-29 + ampliaciones 2026-09-30
      (imágenes grandes y carreras: `webify`, `procesandoCartel`); el detalle
      completo vive en el spec histórico de `019-navegacion-directa-seccion-persistente/`

## Phase 5: Redes sociales, teléfono y URL (US4)
- [x] F189 En `api/eventos.js`, `apiToView()`: añadir `redes: e.redesSociales
  || []`, `telefono: e.telefonoEvento || null`, `urlEvento: e.urlEvento || null`
- [x] F190 En `useEventoForm.js`: añadir `inputRedes` (ref [], array de
  {red, url}), `inputTelefono` (ref ""), `inputUrlEvento` (ref "");
  función `añadirRed()` (añade fila vacía), `quitarRed(idx)` (elimina esa fila);
  precargarlos en `editEvento()`; incluirlos en `submitForm()`

## Phase 6: Formularios con contacto y redes (US4, 2026-09-30)
- [x] F191 En `administrador.html` y `organizador.html`, dentro del
  formulario (antes del botón "Previsualizar"):
  - Input `<input type="tel" v-model="inputTelefono"
    placeholder="+34 600 000 000">` con label "Teléfono de contacto
    (opcional)"
  - Input `<input type="url" v-model="inputUrlEvento"
    placeholder="https://...">` con label "Enlace del evento (opcional)"
  - Sección redes sociales: botón `@click="añadirRed()"` "Añadir red
    social"; lista `v-for="(r, i) in inputRedes"`:
```html
    <div class="red-social-row">
      <select v-model="r.red">
        <option value="FACEBOOK">Facebook</option>
        <option value="INSTAGRAM">Instagram</option>
        <option value="X">X (Twitter)</option>
        <option value="YOUTUBE">YouTube</option>
        <option value="TIKTOK">TikTok</option>
        <option value="LINKEDIN">LinkedIn</option>
        <option value="WHATSAPP">WhatsApp (URL wa.me/...)</option>
        <option value="TELEGRAM">Telegram</option>
      </select>
      <input type="url" v-model="r.url" placeholder="https://...">
      <button type="button" @click="quitarRed(i)">×</button>
    </div>
```

  Desviaciones respecto al snippet de arriba, a propósito:
  - El botón lleva `:disabled="inputRedes.length >= 8"`. El enum tiene 8
    valores y es el tope del backend; sin el frenado se pueden elegir más y
    el guardado falla con 400.
  - La `<option>` de WhatsApp dice "WhatsApp" y no "WhatsApp (URL wa.me/...)":
    la ayuda larga estorba en el desplegable (el `placeholder` del input ya
    dice `https://...`).

## Phase 7: Vista pública con contacto/redes (US4, 2026-09-30)
- [x] F192 En `usuarioEstandar.html`, `administrador.html` y
  `organizador.html`, dentro de la tarjeta de evento (sección
  `datos_eventos`), añadir al final:
```html
  <p v-if="ev.telefono">
    📞 <a :href="telHref(ev.telefono)">{{ev.telefono}}</a>
  </p>
  <p v-if="ev.urlEvento">
    🔗 <a :href="ev.urlEvento" target="_blank" rel="noopener">
      Más información</a>
  </p>
  <div v-if="ev.redes && ev.redes.length" class="redes-evento">
    <a v-for="r in ev.redes" :key="r.red"
       :href="r.url" target="_blank" rel="noopener"
       :title="r.red" class="red-evento-link">
      {{textoRed(r.red)}}
    </a>
  </div>
```
  Añadir `textoRed(red)` en `ui/format.js`:
```js
  export function textoRed(red) {
    return { FACEBOOK:'Facebook', INSTAGRAM:'Instagram', X:'X',
      YOUTUBE:'YouTube', TIKTOK:'TikTok', LINKEDIN:'LinkedIn',
      WHATSAPP:'WhatsApp', TELEGRAM:'Telegram' }[red] || red;
  }

  // El texto visible (+34 926 123 456) no se toca; el URI tel: solo admite
  // dígitos y un '+' (RFC 3966), así que se limpia aparte.
  export function telHref(telefono) {
    return 'tel:' + String(telefono).replace(/[^\d+]/g, '');
  }
```
  Añadir en `eventos.css`:
```css
  .redes-evento { display: flex; gap: var(--space-sm);
    flex-wrap: wrap; margin-top: var(--space-xs); }
  .red-evento-link { font-size: 0.85em; padding: var(--space-xs)
    var(--space-sm); background: var(--color-primary);
    color: var(--color-surface); border-radius: var(--radius-pill);
    text-decoration: none; }
  .red-evento-link:hover { background: var(--color-primary-hover); }
  .red-social-row { display: flex; gap: var(--space-sm);
    align-items: center; margin-bottom: var(--space-xs); }
  .red-social-row select, .red-social-row input { flex: 1; }
  ```

  Desviaciones respecto al snippet, a propósito:
  - El `href` del teléfono no es `'tel:' + ev.telefono` sino `telHref(...)`:
    el snippet original produce `tel:+34 926 123 456`, con espacios, que no
    es un URI `tel:` válido (RFC 3966). Verificado en Chrome 2026-10-02:
    el href salía con espacios. `telHref` limpia el URI y deja el texto
    visible tal cual.
  - `textoRed` devuelve `"X (Twitter)"` para `X`, no `"X"`: sola no se
    entiende. Los `<option>` del formulario usan la misma etiqueta.
  - `.red-evento-link` es un contorno (fondo `--color-surface`, borde y
    texto `--color-primary`) que se invierte al pasar por encima, en vez de
    relleno primario. Los badges van sobre `.datos_eventos`, que ya es
    blanco; el relleno sólido convertía la tarjeta en un bloque de color.
    Lleva además `:focus-visible` (se navega con teclado) y transición.
  - `flex: 1` va solo en el input de URL, no en el `select`: estirar un
    desplegable de 8 opciones a media fila queda peor que dejarlo a su
    ancho.
  - Todas las variables usadas (`--color-primary`, `--color-primary-hover`,
    `--color-surface`, `--radius-pill`, `--space-xs`, `--space-sm`) ya
    existían en `css/variables.css` (009-css-variables), que se carga antes
    que `eventos.css`. No se defined ninguna nueva.

## Phase 8: Cierre y check (2026-09-30)
- [x] F193 `node --check` en `api/eventos.js`, `useEventoForm.js`, `ui/format.js`,
      `ui/galeria.check.js` y los 3 entrypoints; `node js/ui/galeria.check.js`,
      `node js/ui/vistas.check.js`, `node js/ui/imagenes.check.js` en verde
- [x] F194 Copiar el spec a `frontend/specs/019-nombre-descripcion-galeria/`
      (spec.md, plan.md, tasks.md) con F181-F192 marcadas y Status Implemented;
      el folder anterior `019-navegacion-directa-seccion-persistente/` queda como
      historial (mal nombrado; su contenido es este spec)