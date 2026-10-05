# Feature Specification: Nombre, descripción y galería — Frontend

**Feature Branch**: `019-nombre-descripcion-galeria`
**Created**: 2026-09-25
**Status**: Implemented (2026-10-02; F181-F192 completadas, ver tasks.md)

> Nota de trazabilidad: este folder es la copia con el NOMBRE CORRECTO del spec.
> Hasta el 2026-09-30 el spec (F181-F188) vivía en un folder heredado mal nombrado;
> el contenido real siempre fue "Nombre, descripción y galería". Aquí se consolidan
> F181-F192 completas. El folder mal nombrado se retiró el 2026-10-04 (duplicado
> obsoleto, con `plan.md` idéntico a `tasks.md`): su detalle de F188 se movió aquí.

## User Scenarios & Testing

### User Story 1 - Formulario con nombre y descripción separados (P1)
**Acceptance Scenarios**:
1. El formulario muestra "Nombre del evento" (texto, obligatorio) y
   "Descripción" (textarea, opcional) como campos separados.
2. Al editar un evento existente, `nombre` y `descripcion` se precargan
   en sus campos respectivos.
3. En la previsualización se muestran ambos campos.

### User Story 2 - Galería de fotos en el formulario (P2)
**Acceptance Scenarios**:
1. El formulario tiene una sección "Galería de fotos (máx. 5, opcional)"
   con hasta 5 slots de subida de imagen.
2. Al guardar, las fotos se suben una a una con
   `POST /eventos/{id}/galeria` (orden 0-4).
3. Al editar, los slots muestran las fotos existentes con botón de
   eliminar por slot.
4. En la vista pública (tarjeta de evento) aparece una galería de miniaturas
   clicables que abren el lightbox.

### User Story 3 - Vista pública muestra la galería (P1)
**Acceptance Scenarios**:
1. Las tarjetas de evento muestran las miniaturas de galería si existen.
2. Hacer clic en una miniatura abre el lightbox con esa foto.


### User Story 4 - Redes sociales, teléfono y URL en formulario y vista (P2)

**Acceptance Scenarios**:
1. El formulario tiene un botón "Añadir red social" que despliega una fila
   con un `<select>` de red y un `<input>` de URL; se pueden añadir hasta
   8 (una por red); botón "×" para eliminar la fila.
2. El campo de teléfono (opcional) se muestra con `<input type="tel">`.
3. El campo de URL del evento (opcional) se muestra con `<input type="url">`.
4. En la tarjeta pública, las redes sociales se muestran como iconos SVG
   enlazados; el teléfono como `<a href="tel:...">` y la URL como enlace
   externo. WhatsApp usa `href` con la URL `wa.me/...` almacenada, sin
   lógica especial adicional.
5. Al editar, los datos existentes se precargan en el formulario.

## Requirements
- **FR-001**: `apiToView()` añade `nombre: e.nombre` y `description:
  e.descripcion`; `content` (el alias antiguo de `descripcion`) pasa a
  mapear `e.nombre` para no romper las plantillas que ya usan `ev.content`.
- **FR-002**: El formulario añade `inputNombre` (obligatorio) y
  `inputDescripcion` (textarea, opcional).
- **FR-003**: La galería se sube después del evento (requiere el `id` del
  evento recién creado/editado).
- **FR-004**: La vista pública muestra miniaturas de galería con lightbox.
- **FR-005**: `apiToView()` añade `redes: e.redesSociales || []`,
  `telefono: e.telefonoEvento || null` y `urlEvento: e.urlEvento || null`
  (F189).
- **FR-006**: `useEventoForm.js` añade `inputRedes`, `inputTelefono`,
  `inputUrlEvento`, `añadirRed()` y `quitarRed(i)`; los precarga en
  `editEvento()` y `previsualizar()`, los envía en `submitForm()` y los
  expone en el return (F190).
- **FR-007**: Los formularios de `administrador.html` y `organizador.html`
  añaden teléfono, URL y sección de redes con el markup y el resto de
  interacciones del US4 (F191).
