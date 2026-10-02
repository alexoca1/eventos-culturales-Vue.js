// Self-check de la galería de fotos (019). Se ejecuta con:
//     node js/ui/galeria.check.js
// Sin framework ni fixtures. Es lo que más fácil se rompe al tocar la feature:
//   1. apiToView separa nombre/descripcion y deja galeriaIndices listo;
//   2. planGaleria decide bien qué slot se sube y cuál se borra (incluido el
//      caso de sustituir una foto que ya estaba guardada: sube, NO borra);
//   3. galeriaUrl y openLightbox: por id sale el cartel HD, por URL la foto tal cual;
//   4. toda vista con miniaturas tiene galeriaUrl expuesto en su entrypoint
//      (si falta, el clic peta en runtime y no hay aviso hasta que se usa).
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

// ------------------------------------------------------------- shim navegador
globalThis.location = { hostname: "127.0.0.1", href: "", pathname: "/frontend/views/administrador.html" };
globalThis.sessionStorage = {
    _m: new Map(),
    getItem(k) { return this._m.has(k) ? this._m.get(k) : null; },
    setItem(k, v) { this._m.set(k, String(v)); },
    removeItem(k) { this._m.delete(k); }
};
globalThis.window = { location: globalThis.location, addEventListener() {} };
globalThis.URL.createObjectURL = () => "blob:miniatura";
globalThis.URL.revokeObjectURL = () => {};
globalThis.alert = () => {};
// Sin shim de document a propósito: Vue lo detecta (doc = null) y arranca igual que
// en vistas.check.js. Si lo definieras, createElement se ejecutaría al importar.

const { AuthExpiredError } = await import("../api/http.js");
const { apiToView, galeriaUrl, cargarGalerias } = await import("../api/eventos.js");
const { planGaleria, useEventoForm } = await import("../composables/useEventoForm.js");
const { useLightbox } = await import("../composables/useLightbox.js");

// ---------------------------------------------- 1. apiToView: nombre ≠ descripcion
const v = apiToView({
    id: 7, nombre: "Noche de Flamenco", descripcion: "Con guitarra y baile.",
    establecimiento: "Casa Cultural", direccion: "Plaza Mayor 1", fecha: "2026-10-02",
    etiquetas: [{ nombre: "Música" }], cartelUrl: "", mapaEmbed: ""
});
assert.equal(v.nombre, "Noche de Flamenco", "apiToView debe exponer `nombre`");
assert.equal(v.content, "Noche de Flamenco", "`content` es el alias visible: el nombre, no la descripción");
assert.notEqual(v.content, v.description, "si content y description fueran el mismo texto, la tarjeta no distinguiría título y descripción");
assert.ok(v.description.includes("guitarra"), "`description` debe llevar el texto largo");
assert.deepEqual(v.galeriaIndices, [], "galeriaIndices arranca vacío: el listado no trae los órdenes");
assert.equal(v.establishment, "Casa Cultural", "apiToView no debe perder el resto del mapeo");
assert.deepEqual(v.etiquetas, ["Música"], "apiToView no debe perder las etiquetas");

// Sin descripción: "" y no undefined, para que la plantilla no imprima "undefined"
assert.equal(apiToView({ id: 8, nombre: "Charla", fecha: "2026-10-03" }).description, "",
    "una descripción ausente debe llegar como cadena vacía");

// 019 (F189): contacto y redes. Con datos y con los tres campos ausentes, que es el
// caso de los eventos ya guardados: si no tienen default, la tarjeta imprimiría
// "undefined" o el v-for de redes reventaría.
const contacto = apiToView({
    id: 9, nombre: "Verbena", fecha: "2026-10-05",
    redesSociales: [{ red: "FACEBOOK", url: "https://fb.test/v" }, { red: "TIKTOK", url: "https://tt.test/v" }],
    telefonoEvento: "+34 600 111 222", urlEvento: "https://ejemplo.org"
});
assert.equal(contacto.telefono, "+34 600 111 222", "apiToView debe mapear telefonoEvento -> telefono");
assert.equal(contacto.urlEvento, "https://ejemplo.org", "apiToView debe mapear urlEvento tal cual");
assert.deepEqual(contacto.redes, [{ red: "FACEBOOK", url: "https://fb.test/v" }, { red: "TIKTOK", url: "https://tt.test/v" }],
    "apiToView debe exponer las redes con su {red, url} intacto");
const sinContacto = apiToView({ id: 10, nombre: "Sin contacto", fecha: "2026-10-06" });
assert.deepEqual(sinContacto.redes, [], "sin redes debe llegar [] (v-for sobre undefined no pinta nada pero da error en runtime)");
assert.equal(sinContacto.telefono, null, "sin teléfono debe llegar null, no undefined");
assert.equal(sinContacto.urlEvento, null, "sin URL debe llegar null, no undefined");

// 019 (F190): el DTO del guardado y la precarga de la edición. Se comprueba sobre el
// fuente porque submitForm toca el DOM y el fetch real (ver imagenes.check.js para el
// mismo criterio): lo que importa es que el evento viaje con esos tres campos y que
// editar los recupere.
const fuenteForm = readFileSync(new URL("../composables/useEventoForm.js", import.meta.url), "utf8");
for (const clave of ["redesSociales:", "telefonoEvento:", "urlEvento:"]) {
    assert.ok(fuenteForm.includes(clave), `useEventoForm.js: el DTO de submitForm no envía ${clave}`);
}
assert.ok(/inputRedes\.value = \(ev\.redes \|\| \[\]\)\.map/.test(fuenteForm),
    "useEventoForm.js: editEvento debe precargar las redes del evento");
assert.ok(/inputTelefono\.value = ev\.telefono \|\| ""/.test(fuenteForm),
    "useEventoForm.js: editEvento debe precargar el teléfono");
assert.ok(/inputUrlEvento\.value = ev\.urlEvento \|\| ""/.test(fuenteForm),
    "useEventoForm.js: editEvento debe precargar la URL del evento");

// ------------------------------------------------------- 2. planGaleria: qué hacer
const ORDENES = [0, 1, 2, 3, 4];
const plan = (fotos, existentes) => planGaleria(ORDENES, fotos, existentes);

// Crear con dos fotos nuevas: solo subir, y en el orden de los huecos
assert.deepEqual(plan({ 0: { file: "a" }, 3: { file: "b" } }, []),
    [{ orden: 0, accion: "subir" }, { orden: 3, accion: "subir" }],
    "al crear hay que subir exactamente los archivos nuevos");

// Editar, sustituir la foto 0 y pulsar "x" en la 2 (quedó en null): sube 0, borra 2
// y NO toca la 1, que sigue guardada y sin tocar.
assert.deepEqual(plan({ 0: { file: "a" }, 2: null }, [0, 1, 2]),
    [{ orden: 0, accion: "subir" }, { orden: 2, accion: "borrar" }],
    "editar sin tocar una foto no debe generar nada para ella: se perdería al guardar");

// Sustituir: el mismo orden tiene archivo nuevo -> sube y NO borra (el backend
// hace upsert; un DELETE aquí dejaría el hueco vacío después del POST)
assert.deepEqual(plan({ 2: { file: "nueva" } }, [2]),
    [{ orden: 2, accion: "subir" }],
    "sustituir una foto no puede generar además un borrado del mismo orden");

// Editar y no tocar la galería: ni una petición. Este es el caso que en el navegador
// vaciaba la galería entera: `fotos` llega como array vacío (undefined en cada
// posición) y eso NO es lo mismo que un null explícito de "quitada".
assert.deepEqual(plan([], [0, 1, 2]), [],
    "guardar una edición sin tocar los slots no puede borrar la galería (regresión: se perdían todas)");
assert.deepEqual(plan({ 1: null }, [0, 1, 2]), [{ orden: 1, accion: "borrar" }],
    "solo el slot marcado con null se borra; los demás se conservan");
assert.deepEqual(plan({ 3: null }, [0, 1, 2]), [],
    "quitar un slot que nunca tuvo foto no genera peticiones: no hay nada que borrar en el backend");