- **FR-008**: Las tarjetas públicas (6 tarjetas con evento real) muestran
  teléfono/URL/redes al final de `datos_eventos`; `textoRed(red)` en
  `js/ui/format.js`; estilos `.redes-evento`, `.red-evento-link`,
  `.red-social-row` con variables ya definidas; `textoRed` expuesto en los 3
  entrypoints (F192).

## Assumptions
- `content` en `apiToView()` sigue mapeando `e.nombre` (no `e.descripcion`)
  para no romper ninguna plantilla existente — es el "nombre visible" del
  evento en las tarjetas.
- Las fotos de galería en la vista pública se cargan con
  `<img src="${API}/eventos/${id}/galeria/${orden}">` para los índices
  que devuelva `GET /eventos/{id}/galeria`.
- Decisiones tomadas al implementar (2026-09-29):
  - Los `input[type=file]` de galería aceptan `image/jpeg, image/png, image/webp`
    en vez de `image/*`: el backend rechaza GIF y cualquier otro tipo con 400, así
    que el filtro del navegador evita el rechazo.
  - Las miniaturas se pintan en las tarjetas `.mostrar_evento` de las tres vistas
    (6 tarjetas), no en las listas compactas `.evento-row` (miniatura de 48px) ni en la
    tarjeta de previsualización del organizador: esa previsualización aún no tiene `id`
    (el evento aún no está guardado) y sus fotos son locales, no están en el backend.
  - `galeriaIndices` se pide por evento con `GET /eventos/{id}/galeria` al cargar cada
    lista; si el evento no tiene galería devuelve `[]` y la tarjeta no pinta miniaturas.
  - La carga "bajo demanda" de los índices de un evento pedida en F186 ya la cubre
    `cargarGaleria(ev)` (existente en `useEventosInvitado.js`): no se añadió
    `cargarGaleriaEvento` duplicada.
  - Si alguna foto falla al sincronizar tras guardar, el evento se conserva: se avisa de
    los fallos en un `alert` en vez de dar el guardado por perdido.
  - Un slot sin tocar (`undefined` en `fotosGaleria`) NO genera ninguna petición: solo el
    `null` que deja `quitarFotoGaleria()` significa "bórrala". Sin esa distinción, editar
    un evento y guardar vaciaba la galería entera (bug encontrado en la prueba de
    navegador del 2026-09-29 y cubierto por `galeria.check.js`).
  - Al pulsar "x" en una foto ya guardada la miniatura sigue visible (no se borra hasta
    guardar) pero se marca tachada y atenuada con `.galeria-slot-borrar`, para que el clic
    tenga respuesta.
- Decisiones al añadir contacto y redes (2026-09-30):
  - El `select` de cada fila replica los valores del enum `RedSocial` del backend
    (FACEBOOK, INSTAGRAM, X, YOUTUBE, TIKTOK, LINKEDIN, WHATSAPP, TELEGRAM); la nueva fila
    arranca en FACEBOOK. `textoRed` traduce el valor; fallback al propio valor si llegara
    uno desconocido.
  - El bloque de contacto/redes se añadió al final de cada `div.datos_eventos` de las 6
    tarjetas con evento real y TAMBIÉN a la tarjeta de previsualización del organizador
    (con `previewEv.*`; `previsualizar()` construye esos campos desde los inputs).
  - El CSS usa solo variables ya definidas en `css/variables.css`
    (`--color-primary`, `--color-surface`, `--space-xs/sm`, `--radius-pill/sm`,
    `--transition-fast`, `--color-border`).
  - En `submitForm()`, `telefonoEvento`/`urlEvento` van `null` si el campo quedó vacío y
    `redesSociales` refleja las filas tal cual (el backend valida las URLs y el límite de 8).
  - La red con URL vacía se envía igualmente: el backend la rechaza con 400 y su mensaje
    llega al `alert` del guardado, sin bloquear el resto del evento. (No se añadió validación
    local de URL: el backend es la única fuente de verdad y ya avisa campo a campo.)