assert.deepEqual(plan({}, []), [], "un evento sin fotos ni cambios no debe generar peticiones");
assert.deepEqual(plan({ 4: { file: "x" } }, []), [{ orden: 4, accion: "subir" }],
    "el último slot (índice 4) es válido: el backend admite órdenes 0..4");

// El ref se rellena con Object.assign: quitar un slot lo deja en null, no lo borra
const form = useEventoForm();
form.editingId.value = 7;
form.fotosGaleria.value = { 0: { file: "a", preview: "blob:a" }, 1: null };
form.galeriaExistente.value = [0, 1];
assert.equal(form.slotOcupado(0), true, "un slot con archivo nuevo está ocupado");
assert.equal(form.slotOcupado(1), true, "un slot con foto ya guardada está ocupado (se ve su miniatura)");
assert.equal(form.slotOcupado(2), false, "un slot vacío no muestra miniatura ni botón de quitar");
assert.equal(form.slotPreview(0), "blob:a", "con archivo nuevo, la miniatura es la previsualización local");
assert.equal(form.slotPreview(1), galeriaUrl(7, 1), "sin archivo nuevo, la miniatura apunta a la foto guardada");
assert.equal(form.slotPreview(2), "", "un slot vacío no tiene miniatura");
assert.equal(form.slotPendienteBorrar(1), true, "foto guardada tras pulsar x: se marca como pendiente de borrar");
assert.equal(form.slotPendienteBorrar(0), false, "slot con archivo nuevo no está pendiente de borrar");
assert.equal(form.slotPendienteBorrar(2), false, "slot vacío no está pendiente de borrar");
form.fotosGaleria.value = {};
assert.equal(form.slotPendienteBorrar(1), false,
    "slot sin tocar (undefined) no es pendiente: es lo que evita vaciar la galería al editar");
assert.equal(form.galeriaSlots.length, 5, "el formulario ofrece los 5 huecos que admite el backend");
assert.deepEqual(form.galeriaSlots, [0, 1, 2, 3, 4], "los órdenes son 0..4 como espera el backend");

// ------------------------------------------------------- 3. URLs y lightbox
assert.match(galeriaUrl(7, 3), /\/eventos\/7\/galeria\/3$/, "galeriaUrl debe apuntar al orden pedido");

const lb = useLightbox();
lb.openLightbox(7);
assert.match(lb.lightboxSrc.value, /\/eventos\/7\/cartel-hd$/, "por id sigue saliendo el cartel HD");
lb.openLightbox(galeriaUrl(7, 3), "Foto 4 del evento");
assert.match(lb.lightboxSrc.value, /\/eventos\/7\/galeria\/3$/, "por URL debe abrirse la foto de galería, no el cartel");
assert.equal(lb.lightboxAlt.value, "Foto 4 del evento", "el alt del lightbox debe describir la foto");

// ------------------------------------------- 4. las plantillas pueden llamarlo
const plantilla = n => readFileSync(new URL(`../../views/${n}.html`, import.meta.url), "utf8");
const entrypoint = n => readFileSync(new URL(`../pages/${n}.js`, import.meta.url), "utf8");
const PAGINAS = [["administrador", "admin"], ["organizador", "organizador"], ["usuarioEstandar", "invitado"]];