- F188 se verificó en navegador real (Chrome vía extensión MCP, 2026-09-29) además de los
  checks: login admin, evento con cartel + 3 fotos, miniaturas públicas cargadas,
  lightbox (src/alt/cierre con `Esc` y clic fuera) y los tres casos de `planGaleria`
  (no tocar → intacta, quitar una → borra solo esa, sustituir/hueco → upsert sin borrados).
  La inspección visual de estilos en píxeles sigue siendo del usuario.
- F188 ampliado el 2026-09-30 tras el bug de imágenes grandes (dos fallos encadenados):
  - 1) El guardado mandaba el `File` original, sin pasar por `canvas`: una foto de 5.7 MB
    reventaba el límite de 2 MB de `spring.servlet.multipart` y Chrome cerraba la conexión
    con `net::ERR_CONNECTION_RESET` (sin respuesta que leer, luego sin 413 posible).
  - 2) Ya comprimida a WebP, si se quedaba por encima de ~500 KB el `INSERT` del `LONGBLOB`
    moría con `PacketTooBigException` (`max_allowed_packet=1 MB` en XAMPP) y la API devolvía
    500. Causa raíz: el almacenamiento, no el tráfico.
  - Solución en el frontend: `webify()` recorta hasta `MAX_BYTES_IMAGEN` (480 KiB) probando
    una escalera de calidad y, si hace falta, reduciendo el lado mayor; devuelve el primer
    resultado que entra. `useEventoForm` espera a las conversiones en vuelo (`procesandoGaleria`)
    antes del POST, y los archivos ya comprimidos no se recomprimen.
  - Solución en el backend: `GlobalExceptionHandler` traduce esa causa a 413 con mensaje
    accionable en lugar del 500 genérico.
  - Verificado en Chrome (2026-09-30) por la UI de administración: la foto de 5.7 MB quedó en
    WebP de 336.052 bytes, se guardó, y una segunda edición que no tocó la galería la
    conservó intacta. Además: POST directo de 600 KB -> 413, de 500 KB -> 201.
  - Cobertura: `js/ui/imagenes.check.js` (escalera y presupuesto, con mutation test negativo).
- F188, tercera tanda (2026-09-30): el CARTEL no esperaba a su propia compresión, el mismo
  bug que ya se había arreglado en galería pero por el camino del handler de plantilla:
  - `handleFileUpload` (js/ui/imagenes.js) lanzaba las dos conversiones (400 display + 1600 HD)
    con callbacks sueltos sin registrar ninguna, y `submitForm` solo esperaba a
    `esperarCompresionGaleria()`.
  - Reproducido en Chrome el 2026-09-30 con un JPEG de 8,5 MB puesto en `#fileInput` y
    "Guardar" pulsado en el MISMO tick (fijo sintético de ruido, 4000x2600, generado en la
    página para no depender del disco): el PUT sí salió (la descripción se guardó) pero el
    cartel se quedó en los 33.256 bytes anteriores. Pérdida SILENCIOSA: al editar, el backend
    conserva el cartel previo y la app no dice nada. En modo crear daba el error falso
    "Sube la imagen del cartel (obligatoria al crear)" con la foto ya elegida.
  - Arreglo: `procesandoCartel` (Set a nivel de módulo) + `esperarCompresionCartel()`, y las
    dos esperas se hacen ANTES de `validarObligatorios()` en `submitForm`, porque esa
    validación mira `file.value` y llegaría a null.
  - Verificado tras el arreglo con el mismo patrón de carrera: 8.565.007 -> 13.788 (display) y
    326.698 (HD), y en modo crear con cartel y galería pesados a la vez (7.110.281 bytes):
    evento creado con cartel 17.222 y galería 348.310, ambos WebP y bajo el presupuesto.
  - Cobertura en `js/ui/imagenes.check.js`: caso 6 (el handler registra y `esperarCompresionCartel`
    deja display y HD listos y dentro del presupuesto) y caso 7 (sobre el fuente, que la espera
    existe y precede a la validación). Verificado con 4 mutaciones: sin registro, sin espera,
    espera después de validar y sin espera de galería: las 4 se detectan.