let miniaturas = 0;
let tarjetasContacto = 0;
for (const [nombre, page] of PAGINAS) {
    const h = plantilla(nombre);
    const e = entrypoint(page);
    // Una miniatura por tarjeta, no por aparición de la palabra: el conteo de
    // cadenas cruzaría también el comentario y el nombre de la clase.
    const enVista = (h.match(/<div class="galeria-miniaturas"/g) || []).length;
    if (enVista > 0) {
        // Si la vista pinta miniaturas y el entrypoint no expone galeriaUrl, el clic
        // revienta en runtime con "galeriaUrl is not a function" y no se ve hasta usarlo.
        assert.ok(/return \{[\s\S]*galeriaUrl/.test(e),
            `${nombre}.html usa galeria-miniaturas pero ${page}.js no devuelve galeriaUrl en el setup()`);
    }
    miniaturas += enVista;
    // 019 (F192): contacto y redes al final de cada tarjeta real. Igual que las
    // miniaturas, el conteo no cruza el comentario ni la tarjeta de previsualización.
    const enTarjetas = (h.match(/<p v-if="ev\.telefono">/g) || []).length;
    if (enTarjetas > 0) {
        assert.ok(/return \{[\s\S]*textoRed/.test(e),
            `${nombre}.html pinta contacto/redes pero ${page}.js no devuelve textoRed en el setup()`);
        assert.ok(/return \{[\s\S]*telHref/.test(e),
            `${nombre}.html usa telHref(...) pero ${page}.js no lo devuelve en el setup()`);
        assert.ok(!/tel:'\s*\+/.test(h),
            `${nombre}.html: el href tel: se concatena a mano; usa telHref(...) para limpiar el URI`);
        assert.ok(/<p v-if="ev\.urlEvento">/.test(h), `${nombre}.html: falta el enlace del evento en las tarjetas`);
        assert.ok(/class="redes-evento"/.test(h), `${nombre}.html: falta el bloque redes-evento`);
    }
    tarjetasContacto += enTarjetas;
    // El formulario del admin y el del organizador deben tener los 5 slots
    if (/Galería de fotos/.test(h)) {
        assert.ok(h.includes('v-for="orden in galeriaSlots"'), `${nombre}.html: los slots deben iterar galeriaSlots`);
        assert.ok(/@change="elegirFotoGaleria\(orden, \$event\)"/.test(h), `${nombre}.html: falta elegirFotoGaleria(orden, $event)`);
        assert.ok(/@click="quitarFotoGaleria\(orden\)"/.test(h), `${nombre}.html: falta quitarFotoGaleria(orden)`);
        assert.ok(/v-model="inputNombre"/.test(h), `${nombre}.html: falta el input del nombre`);
        assert.ok(/v-model="inputDescripcion"/.test(h), `${nombre}.html: falta la textarea de la descripción`);
        // 019 (F191): campos de contacto y sección de redes en el formulario
        assert.ok(/v-model="inputTelefono"/.test(h), `${nombre}.html: falta el campo del teléfono`);
        assert.ok(/v-model="inputUrlEvento"/.test(h), `${nombre}.html: falta el campo de la URL del evento`);
        assert.ok(/@click="añadirRed\(\)"/.test(h), `${nombre}.html: falta el botón de añadir red`);
        assert.ok(/v-for="\(r, i\) in inputRedes"/.test(h), `${nombre}.html: faltan las filas de redes`);
        assert.ok(/@click="quitarRed\(i\)"/.test(h), `${nombre}.html: falta el botón de quitar red`);
    }
    // El campo viejo fuera de las tres vistas
    assert.ok(!/v-model="inputContent"|errContent|f-contenido/.test(h),
        `${nombre}.html: quedan referencias al campo viejo (inputContent/errContent/f-contenido)`);
    // El overlay es el mismo para cartel y para foto de galería: si el alt se queda
    // fijo, un lector de pantalla anuncia "cartel ampliado" al abrir una foto.
    assert.ok(h.includes(':alt="lightboxAlt"'), `${nombre}.html: el lightbox debe usar :alt="lightboxAlt"`);
}
// 2 en admin (día + próximos), 2 en organizador (resultados de la búsqueda +
// próximos; la previsualización no lleva id todavía) y 3 en invitado (próximos,
// día y favoritos).
assert.equal(miniaturas, 7, "las 7 tarjetas con evento real deben pintar miniaturas");
// Las mismas 7 tarjetas llevan el bloque de contacto/redes (F192); la tarjeta de
// previsualización del organizador usa previewEv.* con su propio bloque, no cuenta aquí.
assert.equal(tarjetasContacto, 7, "las 7 tarjetas con evento real deben mostrar teléfono, URL y redes");

// --------------------- 5. la capa de red (contrato con la API), en seco
// Se ejercita el fetch() que usan las plantillas con un doble: el que hace el GET de
// los índices, el POST multipart y el DELETE. Lo que se comprueba aquí es el CONTRATO
// (nombre del campo, método, cabeceras y forma de la respuesta), no el backend de verdad:
// contra el backend real se verificó a mano por HTTP. Un fallo aquí significa que el
// contrato con el backend no cuadra.
let llamado = null;
globalThis.fetch = async (url, opts = {}) => {
    // Se guarda el headers que se envía de verdad: sin esto, quitar el
    // Authorization de un POST o DELETE pasaría el check (el endpoint lo rechazaría
    // con 403 en el navegador, sin que nada aquí se enterase).
    llamado = { url: String(url), metodo: opts.method || "GET", cuerpo: opts.body, headers: opts.headers || {} };
    if (!sessionStorage.getItem("token")) throw new AuthExpiredError("sin sesion");
    if (/\/galeria$/.test(url) && (opts.method || "GET") === "GET") {
        return { ok: true, status: 200, json: async () => [1, 3] };
    }
    if (/\/galeria$/.test(url) && opts.method === "POST") {
        return { ok: true, status: 201, json: async () => ({ orden: 2 }) };
    }
    if (opts.method === "DELETE") return { ok: true, status: 204, json: async () => null };
    return { ok: false, status: 404, json: async () => ({ error: "no encontrado" }) };
};

const { subirFotoGaleria, borrarFotoGaleria, listarIndicesGaleria } = await import("../api/eventos.js");

// GET: una petición por evento, sin cabeceras de escritura
sessionStorage.setItem("token", "jwt");
assert.deepEqual(await listarIndicesGaleria(5), [1, 3], "listarIndicesGaleria debe devolver los órdenes tal cual");
assert.equal(llamado.metodo, "GET", "listar los índices es un GET");
assert.match(llamado.url, /\/eventos\/5\/galeria$/, "debe pedir la galería del evento 5");

// POST: multipart con los nombres de campo que espera el backend ("foto" y "orden")
globalThis.FormData = class {
    constructor() { this.campos = []; }
    append(k, v) { this.campos.push([k, String(v)]); }
};
const subida = await subirFotoGaleria(5, 2, { name: "foto.jpg" });
assert.equal(subida.ok, true, "subirFotoGaleria debe devolver ok cuando el backend acepta");
assert.equal(llamado.metodo, "POST", "subir una foto es un POST");
const nombres = [...llamado.cuerpo.campos].map(([k]) => k).sort();
assert.deepEqual(nombres, ["foto", "orden"], "el multipart debe llevar exactamente los campos foto y orden");
// El backend bindea "orden" a Integer: FormData solo admite string, así que el
// número tiene que llegar convertido (append(2) se guardaría como "2" igualmente,
// pero si alguien lo pasa como Number en otro sitio, esto lo fija por contrato).
assert.equal(llamado.cuerpo.campos.find(([k]) => k === "orden")[1], "2", "el orden debe ir como texto, no como número");
assert.equal(typeof llamado.cuerpo.campos.find(([k]) => k === "orden")[1], "string", "el campo orden del multipart debe ser una cadena");
assert.ok(llamado.url.endsWith("/eventos/5/galeria"), "la subida va a la colección, con el orden en el cuerpo");
assert.match(llamado.headers.Authorization || "", /^Bearer jwt$/,
    "subir una foto es una escritura: sin Authorization el backend responde 403");

// DELETE: un 204 sin cuerpo es el éxito normal
const borrado = await borrarFotoGaleria(5, 2);
assert.equal(borrado, true, "un 204 al borrar debe contarse como correcto");
assert.equal(llamado.metodo, "DELETE", "borrar una foto es un DELETE");
assert.match(llamado.url, /\/eventos\/5\/galeria\/2$/, "el borrado va al orden concreto");
assert.match(llamado.headers.Authorization || "", /^Bearer jwt$/,
    "borrar una foto también necesita Authorization");

// Un 4xx con mensaje del backend debe llegar al organizador, no tragarse
globalThis.fetch = async () => ({ ok: false, status: 400, json: async () => ({ error: "Máximo 5 fotos por evento" }) });
const rechazada = await subirFotoGaleria(5, 9, { name: "foto.jpg" });
assert.equal(rechazada.ok, false, "un 400 del backend no puede darse por buena");
assert.match(rechazada.message, /Máximo 5 fotos/, `el mensaje del backend debe llegar al aviso (llegó: ${rechazada.message})`);

// Restauro el fetch que ignoraba los 4xx, para las pruebas de token y de red
globalThis.fetch = async (url, opts = {}) => {
    llamado = { url: String(url), metodo: opts.method || "GET", cuerpo: opts.body, headers: opts.headers || {} };
    if (!sessionStorage.getItem("token")) throw new AuthExpiredError("sin sesion");
    if (/\/galeria$/.test(url) && (opts.method || "GET") === "GET") {
        return { ok: true, status: 200, json: async () => [1, 3] };
    }
    if (/\/galeria$/.test(url) && opts.method === "POST") {
        return { ok: true, status: 201, json: async () => ({ orden: 2 }) };
    }
    if (opts.method === "DELETE") return { ok: true, status: 204, json: async () => null };
    return { ok: false, status: 404, json: async () => ({ error: "no encontrado" }) };
};

// Sin token el GET tiene que devolver [] y no lanzar: una tarjeta se pintaría igual
// y, sin token, la sesión ya está caducada y el propio flujo lleva al login.
sessionStorage.removeItem("token");
assert.deepEqual(await listarIndicesGaleria(5), [], "sin token, listarIndicesGaleria devuelve [] en vez de reventar");

// Y con la red caída, igual: cargarGalerias no puede vaciar una lista ya pintada.
let sinRed = false;
globalThis.fetch = async () => { if (sinRed) throw new Error("sin red"); return { ok: true, status: 200, json: async () => [0, 4] }; };
sessionStorage.setItem("token", "jwt");
const lista = [{ id: 1, galeriaIndices: [] }, { id: 2, galeriaIndices: [] }];
await cargarGalerias(lista);
assert.deepEqual(lista[0].galeriaIndices, [0, 4], "cargarGalerias debe rellenar los índices de cada evento");
sinRed = true;
await assert.doesNotReject(cargarGalerias([{ id: 3, galeriaIndices: [1] }]));
assert.deepEqual(lista[1].galeriaIndices, [0, 4], "un fallo de red no debe vaciar lo ya cargado");

// -------------------------------- 6. F190/F192 en runtime: lo que las plantillas enlazan
// Hasta aquí las plantillas se comprobaban sobre su texto. Lo que revienta en el
// navegador no es que falte la línea, sino que el composable no exponga lo que esa
// línea enlaza ("añadirRed is not a function") o que editEvento deje el ref vacío
// aunque la línea esté. Eso solo se ve instanciando el composable, que es lo de aquí.
const ENUM_REDES = ["FACEBOOK", "INSTAGRAM", "X", "YOUTUBE", "TIKTOK", "LINKEDIN", "WHATSAPP", "TELEGRAM"];
const { textoRed, telHref } = await import("../ui/format.js");

for (const clave of ["inputRedes", "inputTelefono", "inputUrlEvento"]) {
    assert.ok(form[clave] && typeof form[clave] === "object" && "value" in form[clave],
        `useEventoForm() debe devolver ${clave} como ref: las plantillas lo enlazan con v-model`);
}
for (const fn of ["añadirRed", "quitarRed"]) {
    assert.equal(typeof form[fn], "function", `useEventoForm() debe devolver ${fn}(): sin ella el clic revienta en runtime`);
}

// El selector ofrece exactamente el enum del backend: una opción de más se guarda con
// un 400 y una de menos deja la fila con una red que el usuario no eligió.
for (const nombre of ["administrador", "organizador"]) {
    const opciones = [...plantilla(nombre).matchAll(/<option value="([A-Z]+)">/g)].map(m => m[1]);
    assert.deepEqual(opciones, ENUM_REDES,
        `${nombre}.html: los <option> de la red deben ser los 8 valores del enum del backend`);
    assert.ok(/@click="añadirRed\(\)" :disabled="inputRedes\.length >= 8"/.test(plantilla(nombre)),
        `${nombre}.html: el botón de añadir red debe frenarse en 8, que es el tope del enum`);
}

for (const red of ENUM_REDES) {
    assert.ok(textoRed(red) && textoRed(red) !== red, `textoRed(${red}) debe devolver la etiqueta legible, no el valor crudo`);
}
assert.equal(textoRed("MASTODON"), "MASTODON", "una red desconocida se muestra tal cual, no como undefined");

// 019 (F192): el href tel: debe ser un URI válido. El texto visible conserva
// "+34 926 123 456", pero el href tal cual sale con espacios y no es un tel: válido.
assert.equal(telHref("+34 926 123 456"), "tel:+34926123456",
    "telHref debe limpiar los espacios del URI, no del texto visible");
assert.equal(telHref("(+34) 926-123.456"), "tel:+34926123456",
    "telHref debe limpiar paréntesis, guiones y puntos");
assert.equal(telHref("6474565"), "tel:6474565",
    "telHref debe conservar un número sin prefijo internacional");

// Una fila por clic y quitar por ÍNDICE (por valor borraría la fila equivocada).
form.inputRedes.value = [];
form.añadirRed();
form.añadirRed();
assert.equal(form.inputRedes.value.length, 2, "cada clic en añadir deja una fila más");
assert.deepEqual(form.inputRedes.value[0], { red: "FACEBOOK", url: "" },
    "la fila nueva arranca con la primera red y la URL vacía, que es lo que espera el backend");
form.quitarRed(0);
assert.deepEqual(form.inputRedes.value, [{ red: "FACEBOOK", url: "" }],
    "quitarRed(0) quita la primera fila, no la última");

// Precargar de verdad. El check de fuente daba por buena la línea aunque el valor no
// llegara al ref: aquí se comprueba que editEvento vuelque las cuatro cosas.
sinRed = false;
sessionStorage.setItem("token", "jwt");
const EV = {
    id: 42,
    establishment: "Centro Multifuncional",
    address: "Plaza de España, 1",
    date: "2026-11-20",
    content: "Noche de Flamenco",
    nombre: "Noche de Flamenco",
    description: "Concha Piquer en acústico",
    redes: [
        { red: "INSTAGRAM", url: "https://instagram.com/puertollano" },
        { red: "WHATSAPP", url: "https://wa.me/34600111222" }
    ],
    telefono: "926 001 112",
    urlEvento: "https://puertollano.es/flamenco",
    etiquetas: ["MUSICA"],
    poster: "<img>",
    map: "<iframe></iframe>"
};
await form.editEvento(EV);
assert.equal(form.editingId.value, 42, "editar entra en modo edición con su id");
assert.equal(form.inputNombre.value, "Noche de Flamenco", "precarga el nombre");
assert.equal(form.inputDescripcion.value, "Concha Piquer en acústico", "precarga la descripción");
assert.equal(form.inputTelefono.value, "926 001 112", "precarga el teléfono");
assert.equal(form.inputUrlEvento.value, "https://puertollano.es/flamenco", "precarga la URL del evento");
assert.deepEqual(form.inputRedes.value, EV.redes,
    "precarga las redes como filas editables [{red, url}], que es lo que itera la v-for del formulario");
assert.deepEqual(form.galeriaExistente.value, [0, 4], "precarga los órdenes con foto para pintar los slots");

// Crear después de editar no puede arrastrar las redes del evento anterior.
form.addEvent();
assert.equal(form.editingId.value, null, "addEvent() vuelve a modo crear");
assert.deepEqual(form.inputRedes.value, [], "addEvent() vacía las redes: si no, el evento nuevo hereda las del editado");
assert.equal(form.inputTelefono.value, "", "addEvent() vacía el teléfono");
assert.equal(form.inputUrlEvento.value, "", "addEvent() vacía la URL del evento");

console.log(
    `OK  ${miniaturas} tarjetas con miniaturas, ${tarjetasContacto} tarjetas con contacto/redes, ` +
    `5 slots por formulario, planGaleria con ${ORDENES.length} órdenes, lightbox en los dos modos, ` +
    `GET/POST/DELETE de galería con el contrato de la API, ` +
    `los 3 refs de contacto/redes y sus 2 handlers en runtime, selector = enum de 8, precarga de editar`
);
